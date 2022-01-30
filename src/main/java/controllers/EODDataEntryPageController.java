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
	 	headerRow.add(createHeaderLabel("Day of Month"),0,0);
		headerRow.add(createHeaderLabel("Cash"),1,0);
		headerRow.add(createHeaderLabel("EFTPOS"),2,0);
		headerRow.add(createHeaderLabel("Amex"),3,0);
		headerRow.add(createHeaderLabel("Google Square"),4,0);
		headerRow.add(createHeaderLabel("Cheque"),5,0);
		headerRow.add(createHeaderLabel("Clinical Interventions"),6,0);
		headerRow.add(createHeaderLabel("Medschecks"),7,0);
		headerRow.add(createHeaderLabel("Stock on hand"),8,0);
		headerRow.add(createHeaderLabel("Scripts on file count"),9,0);
		headerRow.add(createHeaderLabel("SMS Patients"),10,0);
		headerRow.add(createHeaderLabel("Till balance"),11,0);
		headerRow.add(createHeaderLabel("Running Till Balance"),12,0);
		headerRow.add(createHeaderLabel("Comments"),13,0);
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
	 	l.setWrapText(true);
	 	return l;
	}
	
}
