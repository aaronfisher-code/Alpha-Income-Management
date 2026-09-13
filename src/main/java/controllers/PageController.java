package controllers;

import com.dlsc.gemsfx.DialogPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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
	private Label liveZStatusLabel;
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

	protected void initializeLiveZStatus(Label statusLabel) throws IOException {
		liveZStatusLabel = statusLabel;
		liveZEnabled = LiveZConfiguration.load().enabled();
		statusLabel.setVisible(liveZEnabled);
		statusLabel.setManaged(liveZEnabled);
		if (!liveZEnabled) return;
		liveZDataService = new ZDataService();
		setLiveZStatus("Live Z data · checking connection…", "#6e6b7b");
	}

	protected boolean isLiveZEnabled() {
		return liveZEnabled;
	}

	protected void requireLiveZConnected(int storeId) {
		if (!liveZEnabled) return;
		try {
			var status = liveZDataService.getStatus(storeId);
			if (!status.connected()) {
				throw new LiveZUnavailableException(
						"Live Z data is enabled, but the pharmacy forwarder is offline.");
			}
			setLiveZStatus("Live Z data · connected", "#16803c");
		} catch (RuntimeException exception) {
			markLiveZUnavailable(exception);
			throw exception;
		}
	}

	protected void markLiveZUnavailable(Throwable failure) {
		if (!liveZEnabled) return;
		setLiveZStatus("Live Z data · unavailable", "#c62828");
	}

	protected Throwable unwrapAsyncFailure(Throwable failure) {
		Throwable current = failure;
		while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
				&& current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	private void setLiveZStatus(String text, String colour) {
		if (liveZStatusLabel == null) return;
		Runnable update = () -> {
			liveZStatusLabel.setText(text);
			liveZStatusLabel.setStyle("-fx-text-fill: " + colour + "; -fx-font-weight: bold;");
		};
		if (Platform.isFxApplicationThread()) update.run();
		else Platform.runLater(update);
	}
}
