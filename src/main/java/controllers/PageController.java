package controllers;

import com.dlsc.gemsfx.DialogPane;
import javafx.fxml.FXML;

import java.util.concurrent.ExecutorService;

public abstract class PageController extends Controller {
	@FXML
	protected DialogPane dialogPane;
	protected DialogPane.Dialog<Void> dialog;
	protected ExecutorService executor;
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
}
