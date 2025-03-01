package controllers;

import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import services.EODService;
import services.TillReportService;
import utils.*;

import java.io.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EODDataEntryPageController extends DateSelectController{

	@FXML private TableView<EODDataPoint> eodDataTable;
	@FXML private VBox editDayPopover;
	@FXML private Region contentDarken;
	@FXML private Label popoverLabel,tillBalanceLabel,runningTillBalanceLabel,subheading;
	@FXML private MFXTextField cashField,eftposField,amexField,googleSquareField,chequeField;
	@FXML private MFXTextField medschecksField,sohField,sofField,smsPatientsField;
	@FXML private Label cashValidationLabel,eftposValidationLabel,amexValidationLabel,googleSquareValidationLabel,chequeValidationLabel;
	@FXML private Label medschecksValidationLabel,sohValidationLabel,sofValidationLabel,smsPatientsValidationLabel;
	@FXML private TextArea notesField;
	@FXML private MFXButton saveButton;
	@FXML private MFXScrollPane popOverScroll;
	@FXML private Button importDataButton,xeroExportButton;
	@FXML private MFXProgressSpinner progressSpinner;
    private TableColumn<EODDataPoint, String> notesCol;
	private double currentTotalTakings;
	private double currentRunningTillBalance;
	private EODService eodService;
	private TillReportService tillReportService;

	@FXML
	private void initialize() {
		try {
			eodService = new EODService();
			tillReportService = new TillReportService();
			executor = Executors.newCachedThreadPool();
		} catch (IOException e) {
			dialogPane.showError("Failed to initialize services", e);
		}
	}

	@Override
	public void fill() {
	 	//Fix slow scroll on popover scroll pane
		final double SPEED = 0.002;
		popOverScroll.getContent().setOnScroll(scrollEvent -> {
			double deltaY = scrollEvent.getDeltaY() * SPEED;
			popOverScroll.setVvalue(popOverScroll.getVvalue() - deltaY);
		});
		ValidatorUtils.setupRegexValidation(cashField,cashValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(eftposField,eftposValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(amexField,amexValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(googleSquareField,googleSquareValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(chequeField,chequeValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(medschecksField,medschecksValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(sohField,sohValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(sofField,sofValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(smsPatientsField,smsPatientsValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
        TableColumn<EODDataPoint, LocalDate> dateCol = new TableColumn<>("DATE");
        TableColumn<EODDataPoint, Double> cashAmountCol = new TableColumn<>("CASH");
        TableColumn<EODDataPoint, Double> eftposAmountCol = new TableColumn<>("EFTPOS");
        TableColumn<EODDataPoint, Double> amexAmountCol = new TableColumn<>("AMEX");
        TableColumn<EODDataPoint, Double> googleSquareAmountCol = new TableColumn<>("GOOGLE\nSQUARE");
        TableColumn<EODDataPoint, Double> chequeAmountCol = new TableColumn<>("CHEQUE");
        TableColumn<EODDataPoint, Integer> medschecksCol = new TableColumn<>("MEDSCHECKS");
        TableColumn<EODDataPoint, Double> stockOnHandAmountCol = new TableColumn<>("STOCK ON\nHAND");
        TableColumn<EODDataPoint, Integer> scriptsOnFileCol = new TableColumn<>("SCRIPTS ON\nFILE");
        TableColumn<EODDataPoint, Integer> smsPatientsCol = new TableColumn<>("SMS PATIENTS");
		TableColumn<EODDataPoint, String> tillBalanceCol = new TableColumn<>("TILL BALANCE");
        TableColumn<EODDataPoint, Double> runningTillBalanceCol = new TableColumn<>("RUNNING TILL\nBALANCE");
		notesCol = new TableColumn<>("NOTES");
		dateCol.setMinWidth(80);
		dateCol.setCellValueFactory(new PropertyValueFactory<>("dateString"));
		cashAmountCol.setCellValueFactory(new PropertyValueFactory<>("cashAmountString"));
		eftposAmountCol.setCellValueFactory(new PropertyValueFactory<>("eftposAmountString"));
		amexAmountCol.setCellValueFactory(new PropertyValueFactory<>("amexAmountString"));
		googleSquareAmountCol.setCellValueFactory(new PropertyValueFactory<>("googleSquareAmountString"));
		chequeAmountCol.setCellValueFactory(new PropertyValueFactory<>("chequeAmountString"));
		medschecksCol.setCellValueFactory(new PropertyValueFactory<>("medschecksString"));
		stockOnHandAmountCol.setCellValueFactory(new PropertyValueFactory<>("stockOnHandAmountString"));
		scriptsOnFileCol.setCellValueFactory(new PropertyValueFactory<>("scriptsOnFileString"));
		smsPatientsCol.setCellValueFactory(new PropertyValueFactory<>("smsPatientsString"));
		tillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("tillBalanceString"));
		runningTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("runningTillBalanceString"));
		notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
		tillBalanceCol.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Get the underlying data object for this row
                    EODDataPoint dataPoint = getTableView().getItems().get(getIndex());
                    if (dataPoint.getTillBalance() < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
		eodDataTable.getColumns().addAll(
                dateCol,
                cashAmountCol,
                eftposAmountCol,
                amexAmountCol,
                googleSquareAmountCol,
                chequeAmountCol,
                medschecksCol,
                stockOnHandAmountCol,
                scriptsOnFileCol,
                smsPatientsCol,
                tillBalanceCol,
                runningTillBalanceCol,
				notesCol
		);
		setDate(main.getCurrentDate());
		TableUtils.resizeTableColumns(eodDataTable,notesCol);
		cashField.textProperty().addListener(_ -> updatePopoverTillBalance());
		eftposField.textProperty().addListener(_ -> updatePopoverTillBalance());
		amexField.textProperty().addListener(_ -> updatePopoverTillBalance());
		googleSquareField.textProperty().addListener(_ -> updatePopoverTillBalance());
		chequeField.textProperty().addListener(_ -> updatePopoverTillBalance());
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Edit"))){
			Platform.runLater(this::addDoubleClickFunction);
		}else{
			subheading.setVisible(false);
		}
        xeroExportButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Export")));
	}

	private void updatePopoverTillBalance() {
		 double tillBalanceTotal = 0;
		 if(cashField.isValid()) tillBalanceTotal += Double.parseDouble(cashField.getText());
		 if(eftposField.isValid()) tillBalanceTotal += Double.parseDouble(eftposField.getText());
		 if(amexField.isValid()) tillBalanceTotal += Double.parseDouble(amexField.getText());
		 if(googleSquareField.isValid()) tillBalanceTotal += Double.parseDouble(googleSquareField.getText());
		 if(chequeField.isValid()) tillBalanceTotal += Double.parseDouble(chequeField.getText());
		tillBalanceTotal-=currentTotalTakings;
		tillBalanceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(tillBalanceTotal));
		runningTillBalanceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(currentRunningTillBalance+tillBalanceTotal));
	}

	private void addDoubleClickFunction(){
		eodDataTable.setRowFactory(_ -> {
			TableRow<EODDataPoint> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					EODDataPoint rowData = row.getItem();
					openEODPopover(rowData);
				}
			});
			return row ;
		});
	}

	public void importFiles(LocalDate targetDate) {
		// -- 1) Load configuration to get the last used directory (if any)
		Properties props = new Properties();
		File configFile = new File("appConfig.properties");
		if (configFile.exists()) {
			try (FileInputStream fis = new FileInputStream(configFile)) {
				props.load(fis);
			} catch (IOException e) {
				dialogPane.showError("Config Error", "Failed to load app configuration", e);
			}
		}

		String lastDir = props.getProperty("lastEODImportDir");

		// -- 2) Build and configure the FileChooser
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data Entry File(s)");
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("XLS Files", "*.xls"));

		// If we do have a stored last directory, set it as initial
		if (lastDir != null) {
			File lastDirFile = new File(lastDir);
			if (lastDirFile.exists() && lastDirFile.isDirectory()) {
				fileChooser.setInitialDirectory(lastDirFile);
			}
		}

		// -- 3) Show multiple-file selection dialog
		List<File> selectedFiles = fileChooser.showOpenMultipleDialog(main.getStg());
		if (selectedFiles == null || selectedFiles.isEmpty()) {
			return; // User canceled
		}

		// -- 4) Store the directory of the first chosen file as the "last import dir"
		File firstFile = selectedFiles.get(0);
		File parentDir = firstFile.getParentFile();
		if (parentDir != null) {
			props.setProperty("lastEODImportDir", parentDir.getAbsolutePath());
			try (FileOutputStream fos = new FileOutputStream(configFile)) {
				props.store(fos, "App configuration");
			} catch (IOException e) {
				dialogPane.showError("Config Error", "Failed to save app configuration", e);
			}
		}

		// -- 5) Continue with your existing checks and import logic
		List<String> allWarnings = new ArrayList<>();
		for (File file : selectedFiles) {
			boolean valid = checkFileForWarnings(file, targetDate, allWarnings);
			if (!valid) {
				// We already showed an error => stop everything
				return;
			}
		}

		// If warnings exist, show them
		if (!allWarnings.isEmpty()) {
			String combinedWarnings = String.join("\n", allWarnings);
			dialogPane.showWarning(
					"Import Warnings",
					combinedWarnings + "\n\nPress OK to proceed with import, or Cancel to stop."
			).thenAccept(buttonType -> {
				if (buttonType == ButtonType.OK) {
					importFilesSequentially(selectedFiles, targetDate, 0);
				}
			});
		} else {
			// No warnings => proceed directly
			importFilesSequentially(selectedFiles, targetDate, 0);
		}
	}

	/**
	 * Tries to open/parse the workbook for the given file and gather any warnings.
	 * Returns false if there's a fatal error, true if the file is basically valid.
	 */
	private boolean checkFileForWarnings(File file, LocalDate targetDate, List<String> warningsOut) {
		try (FileInputStream fis = new FileInputStream(file)) {
			HSSFWorkbook workbook;
			try {
				workbook = new HSSFWorkbook(fis);
			} catch (Exception e) {
				dialogPane.showError("File Error",
						"Invalid XLS format: " + file.getName());
				return false;
			}

			WorkbookProcessor wbp;
			try {
				wbp = new WorkbookProcessor(workbook);
			} catch (Exception e) {
				dialogPane.showError(
						"Workbook Error",
						"Please ensure 'Excel 97-2003' format, not data-only or csv.\nFile: " + file.getName(),
						e
				);
				return false;
			}

			LocalDateTime periodStart = wbp.getPeriodStart();
			LocalDateTime periodEnd   = wbp.getPeriodEnd();
			if (periodStart != null && periodEnd != null) {
				long daysBetween = Duration.between(periodStart, periodEnd).toDays();
				if (daysBetween > 1) {
					warningsOut.add("File " + file.getName()
							+ ": The report period spans multiple days; normally it’s just 1 day.");
				}
				long diffToTarget = Duration.between(periodEnd, targetDate.atStartOfDay()).toDays();
				if (diffToTarget > 1) {
					warningsOut.add("File " + file.getName()
							+ ": The period end date is far from the target date. Please verify.");
				}
			}

		} catch (IOException ioEx) {
			dialogPane.showError("File Error",
					"I/O error reading file: " + file.getName() + "\n" + ioEx.getMessage());
			return false;
		}

		return true;
	}

	private void importFilesSequentially(List<File> files, LocalDate targetDate, int index) {
		// If we've imported all files, we're done
		if (index >= files.size()) {
			return;
		}

		File currentFile = files.get(index);

		// 1) Show “Importing file X of Y” *non-blocking*
		dialogPane.showInformation(
						"Import Progress",
						String.format("Importing file %d of %d:\n%s",
								(index + 1), files.size(), currentFile.getName())
				)
				.thenAccept(btn -> {
					// 2) Only after they close the above dialog, do the actual import
					doSingleFileImport(currentFile, targetDate)
							.thenRun(() -> {
								// 3) Once current import is done (and success dialog closed),
								// import the next file
								importFilesSequentially(files, targetDate, index + 1);
							});
				});
	}

	private CompletableFuture<Void> doSingleFileImport(File newFile, LocalDate targetDate) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		try (FileInputStream fis = new FileInputStream(newFile)) {
			HSSFWorkbook workbook = new HSSFWorkbook(fis);
			WorkbookProcessor wbp = new WorkbookProcessor(workbook);

			// Build data
			List<TillReportDataPoint> importedData = new ArrayList<>();
			for (CellDataPoint cdp : wbp.getDataPoints()) {
				importedData.add(new TillReportDataPoint(
						cdp, wbp, targetDate, main.getCurrentStore().getStoreID()
				));
			}

			Task<Void> importTask = new Task<>() {
				@Override
				protected Void call() throws Exception {
					tillReportService.importTillReportData(importedData);
					return null;
				}
			};

			importTask.setOnRunning(e -> progressSpinner.setVisible(true));

			importTask.setOnSucceeded(e -> {
				progressSpinner.setVisible(false);
				dialogPane.showInformation("Success", "Data imported successfully")
						.thenAccept(btn -> {
							fillTable();
							future.complete(null); // signal done
						});
			});

			importTask.setOnFailed(e -> {
				progressSpinner.setVisible(false);
				Throwable exception = importTask.getException();
				if (exception instanceof IllegalArgumentException) {
					dialogPane.showWarning("Import Warning", exception.getMessage())
							.thenAccept(btn -> future.complete(null));
				} else {
					dialogPane.showError("Failed to Import Data",
									"Error while processing file: " + newFile.getName())
							.thenAccept(btn -> future.complete(null));
				}
			});

			executor.submit(importTask);

		} catch (IOException | RuntimeException e) {
			dialogPane.showError("File Error",
							"Cannot read file: " + newFile.getName() + "\n" + e.getMessage())
					.thenAccept(btn -> future.complete(null));
		}

		return future;
	}

	public void fillTable() {
		Task<ObservableList<EODDataPoint>> fillTableTask = new Task<>() {
			@Override
			protected ObservableList<EODDataPoint> call() throws Exception {
				ObservableList<EODDataPoint> eodDataPoints = FXCollections.observableArrayList();
				YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
				int daysInMonth = yearMonthObject.lengthOfMonth();

				// Create tasks for concurrent execution
				Callable<List<EODDataPoint>> eodDataCallable = () -> eodService.getEODDataPoints(
						main.getCurrentStore().getStoreID(),
						yearMonthObject.atDay(1),
						yearMonthObject.atEndOfMonth()
				);

				Callable<List<TillReportDataPoint>> tillReportDataCallable = () -> tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						yearMonthObject.atDay(1),
						yearMonthObject.atEndOfMonth(),
						"Total Takings"
				);

				// Submit tasks for execution
				Future<List<EODDataPoint>> eodDataFuture = executor.submit(eodDataCallable);
				Future<List<TillReportDataPoint>> tillReportDataFuture = executor.submit(tillReportDataCallable);

				// Wait for both tasks to complete and handle potential errors
				List<EODDataPoint> currentEODDataPoints;
				List<TillReportDataPoint> currentTillReportDataPoints;
				try {
					currentEODDataPoints = eodDataFuture.get();
					if (currentEODDataPoints == null) {
						throw new Exception("EODDataPoints returned null");
					}
				} catch (Exception e) {
					throw new Exception("Failed to retrieve EODDataPoints: " + e.getMessage(), e);
				}

				try {
					currentTillReportDataPoints = tillReportDataFuture.get();
					if (currentTillReportDataPoints == null) {
						throw new Exception("TillReportDataPoints returned null");
					}
				} catch (Exception e) {
					throw new Exception("Failed to retrieve TillReportDataPoints: " + e.getMessage(), e);
				}

				// Populate eodDataPoints
				for (int i = 1; i <= daysInMonth; i++) {
					LocalDate currentDate = LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), i);
					EODDataPoint existingDataPoint = currentEODDataPoints.stream()
							.filter(e -> e.getDate().equals(currentDate))
							.findFirst()
							.orElse(new EODDataPoint(currentDate));
					eodDataPoints.add(existingDataPoint);
				}

				// Calculate till balances
				double runningTillBalance = 0;
				for (EODDataPoint e : eodDataPoints) {
					TillReportDataPoint matchingTillReport = currentTillReportDataPoints.stream()
							.filter(t -> t.getAssignedDate().equals(e.getDate()))
							.findFirst()
							.orElse(null);

					double amount = (matchingTillReport != null) ? matchingTillReport.getAmount() : 0;
					e.calculateTillBalances(amount, runningTillBalance);
					runningTillBalance = e.getRunningTillBalance();
				}

				return eodDataPoints;
			}
		};

		fillTableTask.setOnSucceeded(_ -> {
			progressSpinner.setVisible(false);
			eodDataTable.setItems(fillTableTask.getValue());
			if (main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Edit"))) {
				addDoubleClickFunction();
			} else {
				subheading.setVisible(false);
			}
		});

		fillTableTask.setOnFailed(_ -> {
			progressSpinner.setVisible(false);
			Throwable exception = fillTableTask.getException();
			dialogPane.showError("Failed to fill table", (Exception) exception);
		});

		progressSpinner.setVisible(true);
		executor.submit(fillTableTask);
	}

	public void openEODPopover(EODDataPoint e) {
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(editDayPopover, 0);
		popoverLabel.setText("Modify EOD Values for " + e.getDateString());
		cashField.setText(String.valueOf(e.getCashAmount()));
		eftposField.setText(String.valueOf(e.getEftposAmount()));
		amexField.setText(String.valueOf(e.getAmexAmount()));
		googleSquareField.setText(String.valueOf(e.getGoogleSquareAmount()));
		chequeField.setText(String.valueOf(e.getChequeAmount()));
		tillBalanceLabel.setText(e.getTillBalanceString());
		runningTillBalanceLabel.setText(e.getRunningTillBalanceString());
		medschecksField.setText(String.valueOf(e.getMedschecks()));
		sohField.setText(String.valueOf(e.getStockOnHandAmount()));
		sofField.setText(String.valueOf(e.getScriptsOnFile()));
		smsPatientsField.setText(String.valueOf(e.getSmsPatients()));
		notesField.setText((e.getNotes() == null || e.getNotes().isBlank()) ? "" : String.valueOf(e.getNotes()));
		saveButton.setOnAction(_ -> editEODEntry(e));
		Task<Double> totalTakingsTask = new Task<>() {
			@Override
			protected Double call() {
				List<TillReportDataPoint> tillReports = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						e.getDate(),
						e.getDate(),
						"Total Takings"
				);
				return tillReports.stream().mapToDouble(TillReportDataPoint::getAmount).sum();
			}
		};
		totalTakingsTask.setOnSucceeded(_ -> {
			currentTotalTakings = totalTakingsTask.getValue();
			currentRunningTillBalance = e.getRunningTillBalance() - e.getTillBalance();
			updatePopoverTillBalance();
			progressSpinner.setVisible(false);
		});
		totalTakingsTask.setOnFailed(_ -> {
			dialogPane.showError("Failed to get total takings", (Exception) totalTakingsTask.getException());
			progressSpinner.setVisible(false);
		});
		progressSpinner.setVisible(true);
		executor.submit(totalTakingsTask);
		importDataButton.setOnAction(_ -> importFiles(e.getDate()));
	}

	public void closePopover(){
		AnimationUtils.slideIn(editDayPopover,425);
		contentDarken.setVisible(false);
	}

	public void editEODEntry(EODDataPoint e) {
		Task<Void> editTask = new Task<>() {
			@Override
			protected Void call() {
				double cashValue = Double.parseDouble(cashField.getText());
				double eftposValue = Double.parseDouble(eftposField.getText());
				double amexValue = Double.parseDouble(amexField.getText());
				double googleSquareValue = Double.parseDouble(googleSquareField.getText());
				double chequeValue = Double.parseDouble(chequeField.getText());
				int medschecksValue = Integer.parseInt(medschecksField.getText());
				double sohValue = Double.parseDouble(sohField.getText());
				int sofValue = Integer.parseInt(sofField.getText());
				int smsPatientsValue = Integer.parseInt(smsPatientsField.getText());
				String notesValue = notesField.getText();
				e.setCashAmount(cashValue);
				e.setEftposAmount(eftposValue);
				e.setAmexAmount(amexValue);
				e.setGoogleSquareAmount(googleSquareValue);
				e.setChequeAmount(chequeValue);
				e.setMedschecks(medschecksValue);
				e.setStockOnHandAmount(sohValue);
				e.setScriptsOnFile(sofValue);
				e.setSmsPatients(smsPatientsValue);
				e.setNotes(notesValue);
				if (e.isInDB()) {
					eodService.updateEODDataPoint(e);
				} else {
					eodService.insertEODDataPoint(new EODDataPoint(true, e.getDate(), main.getCurrentStore().getStoreID(), cashValue, eftposValue, amexValue, googleSquareValue, chequeValue, medschecksValue, sohValue, sofValue, smsPatientsValue, 0, 0, notesValue));
				}
				return null;
			}
		};
		editTask.setOnSucceeded(_ -> {
			progressSpinner.setVisible(false);
			dialogPane.showInformation("Success", e.isInDB() ? "EOD data was successfully edited" : "EOD data was successfully added");
			fillTable();
		});
		editTask.setOnFailed(_ -> {
			progressSpinner.setVisible(false);
			dialogPane.showError("Failed to edit EOD entry", (Exception) editTask.getException());
		});
		progressSpinner.setVisible(true);
		executor.submit(editTask);
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		fillTable();
	}

	public void exportToXero() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			Task<Void> exportTask = new Task<>() {
				@Override
				protected Void call() throws Exception {
					try (PrintWriter pw = new PrintWriter(file)) {
						pw.println("*ContactName,Day Of Month,Amount,No. of scripts,Total customers served," +
								"Total Sales (#),Total Govt Contribution ($),Total Takings,Gross Profit ($)," +
								"Total GST Free Sales,*InvoiceNumber,Total GST Sales,*InvoiceDate,*DueDate," +
								"Total GST Collected,*Description,*Quantity,*UnitAmount,Total OTC Sales (#)," +
								"Avg. OTC  Sales Per Customer ($),*AccountCode,*TaxType,Z Dispense Govt Cont," +
								"Stock on hand,Scripts on file count,SMS patients,Clinical Interventions,Medschecks," +
								"Till Balance,Running till Balance,Notes");
						YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
						int daysInMonth = yearMonthObject.lengthOfMonth();
						List<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
						List<TillReportDataPoint> currentTillDataPoints = FXCollections.observableArrayList();
						try {
							currentEODDataPoints = eodService.getEODDataPoints(
									main.getCurrentStore().getStoreID(),
									yearMonthObject.atDay(1),
									yearMonthObject.atEndOfMonth()
							);
							currentTillDataPoints = tillReportService.getTillReportDataPoints(
									main.getCurrentStore().getStoreID(),
									yearMonthObject.atDay(1),
									yearMonthObject.atEndOfMonth()
							);
						} catch (Exception ex) {
							dialogPane.showError("Failed to get EOD data", ex);
						}
						double runningTillBalance = 0;
						for (int i = 1; i < daysInMonth + 1; i++) {
							LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), i);
							EODDataPoint e = null;
							for (EODDataPoint eod : currentEODDataPoints) {
								if (eod.getDate().equals(d)) {
									e = eod;
									break;
								}
							}
							if (e == null) {
								e = new EODDataPoint(false, d, main.getCurrentStore().getStoreID(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
							}
							pw.print("Cash Income," + i + ",");
							pw.print(e.getCashAmount() + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Script Count", "quantity") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total Customers Served", "quantity") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total Sales", "quantity") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total Government Contribution", "amount") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total Takings", "amount") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Gross Profit ($)", "amount") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total GST Free Sales", "quantity") + ",");
							DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							String formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getCashAmount() > 0) ? formattedDate + "c," : ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total GST Sales", "amount") + ",");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getCashAmount() > 0) ? formattedDate + "," : ",");
							LocalDate lastDayOfMonth = d.withDayOfMonth(d.getMonth().length(d.isLeapYear()));
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((e.getCashAmount() > 0) ? formattedDate + "," : ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total GST Collected", "amount") + ",");
							pw.print("Cash Income,1,");
							pw.print(e.getCashAmount() + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Total Sales-OTC Sales", "quantity") + ",");
							pw.print(searchTillData(currentTillDataPoints, d, "Avg. OTC Sales Per Customer", "amount") + ",");
							pw.print("200,GST Free Income,");
							pw.print(searchTillData(currentTillDataPoints, d, "Govt Recovery", "amount") + ",");
							pw.print(e.getStockOnHandAmount() + ",");
							pw.print(e.getScriptsOnFile() + ",");
							pw.print(e.getSmsPatients() + ",");
							pw.print(",");//Clinical interventions
							pw.print(e.getMedschecks() + ",");
							double totalTakings = 0;
							try {
								String recoveryStr = searchTillData(currentTillDataPoints, d, "Total Takings", "amount");
								if (!recoveryStr.trim().isEmpty()) {
									totalTakings = Double.parseDouble(recoveryStr);
								}
							} catch (NumberFormatException ex) {
								dialogPane.showError("Failed to get total takings for " + d, ex);
							}
							double tillBalance = e.getCashAmount() + e.getEftposAmount() + e.getAmexAmount() + e.getGoogleSquareAmount() + e.getChequeAmount() - totalTakings;
							pw.print(NumberFormat.getCurrencyInstance(Locale.US).format(tillBalance) + ",");
							runningTillBalance += tillBalance;
							pw.print(NumberFormat.getCurrencyInstance(Locale.US).format(runningTillBalance) + ",");
							pw.println(e.getNotes());
							pw.print("Eftpos Income," + i + ",");
							pw.print(e.getEftposAmount() + ",,,,,,,,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getEftposAmount() > 0) ? formattedDate + "e,," : ",,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getEftposAmount() > 0) ? formattedDate + "," : ",");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((e.getEftposAmount() > 0) ? formattedDate + ",," : ",,");
							pw.println("Eftpos Income,1," + e.getEftposAmount() + ",,,200,GST Free Income");
							pw.print("Amex Income," + i + ",");
							pw.print(e.getAmexAmount() + ",,,,,,,,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getAmexAmount() > 0) ? formattedDate + "a,," : ",,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getAmexAmount() > 0) ? formattedDate + "," : ",");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((e.getAmexAmount() > 0) ? formattedDate + ",," : ",,");
							pw.println("Amex Income,1," + e.getAmexAmount() + ",,,200,GST Free Income");
							pw.print("Google Square Income," + i + ",");
							pw.print(e.getGoogleSquareAmount() + ",,,,,,,,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getGoogleSquareAmount() > 0) ? formattedDate + "gs,," : ",,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getGoogleSquareAmount() > 0) ? formattedDate + "," : ",");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((e.getGoogleSquareAmount() > 0) ? formattedDate + ",," : ",,");
							pw.println("Google Square Income,1," + e.getGoogleSquareAmount() + ",,,200,GST Free Income");
							pw.print("Cheques Income," + i + ",");
							pw.print(e.getChequeAmount() + ",,,,,,,,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getChequeAmount() > 0) ? formattedDate + "ch,," : ",,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((e.getChequeAmount() > 0) ? formattedDate + "," : ",");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((e.getChequeAmount() > 0) ? formattedDate + ",," : ",,");
							pw.println("Cheques Income,1," + e.getChequeAmount() + ",,,200,GST Free Income");
							double govtRecovery = 0;
							try {
								String recoveryStr = searchTillData(currentTillDataPoints, d, "Govt Recovery", "amount");
								if (!recoveryStr.trim().isEmpty()) {
									govtRecovery = Double.parseDouble(recoveryStr);
								}
							} catch (NumberFormatException ex) {
								dialogPane.showError("Failed to get govt recovery for " + d, ex);
							}
							pw.print("Medicare PBS (Ex GST)," + i + ",");
							pw.print(govtRecovery + ",,,,,,,,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((govtRecovery > 0) ? formattedDate + "ch,," : ",,");
							dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							formattedDate = d.format(dateTimeFormatter);
							pw.print((govtRecovery > 0) ? formattedDate + "," : ",");
							formattedDate = lastDayOfMonth.format(dateTimeFormatter);
							pw.print((govtRecovery > 0) ? formattedDate + ",," : ",,");
							pw.println("Medicare PBS (Ex GST),1," + govtRecovery + ",,,200,GST Free Income");
						}
					}
					return null;
				}
			};
			exportTask.setOnSucceeded(_ -> {
				progressSpinner.setVisible(false);
				dialogPane.showInformation("Success", "EOD data was successfully exported in Xero format");
			});
			exportTask.setOnFailed(_ -> {
				progressSpinner.setVisible(false);
				dialogPane.showError("Failed to export data", (Exception) exportTask.getException());
			});
			progressSpinner.setVisible(true);
			executor.submit(exportTask);
		}
	}

	private String searchTillData(List<TillReportDataPoint> dataPoints,LocalDate date, String key,String field){
		 for(TillReportDataPoint t:dataPoints){
			 if(t.getAssignedDate().equals(date)&&t.getKey().equals(key)){
				 if(field.equals("quantity")){
					 return String.valueOf(t.getQuantity());
				 }else{
					 return String.valueOf(t.getAmount());
				 }
			 }
		 }
		 return "";
	}
}
