package controllers;

import application.Main;
import components.CurvedFittedAreaChart;
import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ThreadLocalRandom;

public class TargetGraphsPageController extends Controller{
	
	@FXML
	private GridPane graphGrid;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
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

	public void setParent(MainMenuController p){this.parent = p;}

	@Override
	public void fill() {
	 	graphGrid.add(loadGraph(),0,0);
		graphGrid.add(loadGraph(),1,0);
		graphGrid.add(loadGraph(),0,1);
		graphGrid.add(loadGraph(),1,1);
		graphGrid.add(new Gauge(Gauge.SkinType.SLIM),2,0);
		graphGrid.add(new Gauge(Gauge.SkinType.SLIM),2,1);
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



	
}
