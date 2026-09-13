package controllers;

import com.dlsc.gemsfx.DialogPane;
import javafx.fxml.FXML;
import services.LiveZConfiguration;
import services.LiveZUnavailableException;
import services.ZDataService;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public abstract class PageController extends Controller {
	@FXML
	protected DialogPane dialogPane;
	protected DialogPane.Dialog<Void> dialog;
	protected ExecutorService executor;
	private boolean liveZEnabled;
	private ZDataService liveZDataService;
	public abstract void fill();
	public DialogPane.Dialog<Void> getDialog() {return dialog;}
	public DialogPane getDialogPane() {return dialogPane;}

	protected <T> DialogPane.Dialog<T> createDialog(DialogPane.Type type) {
		DialogPane.Dialog<T> newDialog = new DialogPane.Dialog<>(dialogPane, type);
		newDialog.setShowHeader(true);
		newDialog.setShowCloseButton(true);
		return newDialog;
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void shutdownExecutor() {
		executor.shutdown();
	}

	protected void initializeLiveZ() throws IOException {
		liveZEnabled = LiveZConfiguration.load().enabled();
		if (!liveZEnabled) return;
		liveZDataService = new ZDataService();
	}

	protected boolean isLiveZEnabled() {
		return liveZEnabled;
	}

	protected void requireLiveZConnected(int storeId) {
		if (!liveZEnabled) return;
		var status = liveZDataService.getStatus(storeId);
		if (!status.connected()) {
			throw new LiveZUnavailableException(
					"Pharmacy data is enabled, but the Z forwarder is offline.");
		}
	}

	protected Throwable unwrapAsyncFailure(Throwable failure) {
		Throwable current = failure;
		while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
				&& current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

}
