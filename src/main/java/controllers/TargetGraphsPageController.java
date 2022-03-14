package controllers;

import application.Main;
import components.layouts.BootstrapColumn;
import components.layouts.BootstrapPane;
import components.layouts.BootstrapRow;
import components.layouts.Breakpoint;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TargetGraphsPageController extends Controller{
	
	@FXML
	private MFXScrollPane graphScrollPane;
	@FXML
	private BorderPane wtdButton,mtdButton,ytdButton,backgroundPane;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private User selectedUser;
    private BootstrapPane outerPane = new BootstrapPane();
	private BootstrapPane graphPane = new BootstrapPane();
	private BootstrapPane gaugePane = new BootstrapPane();
	
	 @FXML
	private void initialize() throws IOException {
		 graphScrollPane.heightProperty().addListener((observable, oldValue, newValue) -> {
			 graphPane.setPrefHeight((Double) newValue);
		 });
	 }

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	@Override
	public void fill() {

		outerPane.setVgap(20);
		outerPane.setHgap(20);
		outerPane.setPadding(new Insets(20));

		BootstrapRow contentRow = new BootstrapRow();

		//Setup line graphs
		graphPane = new BootstrapPane(graphScrollPane);
		graphPane.setVgap(20);
		graphPane.setHgap(20);

		BootstrapRow graphRow = new BootstrapRow();
		BootstrapColumn graph1 = new BootstrapColumn(loadGraph());
		BootstrapColumn graph2 = new BootstrapColumn(loadGraph());
		BootstrapColumn graph3 = new BootstrapColumn(loadGraph());
		BootstrapColumn graph4 = new BootstrapColumn(loadGraph());
		graph1.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		graph1.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		graph1.setBreakpointColumnWidth(Breakpoint.LARGE, 6);
		graph2.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		graph2.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		graph2.setBreakpointColumnWidth(Breakpoint.LARGE, 6);
		graph3.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		graph3.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		graph3.setBreakpointColumnWidth(Breakpoint.LARGE, 6);
		graph4.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		graph4.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		graph4.setBreakpointColumnWidth(Breakpoint.LARGE, 6);

		graphRow.addColumn(graph1);
		graphRow.addColumn(graph2);
		graphRow.addColumn(graph3);
		graphRow.addColumn(graph4);

		graphPane.addRow(graphRow);
		BootstrapColumn graphCol = new BootstrapColumn(graphPane);
		graphCol.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		graphCol.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		graphCol.setBreakpointColumnWidth(Breakpoint.LARGE, 9);
		contentRow.addColumn(graphCol);

		//Setup Gauges
		gaugePane.setVgap(20);
		gaugePane.setHgap(20);

		BootstrapRow gaugeRow = new BootstrapRow();
		BootstrapColumn gauge1 = new BootstrapColumn(loadGauge());
		BootstrapColumn gauge2 = new BootstrapColumn(loadGauge());
		gauge1.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		gauge1.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		gauge1.setBreakpointColumnWidth(Breakpoint.LARGE, 6);
		gauge2.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		gauge2.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		gauge2.setBreakpointColumnWidth(Breakpoint.LARGE, 6);

		gaugeRow.addColumn(gauge1);
		gaugeRow.addColumn(gauge2);


		gaugePane.addRow(gaugeRow);
		BootstrapColumn gaugeCol = new BootstrapColumn(gaugePane);
		gaugeCol.setBreakpointColumnWidth(Breakpoint.XSMALL, 12);
		gaugeCol.setBreakpointColumnWidth(Breakpoint.SMALL, 12);
		gaugeCol.setBreakpointColumnWidth(Breakpoint.LARGE, 3);
		contentRow.addColumn(gaugeCol);

		outerPane.addRow(contentRow);
		graphScrollPane.setContent(outerPane);
		graphPane.setPrefHeight(graphScrollPane.getHeight());

		adjustHeight();


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
//		FlexBoxPane.setGrow(graphTile, 2.0f);
//		HBox.setHgrow(graphTile,Priority.ALWAYS);
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
		VBox.setVgrow(gaugeTile, Priority.ALWAYS);
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

	public void adjustHeight(){
	 	graphPane.setPrefHeight(graphScrollPane.getHeight());
	}



	
}
