package controllers;

import application.Main;
import components.CurvedFittedAreaChart;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ThreadLocalRandom;

public class GraphTileController extends Controller{
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private TargetGraphsPageController parent;
    private User selectedUser;

    @FXML
	private StackPane graphPane;
	
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
	 	graphPane.getChildren().add(fillGraph("No of scripts", true));
	}

	public StackPane fillGraph(String title, Boolean hideYAxis){



		//defining a series
		XYChart.Series<Number,Number> series = new XYChart.Series();
		for(int i=0;i<15;i++)
			series.getData().add(new XYChart.Data(i, ThreadLocalRandom.current().nextInt((50000/30)*(i), (50000/30)*(i+3) + 1)));

		XYChart.Series<Number,Number> series1 = new XYChart.Series();
		for(int i=0;i<31;i++)
			series1.getData().add(new XYChart.Data(i, (48000/30)*(i)));

		XYChart.Series<Number,Number> series2 = new XYChart.Series();
		for(int i=0;i<31;i++)
			series2.getData().add(new XYChart.Data(i, (52000/30)*(i)));

		NumberAxis xAxis = new NumberAxis(0, 31, 1);
		NumberAxis yAxis = new NumberAxis(0,55000,5000);
		xAxis.setLabel("Day of month");
		xAxis.setMinorTickVisible(false);

		yAxis.setLabel("Script #");
		yAxis.setTickLabelsVisible(!hideYAxis);
		yAxis.setTickMarkVisible(false);
		yAxis.setMinorTickVisible(false);
		yAxis.setTickLabelsVisible(false);

		//creating the charts
		CurvedFittedAreaChart areaChart = new CurvedFittedAreaChart(xAxis, yAxis);
		LineChart<Number,Number> target1Chart = new LineChart<>(xAxis, yAxis);
		LineChart<Number,Number> target2Chart = new LineChart<>(xAxis, yAxis);


		series.setName("Actual");
		series1.setName("Target 1");
		series2.setName("Target 2");
		areaChart.getData().add(series);
		target1Chart.getData().add(series1);
		target2Chart.getData().add(series2);

		areaChart.setHorizontalGridLinesVisible(false);
		areaChart.setVerticalGridLinesVisible(false);
		areaChart.setPadding(new Insets(20,40,20,20));
		areaChart.setAnimated(false);
		areaChart.setCreateSymbols(false);
		areaChart.setLegendVisible(false);
		areaChart.setStyle("-fx-stroke: #0F60FF;");

		target1Chart.setHorizontalGridLinesVisible(false);
		target1Chart.setVerticalGridLinesVisible(false);
		target1Chart.setPadding(new Insets(20,40,20,20));
		target1Chart.setAnimated(false);
		target1Chart.setCreateSymbols(false);
		target1Chart.setLegendVisible(false);

		target2Chart.setHorizontalGridLinesVisible(false);
		target2Chart.setVerticalGridLinesVisible(false);
		target2Chart.setPadding(new Insets(20,40,20,20));
		target2Chart.setAnimated(false);
		target2Chart.setCreateSymbols(false);
		target2Chart.setLegendVisible(false);

		StackPane finalPane = layerCharts(target1Chart,target2Chart,areaChart);

		DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#000000",0.41),5.56,0.0,0.0,2.0);
		d.setHeight(12.2);
		d.setWidth(12.1);





		//finalPane.setEffect(d);
		finalPane.setFocusTraversable(false);

		return finalPane;
	}

	private StackPane layerCharts(final XYChart<Number, Number> ... charts) {
		for (int i = 1; i < charts.length; i++) {
			configureOverlayChart(charts[i]);
		}

		StackPane stackpane = new StackPane();
		stackpane.getChildren().addAll(charts);

		return stackpane;
	}

	private void configureOverlayChart(final XYChart<Number, Number> chart) {
		chart.setAlternativeRowFillVisible(false);
		chart.setAlternativeColumnFillVisible(false);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);
		chart.getXAxis().setVisible(false);
		chart.getYAxis().setVisible(false);
	}

	
}
