package controllers;

import application.Main;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import models.CellDataPoint;
import models.Credit;
import models.Invoice;
import models.InvoiceSupplier;
import models.PdfEvidenceField;
import models.InvoiceReconciliation;
import models.PdfEvidenceLocation;
import models.ScannedInvoice;
import services.InvoiceService;
import services.CreditService;
import services.GeminiInvoiceScanService;
import services.PdfEvidenceLocator;
import services.PdfPreviewService;
import utils.InvoiceReconciler;
import utils.WorkbookProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/** Controller for the review-first PDF scan and reconciliation dialog. */
public final class AiInvoiceScanController extends Controller {
	private static final double LOW_FIELD_CONFIDENCE = InvoiceReconciler.REVIEW_CONFIDENCE;
	private static final double EVIDENCE_FILL_OPACITY = 0.08;
	private static final double EVIDENCE_STROKE_OPACITY = 0.72;
	private static final DateTimeFormatter PERIOD_DATE = DateTimeFormatter.ofPattern("d MMM uuuu");
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
	@FXML private ChoiceBox<String> documentKindChoice;
	@FXML private ListView<String> selectedFilesList;
	@FXML private Button chooseFilesButton;
	@FXML private Button scanButton;
	@FXML private CheckBox showMatchedCheck;
	@FXML private ProgressIndicator scanProgress;
	@FXML private Label configurationLabel;
	@FXML private Label summaryLabel;
	@FXML private Label acceptanceSummaryLabel;
	@FXML private Button saveAcceptedButton;
	@FXML private ProgressIndicator saveAcceptedProgress;
	@FXML private Pane scanSourceBar;
	@FXML private Pane scanSummaryBar;
	@FXML private Pane statementTotalsBar;
	@FXML private Label expectedStatementAmountLabel;
	@FXML private Label extractedStatementTotalLabel;
	@FXML private Label statementVarianceLabel;
	@FXML private Label expectedStatementConfidenceLabel;
	@FXML private VBox resultsView;
	@FXML private BorderPane reviewView;
	@FXML private TableView<InvoiceReconciliation> resultsTable;
	@FXML private ScrollPane pdfScrollPane;
	@FXML private StackPane pdfPageStack;
	@FXML private ImageView pdfPreviewImage;
	@FXML private Pane pdfOverlayPane;
	@FXML private Label previewDocumentLabel;
	@FXML private Label previewPageLabel;
	@FXML private Label previewHintLabel;
	@FXML private Button previousPageButton;
	@FXML private Button nextPageButton;
	@FXML private Label reviewTitleLabel;
	@FXML private Label reviewErrorLabel;
	@FXML private ComboBox<InvoiceSupplier> reviewSupplierChoice;
	@FXML private ChoiceBox<String> reviewTypeChoice;
	@FXML private TextField reviewReferenceField;
	@FXML private DatePicker reviewDateField;
	@FXML private Label reviewDueDateLabel;
	@FXML private DatePicker reviewDueDateField;
	@FXML private TextField reviewAmountField;
	@FXML private Label reviewExpectedAmountLabel;
	@FXML private Label reviewVarianceLabel;
	@FXML private TextArea reviewNotesField;
	@FXML private Button reviewBackButton;
	@FXML private Button reviewAcceptButton;

	private final ObservableList<File> selectedFiles = FXCollections.observableArrayList();
	private final ObservableList<InvoiceReconciliation> allResults = FXCollections.observableArrayList();
	private final Map<ScannedInvoice, AcceptedDocument> acceptedDocuments = new HashMap<>();
	private final Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> evidenceLocations = new HashMap<>();
	private final Map<ScannedInvoice, File> evidenceFiles = new HashMap<>();
	private final GeminiInvoiceScanService scanService = new GeminiInvoiceScanService();
	private InvoiceEntryController parent;
	private InvoiceService invoiceService;
	private CreditService creditService;
	private ExecutorService executor;
	private List<InvoiceSupplier> availableSuppliers = List.of();
	private InvoiceReconciliation currentReview;
	private LocalDate currentStatementPeriodStart;
	private LocalDate currentStatementPeriodEnd;
	private boolean currentResultIsStatement;
	private Long currentStatementAmountCents;
	private Double currentStatementAmountConfidence;
	private File currentPreviewFile;
	private Map<PdfEvidenceField, PdfEvidenceLocation> currentEvidence = Map.of();
	private Image currentPreviewImage;
	private int currentPageIndex;
	private int currentPageCount;
	private double currentPageWidth;
	private double currentPageHeight;
	private double previewZoom = 0.5;
	private long previewRequest;
	private boolean commitBusy;
	private static final AtomicInteger SCAN_THREAD_NUMBER = new AtomicInteger();

