package services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A small, dialog-scoped PDF cache with one background download and one pending target. */
public final class DocumentPdfCache implements AutoCloseable {
	private static final long CACHE_BYTES = 20L * 1024 * 1024;
	private static final long PREFETCH_BYTES = 5L * 1024 * 1024;
	private static final int CACHE_DOCUMENTS = 3;

	@FunctionalInterface
	public interface Downloader {
		/** Streams into destination; returns false if the source exceeds maxBytes. */
		boolean download(long documentId, int storeId, Path destination, long maxBytes) throws IOException;
	}

	private record Key(long documentId, int storeId) {}
	private static final class Entry {
		final Key key;
		final boolean speculative;
		final CompletableFuture<File> result = new CompletableFuture<>();
		int users;
		File file;
		long bytes;
		boolean invalidated;
		Entry(Key key, boolean speculative) { this.key = key; this.speculative = speculative; }
	}

	/** Keeps a source file alive while PDFBox is reading it. */
	public final class Source implements AutoCloseable {
		private final Entry entry;
		private final File file;
		private boolean released;
		private Source(Entry entry, File file) { this.entry = entry; this.file = file; }
		public File file() { return file; }
		@Override public void close() {
			synchronized (DocumentPdfCache.this) {
				if (!released) { released = true; release(entry); }
			}
		}
	}

	private final Downloader downloader;
	private final long cacheBytes;
	private final long prefetchBytes;
	private final Map<Key, Entry> downloads = new LinkedHashMap<>(4, 0.75f, true);
	private final ExecutorService background = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "invoice-pdf-prefetch");
		thread.setDaemon(true);
		return thread;
	});
	private Key pendingPrefetch;
	private boolean prefetchRunning;
	private long retainedBytes;
	private boolean closed;

	public DocumentPdfCache(Downloader downloader) { this(downloader, CACHE_BYTES, PREFETCH_BYTES); }

	DocumentPdfCache(Downloader downloader, long cacheBytes, long prefetchBytes) {
		this.downloader = downloader;
		this.cacheBytes = cacheBytes;
		this.prefetchBytes = prefetchBytes;
	}

	/** Replaces queued speculation. Explicit preview requests never queue behind this worker. */
	public synchronized void prefetch(long documentId, int storeId) {
		if (closed) return;
		Key key = new Key(documentId, storeId);
		pendingPrefetch = downloads.containsKey(key) ? null : key;
		if (pendingPrefetch != null && !prefetchRunning) {
			prefetchRunning = true;
			background.execute(this::runPrefetch);
		}
	}

	public synchronized void stopPrefetch() { pendingPrefetch = null; }

	/** A rescan must not reuse a preview of an older version of the source PDF. */
	public synchronized void invalidate(long documentId, int storeId) {
		Key key = new Key(documentId, storeId);
		if (key.equals(pendingPrefetch)) pendingPrefetch = null;
		Entry entry = downloads.remove(key);
		if (entry != null) {
			entry.invalidated = true;
			discardInvalidated(entry);
		}
	}

	public synchronized void invalidateAll() {
		pendingPrefetch = null;
		for (Key key : java.util.List.copyOf(downloads.keySet())) invalidate(key.documentId(), key.storeId());
	}

	private void runPrefetch() {
		while (true) {
			Key key;
			synchronized (this) {
				key = pendingPrefetch;
				pendingPrefetch = null;
				if (closed || key == null) { prefetchRunning = false; return; }
			}
			try (Source ignored = acquire(key, true)) {
				// Warm only the source PDF. Rendering remains an explicit user action.
			} catch (IOException | RuntimeException ignored) {
				// Speculation must not interrupt review; an explicit open can retry.
			}
		}
	}

	/** Call from a worker thread and close the returned source after rendering. */
	public Source acquire(long documentId, int storeId) throws IOException {
		return acquire(new Key(documentId, storeId), false);
	}

	private Source acquire(Key key, boolean speculative) throws IOException {
		while (true) {
			Entry entry;
			boolean start;
			synchronized (this) {
				if (closed) throw new IOException("The document review dialog has closed");
				entry = downloads.get(key);
				start = entry == null;
				if (start) { entry = new Entry(key, speculative); downloads.put(key, entry); }
				entry.users++;
			}
			if (start) download(entry);
			try {
				return new Source(entry, entry.result.get());
			} catch (InterruptedException exception) {
				release(entry);
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while loading the source PDF", exception);
			} catch (ExecutionException exception) {
				release(entry);
				// A speculative size limit or transient error must not block an explicit open.
				if (!speculative && entry.speculative) continue;
				if (exception.getCause() instanceof IOException io) throw io;
				if (exception.getCause() instanceof RuntimeException runtime) throw runtime;
				throw new IOException("Unable to load the source PDF", exception.getCause());
			}
		}
	}

	private void download(Entry entry) {
		File temporary = null;
		try {
			temporary = Files.createTempFile("alpha-review-", ".pdf").toFile();
			long limit = entry.speculative ? Math.min(prefetchBytes, cacheBytes) : Long.MAX_VALUE;
			if (!downloader.download(entry.key.documentId(), entry.key.storeId(), temporary.toPath(), limit)
					|| Files.size(temporary.toPath()) > limit) {
				throw new IOException("The PDF exceeds the background download limit");
			}
			synchronized (this) {
				if (closed) throw new IOException("The document review dialog has closed");
				if (entry.invalidated) throw new IOException("The source PDF was invalidated for rescan");
				entry.file = temporary;
				entry.bytes = Files.size(temporary.toPath());
				retainedBytes += entry.bytes;
				entry.result.complete(temporary);
				temporary = null;
				trim();
			}
		} catch (Exception exception) {
			if (temporary != null) delete(temporary);
			synchronized (this) { downloads.remove(entry.key, entry); }
			entry.result.completeExceptionally(exception);
		}
	}

	private synchronized void release(Entry entry) {
		entry.users--;
		discardInvalidated(entry);
		trim();
	}

	private void discardInvalidated(Entry entry) {
		if (entry.invalidated && entry.users == 0 && entry.file != null) {
			retainedBytes -= entry.bytes;
			delete(entry.file);
			entry.file = null;
		}
	}

	private void trim() {
		var entries = downloads.values().iterator();
		while (entries.hasNext()) {
			Entry entry = entries.next();
			if (entry.users != 0 || entry.file == null) continue;
			if (closed || retainedBytes > cacheBytes || downloads.size() > CACHE_DOCUMENTS) {
				entries.remove();
				retainedBytes -= entry.bytes;
				delete(entry.file);
			}
		}
		// Active renderers pin their files; an oversized source is removed on release.
	}

	@Override public synchronized void close() {
		closed = true;
		pendingPrefetch = null;
		background.shutdownNow();
		trim();
		// Downloads finishing after close delete their temporary files before publication.
	}

	private static void delete(File file) {
		try { Files.deleteIfExists(file.toPath()); }
		catch (IOException exception) { file.deleteOnExit(); }
	}
}
