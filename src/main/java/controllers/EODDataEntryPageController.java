package controllers;

import application.Main;
import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EODDataEntryPageController extends Controller{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;

    @FXML
	private GridPane headerRow;

    @FXML
	private VBox dataEntryRowPane;
	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(MainMenuController p){this.parent = p;}

	@Override
	public void fill() {
	 	headerRow.add(createHeaderLabel("DAY OF MONTH"),0,0);
		headerRow.add(createHeaderLabel("CASH"),1,0);
		headerRow.add(createHeaderLabel("EFTPOS"),2,0);
		headerRow.add(createHeaderLabel("AMEX"),3,0);
		headerRow.add(createHeaderLabel("GOOGLE SQUARE"),4,0);
		headerRow.add(createHeaderLabel("CHEQUE"),5,0);
		headerRow.add(createHeaderLabel("CLINICAL INTERVENTIONS"),6,0);
		headerRow.add(createHeaderLabel("MEDSCHECKS"),7,0);
		headerRow.add(createHeaderLabel("STOCK ON HAND"),8,0);
		headerRow.add(createHeaderLabel("SCRIPTS ON FILE"),9,0);
		headerRow.add(createHeaderLabel("SMS PATIENTS"),10,0);
		headerRow.add(createHeaderLabel("TILL BALANCE"),11,0);
		headerRow.add(createHeaderLabel("RUNNING TILL BALANCE"),12,0);
		headerRow.add(createHeaderLabel("NOTES"),13,0);
		for (int i = 0; i< headerRow.getColumnCount(); i++) {
			ColumnConstraints col = new ColumnConstraints();
			col.setFillWidth(true);
			col.setHgrow(Priority.ALWAYS);
			headerRow.getColumnConstraints().add(col);
		}

	}

	private Label createHeaderLabel(String labelText){
	 	Label l = new Label(labelText);
	 	l.setMaxWidth(Double.MAX_VALUE);
	 	l.setMinWidth(Region.USE_COMPUTED_SIZE+20);
		l.setPrefWidth(Region.USE_COMPUTED_SIZE+20);
	 	l.setWrapText(true);
	 	l.setTextAlignment(TextAlignment.LEFT);
	 	return l;
	}
	
}
