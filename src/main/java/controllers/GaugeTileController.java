package controllers;

import application.Main;
import components.CurvedFittedAreaChart;
import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ThreadLocalRandom;

public class GaugeTileController extends Controller{
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private TargetGraphsPageController parent;
    private User selectedUser;

    @FXML
	private StackPane gaugePane;
	
    @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(TargetGraphsPageController p){this.parent = p;}

	@Override
	public void fill() {
	 	gaugePane.getChildren().add(fillGauge());
	}

	public StackPane fillGauge(){

		StackPane finalPane = new StackPane();
		Gauge g = new Gauge(Gauge.SkinType.SLIM);
		g.setMinValue(0);
		g.setMaxValue(5);
		g.setValue(4.38);
		g.setAnimated(true);
		g.setBarColor(Color.web("#0F60FF"));
		g.setBarBackgroundColor(Color.web("#F3F2F7"));
		g.setValueColor(Color.web("#6e6b7b"));
		g.setAngleRange(90);
		g.setStartAngle(40);

		finalPane.getChildren().add(g);



		finalPane.setFocusTraversable(false);

		return finalPane;
	}

	
}
