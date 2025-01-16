package controllers;

import com.dlsc.gemsfx.DialogPane;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import models.DBTargetDatapoint;
import models.EODDataPoint;
import models.TillReportDataPoint;
import services.EODService;
import services.TargetService;
import services.TillReportService;
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
import utils.RosterUtils;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class TargetGraphsPageController extends PageController {
	
	@FXML private MFXScrollPane graphScrollPane;
	@FXML private BorderPane wtdButton;
    @FXML private BorderPane mtdButton;
    @FXML private BorderPane ytdButton;
    @FXML private DialogPane dialogPane;
	@FXML private MFXProgressBar progressBar;
    private BootstrapPane outerPane;
	private BootstrapPane graphPane;
	private BootstrapPane gaugePane;
	private TillReportService tillReportService;
	private EODService eodService;
	private TargetService targetService;

	@FXML
	private void initialize() {
		try {
			// 1) Eagerly create graphPane so it’s never null:
			graphPane = new BootstrapPane();
			// 2) Set up your listener
			graphScrollPane.heightProperty().addListener((obs, oldValue, newValue) -> {
				graphPane.setPrefHeight(newValue.doubleValue());
			});

			// 3) Possibly set it as content right away:
			graphScrollPane.setContent(graphPane);

			tillReportService = new TillReportService();
			eodService = new EODService();
			targetService = new TargetService();
			executor = Executors.newCachedThreadPool();
		} catch (IOException e){
			dialogPane.showError("Error", "Error initializing services", e);
		}
	}

	public Main getMain() {return main;}

	@Override
	public void fill() {
		 wtdView();
	}

	public void updateGraphs(LocalDate startDate,LocalDate endDate){
		 progressBar.setVisible(true);
		 CompletableFuture<RosterUtils> rosterUtilsFuture = CompletableFuture.supplyAsync(() -> {
			 try {
				 return new RosterUtils(main, startDate, endDate);
			 } catch (Exception e) {
				 return null;
			 }
		 }, executor);

		CompletableFuture<List<DBTargetDatapoint>> scriptCountTargetsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return targetService.getTargetsByKey(
						main.getCurrentStore().getStoreID(),
						"NoOfScripts",
						startDate,
						endDate
				);
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<DBTargetDatapoint>> otcCustomerTargetsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return targetService.getTargetsByKey(
						main.getCurrentStore().getStoreID(),
						"OTCCustomer",
						startDate,
						endDate
				);
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<DBTargetDatapoint>> gpDollarTargetsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return targetService.getTargetsByKey(
						main.getCurrentStore().getStoreID(),
						"GPDollar",
						startDate,
						endDate
				);
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<DBTargetDatapoint>> scriptsOnFileTargetsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return targetService.getTargetsByKey(
						main.getCurrentStore().getStoreID(),
						"ScriptsOnFile",
						startDate,
						endDate
				);
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<TillReportDataPoint>> scriptCountFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<TillReportDataPoint> currentTillReportDataPoints = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						startDate,
						endDate,
						"Script Count"
				);
				return currentTillReportDataPoints;
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<TillReportDataPoint>> otcCustomerFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<TillReportDataPoint> currentTillReportDataPoints = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						startDate,
						endDate,
						"Avg. OTC Sales Per Customer"
				);
				return currentTillReportDataPoints;
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture<List<TillReportDataPoint>> gpDollarFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<TillReportDataPoint> currentTillReportDataPoints = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						startDate,
						endDate,
						"Gross Profit ($)"
				);
				return currentTillReportDataPoints;
			} catch (Exception e) {
				return null;
			}
		}, executor);
		CompletableFuture<List<EODDataPoint>> scriptsOnFileFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<EODDataPoint> currentEODDataPoints = eodService.getEODDataPoints(
						main.getCurrentStore().getStoreID(),
						startDate,
						endDate
				);
				return currentEODDataPoints;
			} catch (Exception e) {
				return null;
			}
		}, executor);

		CompletableFuture.allOf(rosterUtilsFuture,scriptCountTargetsFuture,otcCustomerTargetsFuture,gpDollarTargetsFuture,scriptsOnFileTargetsFuture,scriptCountFuture,otcCustomerFuture,gpDollarFuture,scriptsOnFileFuture)
			.thenRunAsync(() -> {
				try {
					RosterUtils rosterUtils = rosterUtilsFuture.get();
					List<DBTargetDatapoint> noOfScriptsTargets = scriptCountTargetsFuture.get();
					List<DBTargetDatapoint> otcCustomerTargets = otcCustomerTargetsFuture.get();
					List<DBTargetDatapoint> gpDollarTargets = gpDollarTargetsFuture.get();
					List<DBTargetDatapoint> scriptsOnFileTargets = scriptsOnFileTargetsFuture.get();
					List<TillReportDataPoint> scriptCountTillReportDataPoints = scriptCountFuture.get();
					List<TillReportDataPoint> otcDollarTillReportDataPoints = otcCustomerFuture.get();
					List<TillReportDataPoint> gpDollarTillReportDataPoints = gpDollarFuture.get();
					List<EODDataPoint> scriptsOnFileDataPoints = scriptsOnFileFuture.get();

					Platform.runLater(() -> {
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
						BootstrapColumn graph1 = new BootstrapColumn(loadGraph(new NumberOfScriptsStrategy(startDate, endDate, this, scriptCountTillReportDataPoints, rosterUtils, noOfScriptsTargets)));
						BootstrapColumn graph2 = new BootstrapColumn(loadGraph(new OTCDollarPerCustomerStrategy(startDate, endDate, this, otcDollarTillReportDataPoints, rosterUtils, otcCustomerTargets)));
						BootstrapColumn graph3 = new BootstrapColumn(loadGraph(new GPDollarStrategy(startDate, endDate, this, gpDollarTillReportDataPoints, rosterUtils, gpDollarTargets)));
						BootstrapColumn graph4 = new BootstrapColumn(loadGraph(new ScriptsOnFileStrategy(startDate, endDate, this, scriptsOnFileDataPoints, rosterUtils, scriptsOnFileTargets)));
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
						graph1.getContent().maxHeight((main.getStg().getHeight() - 392) / 2);
						graph2.getContent().maxHeight((main.getStg().getHeight() - 392) / 2);
						graph3.getContent().maxHeight((main.getStg().getHeight() - 392) / 2);
						graph4.getContent().maxHeight((main.getStg().getHeight() - 392) / 2);
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
						BootstrapColumn gauge1 = new BootstrapColumn(loadGauge(new MedschecksStrategy()));
						BootstrapColumn gauge2 = new BootstrapColumn(loadGauge(new MedschecksStrategy()));
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
						Platform.runLater(() -> progressBar.setVisible(false));
					});
				} catch (Exception e) {
					Platform.runLater(() -> {
						dialogPane.showError("Error", "Error loading budget and expenses data", e);
						progressBar.setVisible(false);
					});
				}
			});
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

	public BorderPane loadGauge(GaugeTargetStrategy strategy){
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/GaugeTile.fxml"));
		BorderPane gaugeTile = null;
		try {
			gaugeTile = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error loading gauge tile", e);
		}
		GaugeTileController gtc = loader.getController();
		gtc.setMain(main);
		gtc.setStrategy(strategy);
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