	@FXML
	private void initialize() {
		documentKindChoice.getItems().setAll("Supplier statement", "Individual invoice(s)");
		documentKindChoice.getSelectionModel().selectFirst();
		documentKindChoice.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
			selectedFiles.clear();
			// A new individual-invoice selection must not retain statement-only
			// statistics from a previous scan while the user is choosing files.
			currentResultIsStatement = false;
			updateStatementTotals();
		});
		reviewTypeChoice.getItems().setAll("Invoice", "Credit");
		reviewTypeChoice.getSelectionModel().selectedItemProperty().addListener((_, _, value) -> updateReviewType(value));
		reviewAmountField.textProperty().addListener((_, _, _) -> updateReviewReconciliationValues());
		selectedFiles.addListener((javafx.collections.ListChangeListener<File>) _ -> refreshSelectedFiles());
		showMatchedCheck.selectedProperty().addListener((_, _, _) -> applyResultFilter());
		configureTable();
		configurationLabel.setVisible(!scanService.isConfigured());
		configurationLabel.setManaged(!scanService.isConfigured());
	}

	void configure(InvoiceEntryController parent, Main main, InvoiceService invoiceService,
			CreditService creditService, ExecutorService executor) {
		this.parent = parent;
		this.main = main;
		this.invoiceService = invoiceService;
		this.creditService = creditService;
		this.executor = executor;
		parent.fetchContactData().whenComplete((suppliers, error) -> Platform.runLater(() -> {
			if (error != null) {
				reviewErrorLabel.setText("Unable to load suppliers: " + rootCause(error).getMessage());
				reviewErrorLabel.setVisible(true);
				reviewErrorLabel.setManaged(true);
				return;
			}
			availableSuppliers = suppliers == null ? List.of() : List.copyOf(suppliers);
			reviewSupplierChoice.getItems().setAll(availableSuppliers);
			if (currentReview != null) selectReviewSupplier(currentReview.getScanned().supplierName());
			updateAcceptanceControls();
		}));
	}

	@FXML
	private void chooseFiles() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(isStatement() ? "Choose supplier statement PDF" : "Choose invoice PDFs");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF documents", "*.pdf"));
		List<File> chosen;
		if (isStatement()) {
			File file = chooser.showOpenDialog(main.getStg());
			chosen = file == null ? List.of() : List.of(file);
		} else {
			chosen = chooser.showOpenMultipleDialog(main.getStg());
			if (chosen == null) chosen = List.of();
		}
		if (!chosen.isEmpty()) selectedFiles.setAll(chosen);
	}

	@FXML
	private void scanDocuments() {
		if (selectedFiles.isEmpty()) {
			parent.getDialogPane().showWarning("No PDF selected", "Choose a statement or one or more invoice PDFs first.");
			return;
		}
		if (!scanService.isConfigured()) {
			parent.getDialogPane().showError("Document AI is not configured",
					"Configure the document AI API key before launching Alpha Income.");
			return;
		}

		setBusy(true);
		List<File> files = List.copyOf(selectedFiles);
		GeminiInvoiceScanService.ScanKind kind = isStatement()
				? GeminiInvoiceScanService.ScanKind.STATEMENT
				: GeminiInvoiceScanService.ScanKind.INDIVIDUAL_INVOICE;
		Task<ScanRun> task = new Task<>() {
			@Override
			protected ScanRun call() throws Exception {
				List<ScannedInvoice> scanned = new ArrayList<>();
				Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new HashMap<>();
				Map<ScannedInvoice, File> filesByRow = new HashMap<>();
				int parallelism = Math.min(files.size(), scanService.parallelScanLimit());
				updateProgress(0, files.size());
				updateMessage(files.size() == 1
						? "Scanning 0 of 1 PDF" + (kind == GeminiInvoiceScanService.ScanKind.STATEMENT
								? " and determining its statement period" : "") + " with document AI…"
						: "Scanning 0 of " + files.size() + " invoice PDFs with document AI…");
				BiConsumer<Integer, Integer> onStatementChunkProgress = (completed, total) -> {
					updateProgress(completed, total);
					updateMessage("Scanning " + completed + " of " + total + " statement chunks…");
				};
				List<FileScan> fileScans = scanFilesInParallel(files, kind,
						parallelism, this, completed -> {
					updateProgress(completed, files.size());
					updateMessage("Scanning " + completed + " of " + files.size()
							+ (files.size() == 1 ? " PDF…" : " PDFs…"));
				}, onStatementChunkProgress);
				LocalDate periodStart = null;
				LocalDate periodEnd = null;
				Long statementAmountCents = null;
				Double statementAmountConfidence = null;
				for (FileScan fileScan : fileScans) {
					scanned.addAll(fileScan.rows());
					fileScan.rows().forEach(row -> filesByRow.put(row, fileScan.file()));
					locations.putAll(fileScan.locations());
					if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT && fileScan.periodStart() != null
							&& (periodStart == null || fileScan.periodStart().isBefore(periodStart))) {
						periodStart = fileScan.periodStart();
					}
					if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT && fileScan.periodEnd() != null
							&& (periodEnd == null || fileScan.periodEnd().isAfter(periodEnd))) {
						periodEnd = fileScan.periodEnd();
					}
					if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT && fileScan.statementAmountCents() != null) {
						statementAmountCents = fileScan.statementAmountCents();
						statementAmountConfidence = fileScan.statementAmountConfidence();
					}
				}
				if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT
						&& (periodStart == null || periodEnd == null)) {
					throw new IllegalStateException("Document AI could not determine the statement's reporting period. "
							+ "Check that the statement date or period is visible in the PDF.");
				}
				if (scanned.isEmpty()) {
					throw new IllegalStateException(kind == GeminiInvoiceScanService.ScanKind.STATEMENT
							? "Document AI did not find any invoice or credit transactions inside the inferred statement period "
									+ periodLabel(periodStart, periodEnd) + "."
							: "Document AI did not find any invoice or credit transactions in the selected PDF.");
				}
				updateMessage("Loading Z-Office data…");
				List<Invoice> imported = loadImportedRows(scanned, kind, periodStart, periodEnd);
				return new ScanRun(scanned, InvoiceReconciler.reconcile(scanned, imported), locations, filesByRow,
						periodStart, periodEnd, statementAmountCents, statementAmountConfidence);
			}
		};
		summaryLabel.textProperty().bind(task.messageProperty());
		task.setOnSucceeded(_ -> {
			summaryLabel.textProperty().unbind();
			evidenceLocations.clear();
			evidenceLocations.putAll(task.getValue().locations());
			evidenceFiles.clear();
			evidenceFiles.putAll(task.getValue().filesByRow());
			currentStatementPeriodStart = task.getValue().periodStart();
			currentStatementPeriodEnd = task.getValue().periodEnd();
			currentResultIsStatement = kind == GeminiInvoiceScanService.ScanKind.STATEMENT;
			currentStatementAmountCents = task.getValue().statementAmountCents();
			currentStatementAmountConfidence = task.getValue().statementAmountConfidence();
			acceptedDocuments.clear();
			allResults.setAll(task.getValue().reconciliations());
			applyResultFilter();
			updateSummary(task.getValue().scanned().size());
			setBusy(false);
			if (reviewView.isVisible()) backToResults();
			if (!resultsTable.getItems().isEmpty()) resultsTable.getSelectionModel().selectFirst();
		});
		task.setOnFailed(_ -> {
			summaryLabel.textProperty().unbind();
			summaryLabel.setText("Scan failed. No invoices were changed.");
			setBusy(false);
			Throwable error = task.getException();
			String message = errorMessage(error);
			if (error != null) error.printStackTrace(System.err);
			parent.getDialogPane().showError("AI scan failed", message,
					error instanceof Exception exception ? exception : new RuntimeException(error));
		});
		executor.submit(task);
	}

	private List<FileScan> scanFilesInParallel(List<File> files, GeminiInvoiceScanService.ScanKind kind,
			int parallelism, Task<?> parentTask,
			IntConsumer onCompleted, BiConsumer<Integer, Integer> onStatementChunkProgress) throws Exception {
		ExecutorService scanExecutor = Executors.newFixedThreadPool(parallelism, runnable -> {
			Thread thread = new Thread(runnable, "gemini-invoice-scan-" + SCAN_THREAD_NUMBER.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
		CompletionService<FileScan> completion = new ExecutorCompletionService<>(scanExecutor);
		List<Future<FileScan>> futures = new ArrayList<>(files.size());
		List<FileScan> ordered = new ArrayList<>(Collections.nCopies(files.size(), null));
		try {
			for (int index = 0; index < files.size(); index++) {
				int selectedIndex = index;
				File file = files.get(index);
				futures.add(completion.submit(() -> scanFile(selectedIndex, file, kind, onStatementChunkProgress)));
			}
			for (int completed = 1; completed <= files.size(); completed++) {
				if (parentTask.isCancelled()) throw new InterruptedException("Invoice scanning was cancelled");
				FileScan result;
				try {
					result = completion.take().get();
				} catch (ExecutionException exception) {
					Throwable cause = exception.getCause();
					if (cause instanceof InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw interrupted;
					}
					if (cause instanceof Exception checked) throw checked;
					throw new RuntimeException(cause);
				}
				ordered.set(result.index(), result);
				// A statement reports progress for its page chunks directly. The
				// file-level callback would overwrite that more useful X-of-Y value.
				if (!(kind == GeminiInvoiceScanService.ScanKind.STATEMENT && files.size() == 1)) {
					onCompleted.accept(completed);
				}
			}
			return ordered;
		} finally {
			futures.forEach(future -> future.cancel(true));
			scanExecutor.shutdownNow();
		}
	}

	private FileScan scanFile(int index, File sourceFile, GeminiInvoiceScanService.ScanKind kind,
			BiConsumer<Integer, Integer> onStatementChunkProgress)
			throws IOException, InterruptedException {
		GeminiInvoiceScanService.ScanResult result;
		try {
			result = scanService.scan(sourceFile, kind, (completed, total) -> {
				if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT) {
					onStatementChunkProgress.accept(completed, total);
				}
			});
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw exception;
		} catch (IOException exception) {
			throw new IOException("Document AI failed for " + sourceFile.getName()
					+ ". No data from this PDF was imported.", exception);
		}

		List<ScannedInvoice> extracted = result.rows();
		// Prefer document AI's best-estimate boxes. The local text locator fills
		// only values document AI could not place, especially for searchable PDFs.
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> fallback = Map.of();
		try {
			fallback = PdfEvidenceLocator.locateFields(sourceFile, extracted);
		} catch (Exception ignored) {
			// Image-only or protected PDFs may not have a searchable text layer.
		}
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new HashMap<>();
		for (ScannedInvoice row : extracted) {
			Map<PdfEvidenceField, PdfEvidenceLocation> merged = new java.util.EnumMap<>(PdfEvidenceField.class);
			merged.putAll(fallback.getOrDefault(row, Map.of()));
			merged.putAll(result.locations().getOrDefault(row, Map.of()));
			if (!merged.isEmpty()) locations.put(row, Map.copyOf(merged));
		}
		return new FileScan(index, sourceFile, extracted, Map.copyOf(locations),
				result.periodStart(), result.periodEnd(), result.statementAmountCents(),
				result.statementAmountConfidence());
	}

	private List<Invoice> loadImportedRows(List<ScannedInvoice> scanned, GeminiInvoiceScanService.ScanKind kind,
			LocalDate periodStart, LocalDate periodEnd) {
		List<YearMonth> months = new ArrayList<>();
		if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT && periodStart != null && periodEnd != null) {
			YearMonth month = YearMonth.from(periodStart);
			YearMonth finalMonth = YearMonth.from(periodEnd);
			while (!month.isAfter(finalMonth)) {
				months.add(month);
				month = month.plusMonths(1);
			}
		} else {
			months.add(YearMonth.from(main.getCurrentDate()));
		}
		List<Invoice> imported = new ArrayList<>();
		for (YearMonth month : months) {
			List<Invoice> tableRows = invoiceService.getInvoiceTableData(main.getCurrentStore().getStoreID(), month);
			List<Invoice> snapshot = loadImportSnapshot(month, tableRows);
			imported.addAll(snapshot.isEmpty() ? tableRows : snapshot);
		}
		Set<String> knownReferences = new HashSet<>();
		imported.stream().filter(Invoice::isImportExists)
				.forEach(invoice -> knownReferences.add(InvoiceReconciler.reference(invoice.getInvoiceNo())));
		for (ScannedInvoice row : scanned) {
			String reference = InvoiceReconciler.reference(row.invoiceNo());
			if (reference.isBlank() || knownReferences.contains(reference)) continue;
			Invoice resolved = invoiceService.getInvoice(row.invoiceNo(), main.getCurrentStore().getStoreID());
			if (resolved != null && resolved.isImportExists()) {
				imported.add(resolved);
				knownReferences.add(reference);
			}
		}
		return imported;
	}

	private List<Invoice> loadImportSnapshot(YearMonth month, List<Invoice> tableRows) {
		Properties properties = new Properties();
		File configuration = new File("appConfig.properties");
		if (!configuration.isFile()) return new ArrayList<>();
		try (FileInputStream input = new FileInputStream(configuration)) {
			properties.load(input);
			String key = InvoiceEntryController.importSnapshotKey(main.getCurrentStore().getStoreID(), month);
			String path = properties.getProperty(key);
			if (path == null || path.isBlank()) return new ArrayList<>();
			File export = new File(path);
			if (!export.isFile()) return new ArrayList<>();
			try (FileInputStream workbookInput = new FileInputStream(export);
				 HSSFWorkbook workbook = new HSSFWorkbook(workbookInput)) {
				List<Invoice> rows = new ArrayList<>();
				for (CellDataPoint point : new WorkbookProcessor(workbook).getDataPoints()) {
					Invoice invoice = new Invoice();
					invoice.setInvoiceNo(point.getCategory());
					invoice.setImportedInvoiceAmount(point.getAmount());
					invoice.setImportExists(true);
					tableRows.stream()
							.filter(existing -> InvoiceReconciler.reference(existing.getInvoiceNo())
									.equals(InvoiceReconciler.reference(point.getCategory())))
							.findFirst()
							.ifPresent(existing -> {
								invoice.setSupplierName(existing.getSupplierName());
								invoice.setSupplierID(existing.getSupplierID());
								invoice.setStoreID(existing.getStoreID());
								invoice.setInvoiceDate(existing.getInvoiceDate());
								invoice.setDueDate(existing.getDueDate());
								invoice.setDescription(existing.getDescription());
								invoice.setNotes(existing.getNotes());
							});
					rows.add(invoice);
				}
				return rows;
			}
		} catch (Exception ignored) {
			// The API fallback below still supports scan-driven comparisons if the
			// original local export has moved or can no longer be read.
			return new ArrayList<>();
		}
	}

	private void configureTable() {
		TableColumn<InvoiceReconciliation, String> status = column("STATUS", "statusLabel", 135);
		TableColumn<InvoiceReconciliation, String> supplier = confidenceColumn(
				"SUPPLIER", "supplierName", 180, InvoiceReconciliation::getSupplierConfidence);
		TableColumn<InvoiceReconciliation, String> reference = confidenceColumn(
				"REFERENCE", "invoiceNo", 135, InvoiceReconciliation::getReferenceConfidence);
		TableColumn<InvoiceReconciliation, String> type = confidenceColumn(
				"TYPE", "documentType", 80, InvoiceReconciliation::getTypeConfidence);
		TableColumn<InvoiceReconciliation, String> scannedDate = confidenceColumn(
				"OCR DATE", "scannedDateString", 90, InvoiceReconciliation::getDateConfidence);
		TableColumn<InvoiceReconciliation, String> scanned = confidenceColumn(
				"SCANNED", "scannedAmountString", 100, InvoiceReconciliation::getAmountConfidence);
		TableColumn<InvoiceReconciliation, String> imported = column("EXPECTED", "importedAmountString", 100);
		TableColumn<InvoiceReconciliation, String> variance = column("VARIANCE", "varianceString", 100);
		TableColumn<InvoiceReconciliation, Void> action = new TableColumn<>("ACTION");
		action.setPrefWidth(165);
		action.setSortable(false);
		action.setCellFactory(_ -> new TableCell<>() {
			private final Button reviewButton = new Button("Review");
			private final Button deleteButton = new Button("Delete");
			private final HBox buttons = new HBox(5, reviewButton, deleteButton);
			{
				reviewButton.getStyleClass().add("review-add-button");
				reviewButton.setOnAction(_ -> review(getTableView().getItems().get(getIndex())));
				deleteButton.getStyleClass().add("delete-scan-button");
				deleteButton.setOnAction(_ -> deleteScannedRow(getTableView().getItems().get(getIndex())));
			}
			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				InvoiceReconciliation row = empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
						? null : getTableView().getItems().get(getIndex());
				boolean saved = row != null && row.getStatus() == InvoiceReconciliation.Status.SAVED;
				reviewButton.setText(saved ? "Saved" : "Review");
				reviewButton.setDisable(saved || commitBusy);
				deleteButton.setDisable(saved || commitBusy);
				setGraphic(row == null || row.getScanned() == null ? null : buttons);
			}
		});
		resultsTable.getColumns().setAll(status, supplier, reference, type, scannedDate, scanned, imported, variance, action);
		// Let rows with a low-confidence annotation expand while keeping ordinary
		// one-line rows compact. A fixed cell size would waste vertical space.
		resultsTable.setFixedCellSize(-1);
		resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		resultsTable.setPlaceholder(new Label("Scan PDFs to see the comparison with imported Z-Office data."));
		resultsTable.setRowFactory(_ -> {
			TableRow<InvoiceReconciliation> row = new TableRow<>() {
				@Override
				protected void updateItem(InvoiceReconciliation item, boolean empty) {
					super.updateItem(item, empty);
					double rowHeight = hasLowConfidenceAnnotation(item) ? 48 : 32;
					setMinHeight(rowHeight);
					setPrefHeight(rowHeight);
					setMaxHeight(rowHeight);
					getStyleClass().removeAll("reconciliation-good", "reconciliation-attention", "reconciliation-review");
					if (!empty && item != null) {
						if (item.getStatus() == InvoiceReconciliation.Status.SAVED
								|| item.getStatus() == InvoiceReconciliation.Status.ACCEPTED
								|| item.getStatus() == InvoiceReconciliation.Status.MATCHED) getStyleClass().add("reconciliation-good");
						else if (item.getStatus() == InvoiceReconciliation.Status.WITHIN_TOLERANCE) getStyleClass().add("reconciliation-review");
						else getStyleClass().add("reconciliation-attention");
					}
				}
			};
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && !row.isEmpty() && row.getItem().getScanned() != null) review(row.getItem());
			});
			return row;
		});
	}

	private void showEvidence(InvoiceReconciliation reconciliation) {
		if (reconciliation == null || reconciliation.getScanned() == null) return;
		ScannedInvoice scanned = reconciliation.getScanned();
		File file = evidenceFiles.get(scanned);
		if (file == null) {
			previewRequest++;
			currentPreviewFile = null;
			currentEvidence = Map.of();
			pdfPreviewImage.setImage(null);
			pdfOverlayPane.getChildren().clear();
			previewPageLabel.setText("No PDF loaded");
			previousPageButton.setDisable(true);
			nextPageButton.setDisable(true);
			previewHintLabel.setText("The original PDF is no longer available for preview.");
			return;
		}
		currentPreviewFile = file;
		currentEvidence = evidenceLocations.getOrDefault(scanned, Map.of());
		previewDocumentLabel.setText(file.getName() + " · " + scanned.invoiceNo());
		PdfEvidenceLocation primaryEvidence = primaryEvidence(currentEvidence);
		int page = primaryEvidence == null ? 0 : primaryEvidence.pageIndex();
		renderPreviewPage(page, true);
	}

	private void renderPreviewPage(int pageIndex, boolean fitWidth) {
		if (currentPreviewFile == null) return;
		long request = ++previewRequest;
		File file = currentPreviewFile;
		previewHintLabel.setText("Rendering page…");
		Task<PdfPreviewService.RenderedPage> task = new Task<>() {
			@Override
			protected PdfPreviewService.RenderedPage call() throws Exception {
				return PdfPreviewService.render(file, pageIndex);
			}
		};
		task.setOnSucceeded(_ -> {
			if (request != previewRequest) return;
			PdfPreviewService.RenderedPage rendered = task.getValue();
			currentPreviewImage = new Image(new ByteArrayInputStream(rendered.png()));
			currentPageIndex = rendered.pageIndex();
			currentPageCount = rendered.pageCount();
			currentPageWidth = rendered.pageWidth();
			currentPageHeight = rendered.pageHeight();
			if (fitWidth) {
				double viewportWidth = pdfScrollPane.getViewportBounds().getWidth();
				if (viewportWidth > 40) previewZoom = Math.min(1.0, (viewportWidth - 24) / currentPreviewImage.getWidth());
			}
			pdfPreviewImage.setImage(currentPreviewImage);
			updatePreviewGeometry();
			previewPageLabel.setText("Page " + (currentPageIndex + 1) + " of " + currentPageCount);
			previousPageButton.setDisable(currentPageIndex <= 0);
			nextPageButton.setDisable(currentPageIndex >= currentPageCount - 1);
			previewHintLabel.setText(currentEvidence.isEmpty()
					? "No searchable OCR values were found; inspect the rendered PDF manually."
					: "Colour-coded boxes mark the supplier, invoice reference, date, and amount when found.");
		});
		task.setOnFailed(_ -> {
			if (request != previewRequest) return;
			previewHintLabel.setText("Unable to render this PDF: " + rootCause(task.getException()).getMessage());
		});
		executor.submit(task);
	}

	private void updatePreviewGeometry() {
		if (currentPreviewImage == null) return;
		double displayWidth = currentPreviewImage.getWidth() * previewZoom;
		double displayHeight = currentPreviewImage.getHeight() * previewZoom;
		pdfPreviewImage.setFitWidth(displayWidth);
		pdfPreviewImage.setFitHeight(displayHeight);
		pdfPageStack.setMinSize(displayWidth, displayHeight);
		pdfPageStack.setPrefSize(displayWidth, displayHeight);
		pdfPageStack.setMaxSize(displayWidth, displayHeight);
		pdfOverlayPane.setMinSize(displayWidth, displayHeight);
		pdfOverlayPane.setPrefSize(displayWidth, displayHeight);
		pdfOverlayPane.setMaxSize(displayWidth, displayHeight);
		pdfOverlayPane.getChildren().clear();
		if (currentEvidence.isEmpty()) return;
		double focusX = 0;
		double focusY = 0;
		boolean hasFocus = false;
		for (Map.Entry<PdfEvidenceField, PdfEvidenceLocation> entry : currentEvidence.entrySet()) {
			PdfEvidenceLocation evidence = entry.getValue();
			if (evidence.pageIndex() != currentPageIndex) continue;
			double x = evidence.x() / currentPageWidth * displayWidth;
			double y = evidence.y() / currentPageHeight * displayHeight;
			double width = evidence.width() / currentPageWidth * displayWidth;
			double height = evidence.height() / currentPageHeight * displayHeight;
			Rectangle highlight = new Rectangle(x, y, width, height);
			highlight.setFill(Color.web(entry.getKey().color(), EVIDENCE_FILL_OPACITY));
			highlight.setStroke(Color.web(entry.getKey().color(), EVIDENCE_STROKE_OPACITY));
			highlight.setStrokeWidth(entry.getKey() == PdfEvidenceField.REFERENCE ? 2.0 : 1.5);
			highlight.getStrokeDashArray().setAll(8.0, 4.0);
			pdfOverlayPane.getChildren().add(highlight);
			if (!hasFocus || entry.getKey() == PdfEvidenceField.REFERENCE) {
				focusX = x + width / 2;
				focusY = y + height / 2;
				hasFocus = true;
			}
		}
		if (hasFocus) {
			final double centeredX = focusX;
			final double centeredY = focusY;
			Platform.runLater(() -> centerPreviewOn(centeredX, centeredY, displayWidth, displayHeight));
		}
	}

	private static PdfEvidenceLocation primaryEvidence(Map<PdfEvidenceField, PdfEvidenceLocation> evidence) {
		PdfEvidenceLocation reference = evidence.get(PdfEvidenceField.REFERENCE);
		if (reference != null) return reference;
		return evidence.values().stream().findFirst().orElse(null);
	}

	private void centerPreviewOn(double x, double y, double contentWidth, double contentHeight) {
		double viewportWidth = pdfScrollPane.getViewportBounds().getWidth();
		double viewportHeight = pdfScrollPane.getViewportBounds().getHeight();
		if (contentWidth > viewportWidth) {
			double horizontal = (x - viewportWidth / 2) / (contentWidth - viewportWidth);
			pdfScrollPane.setHvalue(Math.max(0, Math.min(1, horizontal)));
		}
		if (contentHeight > viewportHeight) {
			double vertical = (y - viewportHeight / 2) / (contentHeight - viewportHeight);
			pdfScrollPane.setVvalue(Math.max(0, Math.min(1, vertical)));
		}
	}

	@FXML
	private void previousPreviewPage() {
		renderPreviewPage(currentPageIndex - 1, false);
	}

	@FXML
	private void nextPreviewPage() {
		renderPreviewPage(currentPageIndex + 1, false);
	}

	@FXML
	private void zoomPreviewIn() {
		previewZoom = Math.min(2.0, previewZoom * 1.2);
		updatePreviewGeometry();
	}

	@FXML
	private void zoomPreviewOut() {
		previewZoom = Math.max(0.2, previewZoom / 1.2);
		updatePreviewGeometry();
	}

	private static TableColumn<InvoiceReconciliation, String> column(String title, String property, double width) {
		TableColumn<InvoiceReconciliation, String> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>(property));
		column.setPrefWidth(width);
		return column;
	}

	private static boolean hasLowConfidenceAnnotation(InvoiceReconciliation row) {
		if (row == null || row.getScanned() == null || row.isAccepted()) return false;
		ScannedInvoice.FieldConfidences confidence = row.getScanned().fieldConfidences();
		// Only fields that render a warning line in the results table should
		// increase its row height. Due date confidence is reviewed in the edit
		// pane, but due date is intentionally not a results-table column.
		return isLowConfidence(confidence.supplier()) || isLowConfidence(confidence.reference())
				|| isLowConfidence(confidence.date())
				|| isLowConfidence(confidence.type()) || isLowConfidence(confidence.amount());
	}

	private static boolean isLowConfidence(Double score) {
		return score != null && score < LOW_FIELD_CONFIDENCE;
	}

	private static TableColumn<InvoiceReconciliation, String> confidenceColumn(String title, String property,
			double width, Function<InvoiceReconciliation, Double> confidence) {
		TableColumn<InvoiceReconciliation, String> column = column(title, property, width);
		column.setCellFactory(_ -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				getStyleClass().remove("low-confidence-field");
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setText(null);
					setTooltip(null);
					return;
				}
				Double score = confidence.apply(getTableRow().getItem());
				Long percentage = score == null ? null : Math.round(score * 100);
				String valueText = item == null || item.isBlank() ? "—" : item;
				boolean concerning = !getTableRow().getItem().isAccepted()
						&& score != null && score < LOW_FIELD_CONFIDENCE;
				if (concerning) {
					String confidenceText = "⚠ " + percentage + "% confidence";
					setText(valueText + "\n" + confidenceText);
					setTooltip(new Tooltip("Low document AI confidence: " + percentage + "%"));
					getStyleClass().add("low-confidence-field");
				} else {
					setText(valueText);
					setTooltip(null);
				}
			}
		});
		return column;
	}

	private void review(InvoiceReconciliation row) {
		if (row == null || row.getScanned() == null || row.getStatus() == InvoiceReconciliation.Status.SAVED) return;
		currentReview = row;
		ScannedInvoice scanned = row.getScanned();
		AcceptedDocument accepted = acceptedDocuments.get(scanned);
		ScannedInvoice.DocumentType type = accepted == null ? scanned.documentType() : accepted.documentType();
		reviewTitleLabel.setText("Review " + (type == ScannedInvoice.DocumentType.CREDIT
				? "credit" : "invoice"));
		reviewTypeChoice.getSelectionModel().select(
				type == ScannedInvoice.DocumentType.CREDIT ? "Credit" : "Invoice");
		reviewReferenceField.setText(accepted == null ? scanned.invoiceNo() : accepted.reference());
		reviewDateField.setValue(accepted == null ? scanned.invoiceDate() : accepted.documentDate());
		LocalDate dueDate = accepted == null ? scanned.dueDate() : accepted.dueDate();
		reviewDueDateField.setValue(type == ScannedInvoice.DocumentType.CREDIT
				? null : dueDateOrDefault(dueDate, scanned.invoiceDate()));
		double amount = accepted == null ? Math.abs(scanned.amount()) : accepted.amount();
		reviewAmountField.setText(String.format(java.util.Locale.ROOT, "%.2f", amount));
		updateReviewReconciliationValues();
		reviewNotesField.setText(accepted == null ? "" : accepted.notes());
		styleReviewConfidence(reviewSupplierChoice, row.getSupplierConfidence(), row.isAccepted());
		styleReviewConfidence(reviewTypeChoice, row.getTypeConfidence(), row.isAccepted());
		styleReviewConfidence(reviewReferenceField, row.getReferenceConfidence(), row.isAccepted());
		styleReviewConfidence(reviewDateField, row.getDateConfidence(), row.isAccepted());
		styleReviewConfidence(reviewDueDateField, row.getDueDateConfidence(), row.isAccepted());
		styleReviewConfidence(reviewAmountField, row.getAmountConfidence(), row.isAccepted());
		selectReviewSupplier(accepted == null ? scanned.supplierName() : accepted.supplierName());
		showReviewError(null);
		resultsView.setVisible(false);
		resultsView.setManaged(false);
		setReviewChromeVisible(false);
		reviewView.setVisible(true);
		reviewView.setManaged(true);
		Platform.runLater(() -> showEvidence(row));
	}

	private void deleteScannedRow(InvoiceReconciliation row) {
		if (row == null || row.getScanned() == null
				|| row.getStatus() == InvoiceReconciliation.Status.SAVED || commitBusy) return;
		String reference = row.getInvoiceNo().isBlank() ? "this scanned document" : row.getInvoiceNo();
		parent.getDialogPane().showWarning("Remove scanned row?",
				"Remove " + reference + " from the current scan?\n"
						+ "This does not delete anything already saved to the database.")
				.onClose(buttonType -> {
					if (!ButtonType.OK.equals(buttonType)) return;
					ScannedInvoice scanned = row.getScanned();
					allResults.remove(row);
					acceptedDocuments.remove(scanned);
					evidenceLocations.remove(scanned);
					evidenceFiles.remove(scanned);
					applyResultFilter();
					updateSummary(allResults.size());
					if (!resultsTable.getItems().isEmpty()) {
						resultsTable.getSelectionModel().selectFirst();
					} else {
						resultsTable.getSelectionModel().clearSelection();
					}
				});
	}

	@FXML
	private void backToResults() {
		InvoiceReconciliation rowToSelect = currentReview;
		previewRequest++;
		currentReview = null;
		reviewView.setVisible(false);
		reviewView.setManaged(false);
		setReviewChromeVisible(true);
		resultsView.setVisible(true);
		resultsView.setManaged(true);
		resultsTable.requestFocus();
		if (rowToSelect != null) {
			Platform.runLater(() -> selectResultRow(rowToSelect));
		}
	}

	private void selectResultRow(InvoiceReconciliation row) {
		int index = row == null ? -1 : resultsTable.getItems().indexOf(row);
		if (index < 0) {
			resultsTable.getSelectionModel().clearSelection();
			return;
		}
		resultsTable.getSelectionModel().clearAndSelect(index);
		resultsTable.scrollTo(index);
	}

	private void resetScan() {
		if (summaryLabel.textProperty().isBound()) summaryLabel.textProperty().unbind();
		previewRequest++;
		currentReview = null;
		currentStatementPeriodStart = null;
		currentStatementPeriodEnd = null;
		currentResultIsStatement = false;
		currentStatementAmountCents = null;
		currentStatementAmountConfidence = null;
		currentPreviewFile = null;
		currentEvidence = Map.of();
		currentPreviewImage = null;
		currentPageIndex = 0;
		currentPageCount = 0;
		currentPageWidth = 0;
		currentPageHeight = 0;
		acceptedDocuments.clear();
		allResults.clear();
		evidenceLocations.clear();
		evidenceFiles.clear();
		selectedFiles.clear();
		documentKindChoice.getSelectionModel().selectFirst();
		showMatchedCheck.setSelected(false);
		resultsTable.getSelectionModel().clearSelection();
		applyResultFilter();

		summaryLabel.setText("Choose PDFs to begin. No OCR result is saved automatically.");
		reviewTitleLabel.setText("Review invoice");
		reviewSupplierChoice.setValue(null);
		reviewTypeChoice.getSelectionModel().select("Invoice");
		reviewReferenceField.clear();
		reviewDateField.setValue(null);
		reviewDueDateField.setValue(null);
		reviewAmountField.clear();
		reviewNotesField.clear();
		showReviewError(null);

		pdfPreviewImage.setImage(null);
		pdfOverlayPane.getChildren().clear();
		previewDocumentLabel.setText("Select a result row");
		previewPageLabel.setText("No PDF loaded");
		previewHintLabel.setText("Review the highlighted source values before accepting.");
		previousPageButton.setDisable(true);
		nextPageButton.setDisable(true);

		setReviewChromeVisible(true);
		resultsView.setVisible(true);
		resultsView.setManaged(true);
		reviewView.setVisible(false);
		reviewView.setManaged(false);
		setBusy(false);
	}

	@FXML
	private void acceptReview() {
		if (currentReview == null) return;
		InvoiceSupplier supplier = reviewSupplierChoice.getValue();
		String reference = reviewReferenceField.getText() == null ? "" : reviewReferenceField.getText().trim();
		boolean credit = "Credit".equals(reviewTypeChoice.getValue());
		if (supplier == null) {
			showReviewError("Select an existing supplier.");
			reviewSupplierChoice.requestFocus();
			return;
		}
		if (reference.isBlank()) {
			showReviewError("Enter an invoice or credit reference.");
			reviewReferenceField.requestFocus();
			return;
		}
		if (reviewDateField.getValue() == null) {
			showReviewError("Enter the document date.");
			reviewDateField.requestFocus();
			return;
		}
		BigDecimal amount;
		try {
			String rawAmount = reviewAmountField.getText() == null ? "" : reviewAmountField.getText()
					.replace("$", "").replace(",", "").trim();
			amount = new BigDecimal(rawAmount).setScale(2, RoundingMode.HALF_UP);
			if (amount.signum() <= 0) throw new NumberFormatException();
		} catch (NumberFormatException exception) {
			showReviewError("Enter an amount greater than zero, for example 206.03.");
			reviewAmountField.requestFocus();
			return;
		}

		showReviewError(null);
		String notes = reviewNotesField.getText() == null ? "" : reviewNotesField.getText().trim();
		java.time.LocalDate documentDate = reviewDateField.getValue();
		java.time.LocalDate dueDate = credit
				? null : dueDateOrDefault(reviewDueDateField.getValue(), documentDate);
		if (!credit && reviewDueDateField.getValue() == null) reviewDueDateField.setValue(dueDate);
		ScannedInvoice scanned = currentReview.getScanned();
		acceptedDocuments.put(scanned, new AcceptedDocument(
				supplier.getContactID(),
				supplier.getSupplierName(),
				credit ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE,
				reference,
				documentDate,
				credit ? null : dueDate,
				amount.doubleValue(),
				notes));

		int index = allResults.indexOf(currentReview);
		if (index >= 0) {
			currentReview = currentReview.withStatus(InvoiceReconciliation.Status.ACCEPTED);
			allResults.set(index, currentReview);
		}
		applyResultFilter();
		updateSummary(allResults.size());
		backToResults();
	}

	@FXML
	private void saveAccepted() {
		if (commitBusy || allResults.isEmpty()) return;
		if (allResults.stream().anyMatch(row -> !row.isAccepted())) {
			parent.getDialogPane().showWarning("Documents still need review",
					"Review and accept every document that needs attention before saving.");
			return;
		}

		List<PendingSave> pending;
		try {
			pending = buildPendingSaves();
		} catch (IllegalStateException exception) {
			parent.getDialogPane().showWarning("Unable to save accepted documents", exception.getMessage());
			return;
		}
		if (pending.isEmpty()) return;

		setCommitBusy(true);
		Task<CommitResult> task = new Task<>() {
			@Override
			protected CommitResult call() {
				List<InvoiceReconciliation> saved = new ArrayList<>();
				for (PendingSave pendingSave : pending) {
					try {
						saveDocument(pendingSave.row(), pendingSave.document());
						saved.add(pendingSave.row());
					} catch (Exception exception) {
						String message = "Could not save " + pendingSave.row().getInvoiceNo() + ": "
								+ errorMessage(exception);
						return new CommitResult(saved, message);
					}
				}
				return new CommitResult(saved, null);
			}
		};
		task.setOnSucceeded(_ -> {
			CommitResult result = task.getValue();
			for (InvoiceReconciliation saved : result.savedRows()) {
				int index = allResults.indexOf(saved);
				if (index >= 0) allResults.set(index, saved.withStatus(InvoiceReconciliation.Status.SAVED));
			}
			applyResultFilter();
			updateSummary(allResults.size());
			setCommitBusy(false);
			if (!result.savedRows().isEmpty()) {
				parent.fillInvoiceTable();
				parent.fillCreditTable();
			}
			if (result.failureMessage() != null) {
				parent.getDialogPane().showError("Save incomplete", result.failureMessage());
			} else {
				resetScan();
			}
		});
		task.setOnFailed(_ -> {
			setCommitBusy(false);
			parent.getDialogPane().showError("Save failed", errorMessage(task.getException()),
					task.getException() instanceof Exception exception ? exception : new RuntimeException(task.getException()));
		});
		executor.submit(task);
	}

	private List<PendingSave> buildPendingSaves() {
		List<PendingSave> pending = new ArrayList<>();
		for (InvoiceReconciliation row : allResults) {
			if (row.getStatus() == InvoiceReconciliation.Status.SAVED) continue;
			ScannedInvoice scanned = row.getScanned();
			if (scanned == null) continue;
			AcceptedDocument accepted = acceptedDocuments.get(scanned);
			if (accepted == null) accepted = acceptedDocumentFromScanned(row);
			pending.add(new PendingSave(row, accepted));
		}
		return pending;
	}

	private AcceptedDocument acceptedDocumentFromScanned(InvoiceReconciliation row) {
		ScannedInvoice scanned = row.getScanned();
		InvoiceSupplier supplier = findAvailableSupplier(row.getSupplierName());
		if (supplier == null) {
			throw new IllegalStateException("Select an existing supplier for " + row.getInvoiceNo()
					+ " before saving.");
		}
		if (scanned.invoiceDate() == null) {
			throw new IllegalStateException("Enter a document date for " + row.getInvoiceNo()
					+ " before saving.");
		}
		boolean credit = scanned.documentType() == ScannedInvoice.DocumentType.CREDIT;
		LocalDate dueDate = credit ? null : dueDateOrDefault(scanned.dueDate(), scanned.invoiceDate());
		return new AcceptedDocument(
				supplier.getContactID(),
				supplier.getSupplierName(),
				scanned.documentType(),
				scanned.invoiceNo(),
				scanned.invoiceDate(),
				dueDate,
				Math.abs(scanned.amount()),
				"");
	}

	private void saveDocument(InvoiceReconciliation row, AcceptedDocument accepted) {
		if (accepted.documentType() == ScannedInvoice.DocumentType.CREDIT) {
			Credit credit = new Credit();
			credit.setSupplierID(accepted.supplierId());
			credit.setCreditNo(accepted.reference());
			credit.setReferenceInvoiceNo("");
			credit.setCreditDate(accepted.documentDate());
			credit.setCreditAmount(accepted.amount());
			credit.setNotes(accepted.notes());
			credit.setStoreID(main.getCurrentStore().getStoreID());
			creditService.saveOrUpdateCredit(credit);
			return;
		}

		Invoice invoice = new Invoice();
		invoice.setSupplierID(accepted.supplierId());
		invoice.setInvoiceNo(accepted.reference());
		invoice.setInvoiceDate(accepted.documentDate());
		invoice.setDueDate(accepted.dueDate());
		Invoice imported = row.getImported();
		String description = imported == null ? "" : imported.getDescription();
		invoice.setDescription(description == null || description.isBlank() ? "pharmacy stock" : description);
		invoice.setUnitAmount(accepted.amount());
		invoice.setNotes(accepted.notes());
		invoice.setStoreID(main.getCurrentStore().getStoreID());
		String originalReference = imported == null ? "" : imported.getInvoiceNo();
		int originalSupplierId = imported == null ? 0 : imported.getSupplierID();
		invoiceService.saveOrUpdateInvoice(invoice, originalReference, originalSupplierId);
	}

	private void updateReviewType(String value) {
		boolean invoice = !"Credit".equals(value);
		reviewDueDateLabel.setVisible(invoice);
		reviewDueDateLabel.setManaged(invoice);
		reviewDueDateField.setVisible(invoice);
		reviewDueDateField.setManaged(invoice);
		reviewDueDateField.setDisable(!invoice);
		reviewAcceptButton.setText("Accept");
		if (currentReview != null) reviewTitleLabel.setText(invoice ? "Review invoice" : "Review credit");
	}

	private void updateReviewReconciliationValues() {
		if (currentReview == null || currentReview.getImported() == null
				|| !currentReview.getImported().isImportExists()) {
			reviewExpectedAmountLabel.setText("N/A");
			reviewVarianceLabel.setText("N/A");
			reviewVarianceLabel.getStyleClass().remove("review-variance-attention");
			return;
		}

		double expected = currentReview.getImported().getImportedInvoiceAmount();
		reviewExpectedAmountLabel.setText(CURRENCY.format(expected));
		String rawAmount = reviewAmountField.getText() == null
				? "" : reviewAmountField.getText().replace("$", "").replace(",", "").trim();
		try {
			double entered = Double.parseDouble(rawAmount);
			double variance = entered - expected;
			reviewVarianceLabel.setText(CURRENCY.format(variance));
			reviewVarianceLabel.getStyleClass().remove("review-variance-attention");
			if (Math.abs(variance) > InvoiceReconciler.DEFAULT_TOLERANCE_CENTS / 100.0) {
				reviewVarianceLabel.getStyleClass().add("review-variance-attention");
			}
		} catch (NumberFormatException exception) {
			reviewVarianceLabel.setText("N/A");
			reviewVarianceLabel.getStyleClass().remove("review-variance-attention");
		}
	}

	private void selectReviewSupplier(String supplierName) {
		String normalized = InvoiceReconciler.supplier(supplierName);
		reviewSupplierChoice.getItems().stream()
				.filter(supplier -> InvoiceReconciler.supplier(supplier.getSupplierName()).equals(normalized))
				.findFirst()
				.ifPresentOrElse(reviewSupplierChoice::setValue, () -> reviewSupplierChoice.setValue(null));
	}

	private InvoiceSupplier findAvailableSupplier(String supplierName) {
		String normalized = InvoiceReconciler.supplier(supplierName);
		return availableSuppliers.stream()
				.filter(supplier -> InvoiceReconciler.supplier(supplier.getSupplierName()).equals(normalized))
				.findFirst()
				.orElse(null);
	}

	private void showReviewError(String message) {
		boolean visible = message != null && !message.isBlank();
		reviewErrorLabel.setText(visible ? message : "");
		reviewErrorLabel.setVisible(visible);
		reviewErrorLabel.setManaged(visible);
	}

	private static void styleReviewConfidence(Control control, Double score, boolean accepted) {
		control.getStyleClass().remove("low-confidence-input");
		if (accepted) {
			control.setTooltip(null);
			return;
		}
		Long percentage = score == null ? null : Math.round(score * 100);
		if (score != null && score < LOW_FIELD_CONFIDENCE) {
			control.setTooltip(new Tooltip("Low document AI confidence: " + percentage + "%"));
			control.getStyleClass().add("low-confidence-input");
		} else if (score == null) {
			control.setTooltip(new Tooltip("Document AI confidence unavailable"));
		} else {
			control.setTooltip(null);
		}
	}

	private void setReviewChromeVisible(boolean visible) {
		scanSourceBar.setVisible(visible);
		scanSourceBar.setManaged(visible);
		scanSummaryBar.setVisible(visible);
		scanSummaryBar.setManaged(visible);
		boolean showStatementTotals = visible && currentResultIsStatement
				&& currentStatementPeriodStart != null && currentStatementPeriodEnd != null;
		statementTotalsBar.setVisible(showStatementTotals);
		statementTotalsBar.setManaged(showStatementTotals);
		boolean showConfiguration = visible && !scanService.isConfigured();
		configurationLabel.setVisible(showConfiguration);
		configurationLabel.setManaged(showConfiguration);
	}

	private void applyResultFilter() {
		FilteredList<InvoiceReconciliation> filtered = new FilteredList<>(allResults,
				row -> showMatchedCheck.isSelected() || row.isNotable());
		resultsTable.setItems(filtered);
	}

	private void updateSummary(int extractedCount) {
		long accepted = allResults.stream().filter(InvoiceReconciliation::isAccepted).count();
		long notable = allResults.size() - accepted;
		String period = !currentResultIsStatement || currentStatementPeriodStart == null || currentStatementPeriodEnd == null ? ""
				: "Statement period " + periodLabel(currentStatementPeriodStart, currentStatementPeriodEnd) + " · ";
		summaryLabel.setText(period + extractedCount + " extracted · " + accepted + " accepted · "
				+ notable + " need attention. Variance is scanned minus Z-Office.");
		updateAcceptanceControls();
		updateStatementTotals();
	}

	private void updateAcceptanceControls() {
		long accepted = allResults.stream().filter(InvoiceReconciliation::isAccepted).count();
		long pendingSave = allResults.stream()
				.filter(row -> row.getStatus() != InvoiceReconciliation.Status.SAVED)
				.count();
		boolean allAccepted = !allResults.isEmpty() && accepted == allResults.size();
		if (allAccepted && !showMatchedCheck.isSelected()) showMatchedCheck.setSelected(true);
		boolean showSave = allAccepted && pendingSave > 0;
		acceptanceSummaryLabel.setText(allResults.isEmpty()
				? "" : pendingSave == 0
						? "Saved " + accepted + "/" + allResults.size()
						: accepted + "/" + allResults.size() + " accepted");
		saveAcceptedButton.setVisible(showSave || commitBusy);
		saveAcceptedButton.setManaged(showSave || commitBusy);
		saveAcceptedButton.setDisable(!showSave || commitBusy);
	}

	private void updateStatementTotals() {
		boolean statementResult = currentResultIsStatement
				&& currentStatementPeriodStart != null && currentStatementPeriodEnd != null;
		statementTotalsBar.setVisible(statementResult);
		statementTotalsBar.setManaged(statementResult);
		if (!statementResult) return;

		long extractedTotalCents = allResults.stream()
				.map(InvoiceReconciliation::getScanned)
				.filter(java.util.Objects::nonNull)
				.mapToLong(ScannedInvoice::amountCents)
				.sum();
		extractedStatementTotalLabel.setText(formatCurrency(extractedTotalCents));
		expectedStatementAmountLabel.setText(currentStatementAmountCents == null
				? "Not found" : formatCurrency(currentStatementAmountCents));

		Long varianceCents = currentStatementAmountCents == null
				? null : extractedTotalCents - currentStatementAmountCents;
		statementVarianceLabel.setText(varianceCents == null ? "Unavailable" : formatCurrency(varianceCents));
		statementVarianceLabel.getStyleClass().removeAll(
				"statement-total-good", "statement-total-review", "statement-total-attention", "statement-total-unavailable");
		if (varianceCents == null) statementVarianceLabel.getStyleClass().add("statement-total-unavailable");
		else if (varianceCents == 0) statementVarianceLabel.getStyleClass().add("statement-total-good");
		else if (Math.abs(varianceCents) <= InvoiceReconciler.DEFAULT_TOLERANCE_CENTS) {
			statementVarianceLabel.getStyleClass().add("statement-total-review");
		} else statementVarianceLabel.getStyleClass().add("statement-total-attention");

		Long confidencePercent = currentStatementAmountConfidence == null
				? null : Math.round(currentStatementAmountConfidence * 100);
		expectedStatementConfidenceLabel.setText(confidencePercent == null
				? "Printed total confidence unavailable"
				: confidencePercent + "% document AI confidence");
		expectedStatementAmountLabel.getStyleClass().remove("low-confidence-statement-total");
		if (currentStatementAmountConfidence != null && currentStatementAmountConfidence < LOW_FIELD_CONFIDENCE) {
			expectedStatementConfidenceLabel.setText("⚠ Low document AI confidence: " + confidencePercent + "%");
			expectedStatementAmountLabel.getStyleClass().add("low-confidence-statement-total");
		} else if (currentStatementAmountConfidence == null) {
			expectedStatementConfidenceLabel.setText("Printed total confidence unavailable");
		} else {
			// Keep high-confidence statement totals visually clean: the amount
			// itself is the useful value and needs no extra annotation.
			expectedStatementConfidenceLabel.setText("");
		}
	}

	private static String formatCurrency(long cents) {
		return CURRENCY.format(cents / 100.0);
	}

	private static String periodLabel(LocalDate start, LocalDate end) {
		if (start == null || end == null) return "unknown";
		return PERIOD_DATE.format(start) + " – " + PERIOD_DATE.format(end);
	}

	private static LocalDate dueDateOrDefault(LocalDate dueDate, LocalDate invoiceDate) {
		return dueDate == null && invoiceDate != null ? invoiceDate.plusDays(30) : dueDate;
	}

	private void refreshSelectedFiles() {
		selectedFilesList.getItems().setAll(selectedFiles.stream().map(File::getName).toList());
		scanButton.setDisable(commitBusy || selectedFiles.isEmpty());
		chooseFilesButton.setText(selectedFiles.isEmpty() ? "Choose PDF" : "Change selection");
	}

	private boolean isStatement() {
		return documentKindChoice.getSelectionModel().getSelectedIndex() == 0;
	}

	private void setBusy(boolean busy) {
		if (busy) scanProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		scanProgress.setVisible(busy);
		scanProgress.setManaged(busy);
		scanButton.setDisable(busy || commitBusy || selectedFiles.isEmpty());
		chooseFilesButton.setDisable(busy || commitBusy);
		documentKindChoice.setDisable(busy || commitBusy);
		showMatchedCheck.setDisable(busy || commitBusy);
		resultsTable.setDisable(busy || commitBusy);
		if (busy) {
			saveAcceptedButton.setVisible(false);
			saveAcceptedButton.setManaged(false);
		} else {
			updateAcceptanceControls();
		}
	}

	private void setCommitBusy(boolean busy) {
		commitBusy = busy;
		saveAcceptedProgress.setVisible(busy);
		saveAcceptedProgress.setManaged(busy);
		showMatchedCheck.setDisable(busy);
		resultsTable.setDisable(busy);
		scanButton.setDisable(busy || selectedFiles.isEmpty());
		chooseFilesButton.setDisable(busy);
		documentKindChoice.setDisable(busy);
		updateAcceptanceControls();
	}

	private static Throwable rootCause(Throwable value) {
		if (value == null) return new IllegalStateException("No error details were returned");
		Throwable current = value;
		while (current.getCause() != null && current.getCause() != current) current = current.getCause();
		return current;
	}

	private static String errorMessage(Throwable value) {
		Throwable root = rootCause(value);
		String rootMessage = root.getMessage() == null ? "" : root.getMessage().trim();
		String message = value == null || value.getMessage() == null ? "" : value.getMessage().trim();
		if (message.isBlank()) return rootMessage.isBlank() ? "Unknown scan error" : rootMessage;
		if (root == value || rootMessage.isBlank() || message.contains(rootMessage)) return message;
		return message + "\n\nCause: " + rootMessage;
	}

	private record AcceptedDocument(int supplierId,
			String supplierName,
			ScannedInvoice.DocumentType documentType,
			String reference,
			LocalDate documentDate,
			LocalDate dueDate,
			double amount,
			String notes) {}

	private record PendingSave(InvoiceReconciliation row, AcceptedDocument document) {}

	private record CommitResult(List<InvoiceReconciliation> savedRows, String failureMessage) {}

	private record ScanRun(List<ScannedInvoice> scanned,
			List<InvoiceReconciliation> reconciliations,
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations,
			Map<ScannedInvoice, File> filesByRow,
			LocalDate periodStart,
			LocalDate periodEnd,
			Long statementAmountCents,
			Double statementAmountConfidence) {}

	private record FileScan(int index, File file, List<ScannedInvoice> rows,
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations,
			LocalDate periodStart,
			LocalDate periodEnd,
			Long statementAmountCents,
			Double statementAmountConfidence) {}
}
