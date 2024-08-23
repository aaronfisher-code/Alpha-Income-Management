package controllers;

import com.dlsc.gemsfx.DialogPane;
import javafx.fxml.FXML;

public abstract class PageController extends Controller {
	@FXML
	protected DialogPane dialogPane;
	protected DialogPane.Dialog<Object> dialog;
	public abstract void fill();
	public DialogPane.Dialog<Object> getDialog() {return dialog;}
	public DialogPane getDialogPane() {return dialogPane;}
}
