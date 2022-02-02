package controllers;

import application.Main;
import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TargetGraphsPageController extends Controller{
	
	@FXML
	private GridPane graphGrid;
	@FXML
	private BorderPane wtdButton,mtdButton,ytdButton;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private User selectedUser;
	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	@Override
	public void fill() {

	 	graphGrid.add(loadGraph(),0,0);
		graphGrid.add(loadGraph(),1,0);
		graphGrid.add(loadGraph(),0,1);
		graphGrid.add(loadGraph(),1,1);
		graphGrid.add(loadGauge(),2,0);
		graphGrid.add(loadGauge(),2,1);
	}

	public BorderPane loadGraph() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/GraphTile.fxml"));
		BorderPane graphTile = null;
		try {
			graphTile = loader.load();
		} catch (IOException e) {

			e.printStackTrace();
		}
		GraphTileController gtc = loader.getController();
		gtc.setMain(main);
		gtc.setConnection(con);
		gtc.setParent(this);
		gtc.fill();
		return graphTile;
	}

	public BorderPane loadGauge() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/GaugeTile.fxml"));
		BorderPane gaugeTile = null;
		try {
			gaugeTile = loader.load();
		} catch (IOException e) {

			e.printStackTrace();
		}
		GaugeTileController gtc = loader.getController();
		gtc.setMain(main);
		gtc.setConnection(con);
		gtc.setParent(this);
		gtc.fill();
		return gaugeTile;
	}

	public void wtdView(){
		formatTabSelect(wtdButton);
		formatTabDeselect(mtdButton);
		formatTabDeselect(ytdButton);
	}

	public void mtdView(){
		formatTabSelect(mtdButton);
		formatTabDeselect(wtdButton);
		formatTabDeselect(ytdButton);
	}

	public void ytdView(){
		formatTabSelect(ytdButton);
		formatTabDeselect(wtdButton);
		formatTabDeselect(mtdButton);
	}

	public void formatTabSelect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #0F60FF");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("-fx-background-color: #0F60FF");
			}
		}
	}

	public void formatTabDeselect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #6e6b7b");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("");
			}
		}
	}



	
}
