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

	public ExecutorService getExecutor() {
		return executor;
	}

	public void shutdownExecutor() {
		executor.shutdown();
	}
}
