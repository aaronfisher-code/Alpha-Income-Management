package services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class DocumentPdfCacheTest {
	@Test
	void rescanInvalidatesTheOldPreviewWhileAllowingAnActiveRendererToFinish() throws Exception {
		AtomicInteger downloads = new AtomicInteger();
		try (var cache = new DocumentPdfCache((document, store, path, limit) ->
				write(path, ("version-" + downloads.incrementAndGet()).getBytes(StandardCharsets.UTF_8)))) {
			File old;
			try (var first = cache.acquire(101, 1)) {
				old = first.file();
				cache.invalidate(101, 1);
				assertTrue(old.exists());
				try (var refreshed = cache.acquire(101, 1)) {
					assertNotEquals(old, refreshed.file());
					assertEquals("version-2", Files.readString(refreshed.file().toPath()));
				}
			}
			assertFalse(old.exists());
			cache.invalidateAll();
			try (var again = cache.acquire(101, 1)) { assertEquals("version-3", Files.readString(again.file().toPath())); }
		}
	}

	@Test
	void downloadsOnlyRequestedSourcesAndCachesByStoreAndDocument() throws Exception {
		AtomicInteger downloads = new AtomicInteger();
		File first;
		File otherStore;
		File otherDocument;
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
			downloads.incrementAndGet();
			return write(path, (store + ":" + document).getBytes(StandardCharsets.UTF_8));
		})) {
			assertEquals(0, downloads.get());
			try (var source = cache.acquire(101, 1); var repeated = cache.acquire(101, 1);
				 var storeSource = cache.acquire(101, 2); var documentSource = cache.acquire(102, 1)) {
				first = source.file();
				assertSame(first, repeated.file());
				assertEquals("1:101", Files.readString(first.toPath()));
				otherStore = storeSource.file();
				otherDocument = documentSource.file();
				assertNotEquals(first, otherStore);
				assertNotEquals(first, otherDocument);
				assertEquals(3, downloads.get());
			}
		}
		assertFalse(first.exists());
		assertFalse(otherStore.exists());
		assertFalse(otherDocument.exists());
	}

	@Test
	void previewSharesAnInflightPrefetchAndOtherPreviewsBypassTheBackgroundQueue() throws Exception {
		List<Long> requested = new CopyOnWriteArrayList<>();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch latestStarted = new CountDownLatch(1);
		try (var workers = Executors.newSingleThreadExecutor();
			 DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
				 requested.add(document);
				 if (document == 101) { started.countDown(); await(release); }
				 if (document == 103) latestStarted.countDown();
				 return write(path, new byte[] { 1 });
			 })) {
			try {
				cache.prefetch(101, 1);
				assertTrue(started.await(5, TimeUnit.SECONDS));
				cache.prefetch(102, 1);
				cache.prefetch(103, 1); // Supersedes the obsolete queued document.
				var shared = workers.submit(() -> cache.acquire(101, 1));
				try (var other = cache.acquire(201, 1)) {
					assertTrue(other.file().exists());
					assertEquals(List.of(101L, 201L), requested);
				}
				release.countDown();
				try (var first = shared.get(5, TimeUnit.SECONDS); var repeated = cache.acquire(101, 1)) {
					assertSame(first.file(), repeated.file());
				}
				assertTrue(latestStarted.await(5, TimeUnit.SECONDS));
				try (var latest = cache.acquire(103, 1)) { assertTrue(latest.file().exists()); }
				assertEquals(List.of(101L, 201L, 103L), requested);
			} finally { release.countDown(); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void speculativeSizeLimitsAndErrorsDoNotPreventOpeningTheInvoice(boolean networkFailure) throws Exception {
		List<Long> limits = new CopyOnWriteArrayList<>();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		List<Path> paths = new CopyOnWriteArrayList<>();
		try (var worker = Executors.newSingleThreadExecutor();
			 DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
				 limits.add(limit);
				 paths.add(path);
				 if (limit != Long.MAX_VALUE) {
					 started.countDown();
					 await(release);
					 write(path, new byte[] { 1 });
					 if (networkFailure) throw new IOException("Drive temporarily unavailable");
					 return false;
				 }
				 return write(path, new byte[8]);
			 }, 10, 5)) {
			try {
				cache.prefetch(101, 1);
				assertTrue(started.await(5, TimeUnit.SECONDS));
				var preview = worker.submit(() -> cache.acquire(101, 1));
				release.countDown();
				try (var source = preview.get(5, TimeUnit.SECONDS)) {
					assertEquals(8, Files.size(source.file().toPath()));
				}
				assertEquals(List.of(5L, Long.MAX_VALUE), limits);
				assertFalse(Files.exists(paths.getFirst()));
			} finally { release.countDown(); }
		}
		assertTrue(paths.stream().noneMatch(Files::exists));
	}

	@Test
	void evictionHonoursByteAndDocumentLimitsWithoutDeletingFilesBeingRendered() throws Exception {
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) ->
				write(path, new byte[4]), 8, 5)) {
			File older;
			try (var first = cache.acquire(101, 1)) { older = first.file(); }
			try (var pinned = cache.acquire(102, 1)) {
				try (var third = cache.acquire(103, 1)) { assertTrue(third.file().exists()); }
				assertFalse(older.exists());
				assertTrue(pinned.file().exists());
				cache.close();
				assertTrue(pinned.file().exists());
				File active = pinned.file();
				pinned.close();
				assertFalse(active.exists());
			}
		}
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) ->
				write(path, new byte[0]))) {
			File oldest;
			try (var source = cache.acquire(101, 1)) { oldest = source.file(); }
			for (int id = 102; id <= 104; id++) {
				try (var source = cache.acquire(id, 1)) { assertTrue(source.file().exists()); }
			}
			assertFalse(oldest.exists());
		}
	}

	@Test
	void failedDownloadsCanBeRetried() throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
			if (attempts.incrementAndGet() == 1) throw new IOException("Drive unavailable");
			return write(path, new byte[] { 1 });
		})) {
			assertEquals("Drive unavailable", assertThrows(IOException.class, () -> cache.acquire(101, 1)).getMessage());
			try (var source = cache.acquire(101, 1)) { assertTrue(source.file().exists()); }
			assertEquals(2, attempts.get());
		}
	}

	@Test
	void closingDuringPrefetchCleansLateFilesAndDoesNotStartQueuedDownloads() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger downloads = new AtomicInteger();
		List<Path> paths = new CopyOnWriteArrayList<>();
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
			downloads.incrementAndGet();
			paths.add(path);
			started.countDown();
			// Deliberately model a network read that finishes after cancellation.
			boolean released = false;
			while (!released) {
				try { released = release.await(5, TimeUnit.SECONDS); }
				catch (InterruptedException ignored) { }
			}
			return write(path, new byte[] { 1 });
		})) {
			try {
				cache.prefetch(101, 1);
				assertTrue(started.await(5, TimeUnit.SECONDS));
				cache.prefetch(102, 1);
				cache.close();
				release.countDown();
				awaitCondition(() -> paths.stream().noneMatch(Files::exists));
				assertEquals(1, downloads.get());
				assertThrows(IOException.class, () -> cache.acquire(101, 1));
			} finally { release.countDown(); }
		}
	}

	@Test
	void closingDuringAnExplicitDownloadRejectsLateResults() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try (var worker = Executors.newSingleThreadExecutor();
			 DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
				 started.countDown(); await(release); return write(path, new byte[] { 1 });
			 })) {
			try {
				var pending = worker.submit(() -> cache.acquire(101, 1));
				assertTrue(started.await(5, TimeUnit.SECONDS));
				cache.close();
				release.countDown();
				ExecutionException failure = assertThrows(ExecutionException.class,
						() -> pending.get(5, TimeUnit.SECONDS));
				assertInstanceOf(IOException.class, failure.getCause());
			} finally { release.countDown(); }
		}
	}

	private static boolean write(Path path, byte[] content) throws IOException { Files.write(path, content); return true; }

	private static void await(CountDownLatch latch) throws IOException {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("Test download timed out");
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException(exception);
		}
	}

	private static void awaitCondition(BooleanSupplier condition) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
		assertTrue(condition.getAsBoolean());
	}
}
