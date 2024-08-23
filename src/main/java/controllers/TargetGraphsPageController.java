package controllers;

import com.dlsc.gemsfx.DialogPane;
import strategies.*;
import application.Main;
import components.layouts.BootstrapColumn;
import components.layouts.BootstrapPane;
import components.layouts.BootstrapRow;
import components.layouts.Breakpoint;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import utils.GUIUtils;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class TargetGraphsPageController extends PageController {
	
	@FXML private MFXScrollPane graphScrollPane;
	@FXML private BorderPane wtdButton;
    @FXML private BorderPane mtdButton;
    @FXML private BorderPane ytdButton;
    @FXML private DialogPane dialogPane;
    private BootstrapPane outerPane;
	private BootstrapPane graphPane;
	private BootstrapPane gaugePane;
	
	 @FXML
	private void initialize() {
		 graphScrollPane.heightProperty().addListener((_, _, newValue) -> {
			 graphPane.setPrefHeight((Double) newValue);
		 });
	 }

	public Main getMain() {return main;}

	@Override
	public void fill() {
		 wtdView();
	}

	public void updateGraphs(LocalDate startDate,LocalDate endDate){
		 graphScrollPane.setContent(null);
		outerPane = new BootstrapPane();
		outerPane.setVgap(20);
		outerPane.setHgap(20);
		outerPane.setPadding(new Insets(20));
		BootstrapRow contentRow = new BootstrapRow();
		//Setup line graphs
		graphPane = new BootstrapPane(graphScrollPane);
		graphPane.setVgap(20);
		graphPane.setHgap(20);
		BootstrapRow graphRow = new BootstrapRow();
		BootstrapColumn graph1 = new BootstrapColumn(loadGraph(new NumberOfScriptsStrategy(startDate, endDate, this)));
		BootstrapColumn graph2 = new BootstrapColumn(loadGraph(new OTCDollarPerCustomerStrategy(startDate, endDate, this)));
		BootstrapColumn graph3 = new BootstrapColumn(loadGraph(new GPDollarStrategy(startDate, endDate, this)));
		BootstrapColumn graph4 = new BootstrapColumn(loadGraph(new ScriptsOnFileStrategy(startDate, endDate, this)));
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
		graph1.getContent().maxHeight((main.getStg().getHeight()-392)/2);
		graph2.getContent().maxHeight((main.getStg().getHeight()-392)/2);
		graph3.getContent().maxHeight((main.getStg().getHeight()-392)/2);
		graph4.getContent().maxHeight((main.getStg().getHeight()-392)/2);
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
		gaugePane = new BootstrapPane(graphScrollPane);
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
		Platform.runLater(() -> graphPane.setPrefHeight(graphScrollPane.getHeight()));
		Platform.runLater(this::adjustHeight);
	}

	public BorderPane loadGraph(LineGraphTargetStrategy strategy){
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/GraphTile.fxml"));
		BorderPane graphTile = null;
		try {
			graphTile = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error loading graph tile", e);
		}
		GraphTileController gtc = loader.getController();
		gtc.setMain(main);
		gtc.setStrategy(strategy);
		gtc.fill();
		return graphTile;
	}

	public BorderPane loadGauge() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/GaugeTile.fxml"));
		BorderPane gaugeTile = null;
		try {
			gaugeTile = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error loading gauge tile", e);
		}
		GaugeTileController gtc = loader.getController();
		gtc.setMain(main);
		gtc.fill();
		VBox.setVgrow(gaugeTile, Priority.ALWAYS);
		return gaugeTile;
	}

	public void wtdView(){
		GUIUtils.formatTabSelect(wtdButton);
		GUIUtils.formatTabDeselect(mtdButton);
		GUIUtils.formatTabDeselect(ytdButton);
		LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
		LocalDate weekEnd = LocalDate.now().with(DayOfWeek.SUNDAY);
		updateGraphs(weekStart,weekEnd);
	}

	public void mtdView(){
		GUIUtils.formatTabSelect(mtdButton);
		GUIUtils.formatTabDeselect(wtdButton);
		GUIUtils.formatTabDeselect(ytdButton);
		LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
		LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		updateGraphs(monthStart,monthEnd);
	}

	public void ytdView(){
		GUIUtils.formatTabSelect(ytdButton);
		GUIUtils.formatTabDeselect(wtdButton);
		GUIUtils.formatTabDeselect(mtdButton);
		LocalDate yearStart = LocalDate.now().withDayOfYear(1);
		LocalDate yearEnd = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
		updateGraphs(yearStart,yearEnd);
	}

	public void adjustHeight(){
	 	graphPane.setPrefHeight(graphScrollPane.getHeight());
	}

	public void throwError (Exception e){
		dialogPane.showError("Error", e.getMessage());
	}
}
