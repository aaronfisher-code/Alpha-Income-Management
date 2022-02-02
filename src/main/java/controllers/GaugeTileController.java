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
		finalPane.getChildren().add(new Gauge(Gauge.SkinType.SLIM));

		DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#000000",0.41),5.56,0.0,0.0,2.0);
		d.setHeight(12.2);
		d.setWidth(12.1);
		//finalPane.setEffect(d);
		finalPane.setFocusTraversable(false);

		return finalPane;
	}

	
}
