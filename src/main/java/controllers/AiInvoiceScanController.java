package controllers;

import application.Main;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import models.CellDataPoint;
import models.Credit;
import models.Invoice;
import models.InvoiceSupplier;
import models.PdfEvidenceField;
import models.InvoiceReconciliation;
import models.PdfEvidenceLocation;
import models.ScannedInvoice;
import models.DocumentAiModels;
import models.DocumentAiModels.BatchDetail;
import models.DocumentAiModels.BatchStatus;
import models.DocumentAiModels.BatchSummary;
import models.DocumentAiModels.EnteredDocument;
import services.InvoiceService;
import services.InvoiceSupplierService;
import services.CreditService;
import services.DocumentAiService;
import services.DocumentPdfCache;
import services.DocumentReviewCommitService;
import services.DocumentReviewCommitService.Completion;
import services.DocumentReviewCommitService.SaveOperation;
import services.GeminiInvoiceScanService;
import services.PdfPreviewService;
import utils.InvoiceReconciler;
import utils.SupplierMatcher;
import utils.WorkbookProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.Properties;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/** Controller for the review-first PDF scan and reconciliation dialog. */
public final class AiInvoiceScanController extends Controller {
	private static final double LOW_FIELD_CONFIDENCE = InvoiceReconciler.REVIEW_CONFIDENCE;
	private static final double EVIDENCE_FILL_OPACITY = 0.08;
	private static final double EVIDENCE_STROKE_OPACITY = 0.72;
	private static final DateTimeFormatter PERIOD_DATE = DateTimeFormatter.ofPattern("d MMM uuuu");
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
	@FXML private Button scanButton;
	@FXML private CheckBox showMatchedCheck;
	@FXML private ProgressIndicator scanProgress;
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
	@FXML private Button rotatePreviewButton;
	@FXML private Button previousPageButton;
	@FXML private Button nextPageButton;
	@FXML private Label reviewTitleLabel;
	@FXML private Label reviewErrorLabel;
	@FXML private ComboBox<InvoiceSupplier> reviewSupplierChoice;
	@FXML private Button customSupplierButton;
	@FXML private TextField customSupplierNameField;
	@FXML private Label customSupplierHelpLabel;
	@FXML private ChoiceBox<String> reviewTypeChoice;
	@FXML private TextField reviewReferenceField;
	@FXML private DatePicker reviewDateField;
	@FXML private Label reviewDueDateLabel;
	@FXML private DatePicker reviewDueDateField;
	@FXML private Label reviewDueDateHintLabel;
	@FXML private TextField reviewAmountField;
	@FXML private Label reviewExpectedAmountLabel;
	@FXML private Label reviewVarianceLabel;
	@FXML private TextArea reviewNotesField;
	@FXML private Button reviewBackButton;
	@FXML private Button reviewAcceptButton;
	@FXML private VBox queueView;
	@FXML private TableView<BatchSummary> queueTable;
	@FXML private Button refreshQueueButton;
	@FXML private Button reviewAllButton;
	@FXML private Label combinedReviewHint;
	@FXML private Button resultsBackButton;
	@FXML private HBox reviewBatchControls;

	private final ObservableList<InvoiceReconciliation> allResults = FXCollections.observableArrayList();
	private final ObservableList<BatchSummary> queuedBatches = FXCollections.observableArrayList();
	private final Map<ScannedInvoice, AcceptedDocument> acceptedDocuments = new IdentityHashMap<>();
	private final Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> evidenceLocations = new IdentityHashMap<>();
	private final Map<ScannedInvoice, Long> documentIdsByRow = new IdentityHashMap<>();
	private final Map<Long, Long> batchIdsByDocument = new HashMap<>();
	private final List<LoadedBatch> currentBatches = new ArrayList<>();
	private final Set<Long> completedBatchIds = new HashSet<>();
	private DocumentAiService documentAiService;
	private DocumentPdfCache pdfCache;
	private int reviewStoreId;
	private boolean disposed;
	private InvoiceEntryController parent;
	private InvoiceService invoiceService;
	private InvoiceSupplierService invoiceSupplierService;
	private CreditService creditService;
	private ExecutorService executor;
	private List<InvoiceSupplier> availableSuppliers = List.of();
	private CompletableFuture<List<InvoiceSupplier>> supplierLoad = CompletableFuture.completedFuture(List.of());
	private InvoiceReconciliation currentReview;
	private LocalDate currentStatementPeriodStart;
	private LocalDate currentStatementPeriodEnd;
	private boolean currentResultIsStatement;
	private Long currentStatementAmountCents;
	private Double currentStatementAmountConfidence;
	private Long currentPreviewDocumentId;
	private Map<PdfEvidenceField, PdfEvidenceLocation> currentEvidence = Map.of();
	private Image currentPreviewImage;
	private int currentPageIndex;
	private int currentPageCount;
	private double currentPageWidth;
	private double currentPageHeight;
	private double previewZoom = 0.5;
	private int previewRotationQuarterTurns;
	private long previewRequest;
	private boolean commitBusy;
	private boolean queueBusy;
	private boolean customSupplierSelected;
	private final Map<String, Integer> resolvedCustomSupplierIds = new HashMap<>();
	private Timeline queueRefreshTimeline;

	@FXML
	private void initialize() {
		reviewTypeChoice.getItems().setAll("Invoice", "Credit");
		reviewTypeChoice.getSelectionModel().selectedItemProperty().addListener((_, _, value) -> updateReviewType(value));
		reviewDueDateField.valueProperty().addListener((_, _, _) -> updateDueDateReviewPresentation());
		reviewAmountField.textProperty().addListener((_, _, _) -> updateReviewReconciliationValues());
		showMatchedCheck.selectedProperty().addListener((_, _, _) -> applyResultFilter());
		configureTable();
		configureQueueTable();
	}

	void configure(InvoiceEntryController parent, Main main, InvoiceService invoiceService,
			CreditService creditService, ExecutorService executor) {
		this.parent = parent;
		this.main = main;
		this.invoiceService = invoiceService;
		this.creditService = creditService;
		this.executor = executor;
		try {
			documentAiService = new DocumentAiService();
			pdfCache = new DocumentPdfCache(documentAiService::downloadPdf);
			invoiceSupplierService = new InvoiceSupplierService();
		} catch (IOException exception) {
			parent.getDialogPane().showError("Document review unavailable", exception);
			return;
		}
		supplierLoad = parent.fetchContactData().thenApply(suppliers ->
				suppliers == null ? List.of() : List.copyOf(suppliers));
		supplierLoad.whenComplete((suppliers, error) -> Platform.runLater(() -> {
			if (error != null) {
				reviewErrorLabel.setText("Unable to load suppliers: " + rootCause(error).getMessage());
				reviewErrorLabel.setVisible(true);
				reviewErrorLabel.setManaged(true);
				return;
			}
			availableSuppliers = suppliers;
			reviewSupplierChoice.getItems().setAll(availableSuppliers);
			if (currentReview != null) selectReviewSupplier(currentReview.getScanned().supplierName());
			updateAcceptanceControls();
			}));
		refreshQueue();
		queueRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), _ -> {
			if (!queueBusy && queueView.isVisible() && !commitBusy) refreshQueueSilently();
		}));
		queueRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
		queueRefreshTimeline.play();
	}

	@FXML
	private void scanDocuments() {
		if (documentAiService == null || queueBusy) return;
		setQueueBusy(true, "Starting a Google Drive scan…");
		Task<Void> task = new Task<>() {
			@Override protected Void call() { documentAiService.scanNow(storeId()); return null; }
		};
		task.setOnSucceeded(_ -> {
			setQueueBusy(false, "Drive scan started in Alpha API. Refresh to follow its progress.");
			refreshQueue();
		});
		task.setOnFailed(_ -> queueFailure("Unable to start Drive scan", task.getException()));
		executor.submit(task);
	}

	@FXML
	private void refreshQueue() {
		loadQueue(true);
	}

	private void refreshQueueSilently() {
		loadQueue(false);
	}

	private void loadQueue(boolean interactive) {
		if (documentAiService == null || main == null || main.getCurrentStore() == null) return;
		if (queueBusy) return;
		if (interactive) setQueueBusy(true, "Refreshing the document review queue…");
		else queueBusy = true;
		Task<List<BatchSummary>> task = new Task<>() {
			@Override protected List<BatchSummary> call() { return documentAiService.queue(storeId()); }
		};
		task.setOnSucceeded(_ -> {
			queuedBatches.setAll(task.getValue());
			queueTable.setItems(queuedBatches);
			queueBusy = false;
			if (interactive) setQueueBusy(false, queueSummary());
			else {
				updateQueueVisualState();
				summaryLabel.setText(queueSummary());
			}
		});
		task.setOnFailed(_ -> {
			if (interactive) queueFailure("Unable to load the document review queue", task.getException());
			else {
				queueBusy = false;
				updateQueueVisualState();
			}
		});
		executor.submit(task);
	}

	private void configureQueueTable() {
		TableColumn<BatchSummary, String> source = batchColumn("SOURCE", "sourceLabel", 360);
		TableColumn<BatchSummary, String> kind = batchColumn("TYPE", "kindLabel", 120);
		TableColumn<BatchSummary, String> status = batchColumn("STATUS", "statusLabel", 140);
		TableColumn<BatchSummary, String> count = batchColumn("CONTENTS", "countLabel", 140);
		TableColumn<BatchSummary, String> detail = batchColumn("DETAILS", "statusDetail", 300);
		detail.setCellFactory(_ -> new TableCell<>() {
			@Override protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? null : item);
				setTooltip(empty || item == null || item.isBlank() ? null : new Tooltip(item));
			}
		});
		TableColumn<BatchSummary, String> created = batchColumn("DISCOVERED", "createdLabel", 165);
		TableColumn<BatchSummary, Void> action = new TableColumn<>("ACTION");
		action.setPrefWidth(210);
		action.setSortable(false);
		action.setCellFactory(_ -> new TableCell<>() {
			private final Button primary = new Button();
			private final Button dismiss = new Button("Dismiss");
			private final HBox buttons = new HBox(6, primary, dismiss);
			{
				primary.getStyleClass().add("review-add-button");
				dismiss.getStyleClass().add("delete-scan-button");
				primary.setOnAction(_ -> handleBatchPrimary(getTableView().getItems().get(getIndex())));
				dismiss.setOnAction(_ -> dismissBatch(getTableView().getItems().get(getIndex())));
			}
			@Override protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				BatchSummary row = empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
						? null : getTableView().getItems().get(getIndex());
				if (row == null) { setGraphic(null); return; }
				primary.setText(row.status() == BatchStatus.FAILED ? "Retry" : "Review");
				primary.setDisable(row.status() != BatchStatus.FAILED && row.status() != BatchStatus.REVIEW_REQUIRED);
				setGraphic(buttons);
			}
		});
		queueTable.getColumns().setAll(source, kind, status, count, detail, created, action);
		queueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		queueTable.setPlaceholder(new Label("No Drive documents currently require review."));
		queueTable.setRowFactory(_ -> {
			TableRow<BatchSummary> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && !row.isEmpty()
						&& row.getItem().status() == BatchStatus.REVIEW_REQUIRED) openBatch(row.getItem());
			});
			return row;
		});
	}

	private static TableColumn<BatchSummary, String> batchColumn(String title, String property, double width) {
		TableColumn<BatchSummary, String> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>(property));
		column.setPrefWidth(width);
		return column;
	}

	private void handleBatchPrimary(BatchSummary batch) {
		if (batch.status() == BatchStatus.FAILED) retryBatch(batch);
		else openBatch(batch);
	}

	private void retryBatch(BatchSummary batch) {
		setQueueBusy(true, "Retrying " + batch.sourceLabel() + "…");
		Task<Void> task = new Task<>() {
			@Override protected Void call() { documentAiService.retry(batch.id(), storeId()); return null; }
		};
		task.setOnSucceeded(_ -> refreshQueue());
		task.setOnFailed(_ -> queueFailure("Unable to retry the scan", task.getException()));
		executor.submit(task);
	}

	private void dismissBatch(BatchSummary batch) {
		parent.getDialogPane().showWarning("Dismiss review batch?",
				"Dismiss “" + batch.sourceLabel() + "”? Its source PDFs will be moved to Google Drive trash.")
				.onClose(button -> {
					if (!ButtonType.OK.equals(button)) return;
					Task<Void> task = new Task<>() {
						@Override protected Void call() { documentAiService.dismiss(batch.id(), storeId()); return null; }
					};
					task.setOnSucceeded(_ -> refreshQueue());
					task.setOnFailed(_ -> queueFailure("Unable to dismiss the batch", task.getException()));
					executor.submit(task);
				});
	}

	private void openBatch(BatchSummary batch) {
		openBatches(List.of(batch));
	}

	@FXML
	private void reviewAll() {
		if (queueBusy || commitBusy) return;
		List<BatchSummary> ready = readyInvoiceBatches();
		if (!ready.isEmpty()) openBatches(ready);
	}

	private List<BatchSummary> readyInvoiceBatches() {
		return queuedBatches.stream()
				.filter(batch -> batch.kind() == DocumentAiModels.BatchKind.INVOICE_BATCH)
				.filter(batch -> batch.status() == BatchStatus.REVIEW_REQUIRED).toList();
	}

	private void openBatches(List<BatchSummary> batches) {
		if (pdfCache != null) pdfCache.stopPrefetch();
		setQueueBusy(true, "Loading invoice and reconciliation data…");
		int requestedStoreId = storeId();
		YearMonth requestedMonth = YearMonth.from(main.getCurrentDate());
		Task<List<LoadedBatch>> task = new Task<>() {
			@Override protected List<LoadedBatch> call() throws Exception {
				List<InvoiceSupplier> suppliers = supplierLoad.get();
				List<LoadedBatch> loaded = new ArrayList<>();
				ReviewLoadContext context = new ReviewLoadContext(requestedStoreId, requestedMonth);
				for (BatchSummary batch : batches) {
					BatchDetail detail = documentAiService.batch(batch.id(), requestedStoreId);
					if (detail.batch().status() != BatchStatus.REVIEW_REQUIRED) {
						throw new IllegalStateException("“" + batch.sourceLabel() + "” is no longer ready. Refresh the queue and try again.");
					}
					loaded.add(loadBatch(detail, suppliers, context));
				}
				return loaded;
			}
		};
		task.setOnSucceeded(_ -> {
			if (disposed) return;
			reviewStoreId = requestedStoreId;
			showLoadedBatches(task.getValue());
		});
		task.setOnFailed(_ -> {
			if (!disposed) queueFailure("Unable to open the review batches", task.getException());
		});
		executor.submit(task);
	}

	private LoadedBatch loadBatch(BatchDetail detail, List<InvoiceSupplier> suppliers) {
		return loadBatch(detail, suppliers, new ReviewLoadContext(storeId(), YearMonth.from(main.getCurrentDate())));
	}

	/** Short-lived data shared only by batches opened together; reopening fetches fresh amounts. */
	private static final class ReviewLoadContext {
		final int storeId;
		final YearMonth defaultMonth;
		final Map<YearMonth, List<Invoice>> months = new HashMap<>();
		final Map<String, Invoice> references = new HashMap<>();

		ReviewLoadContext(int storeId, YearMonth defaultMonth) {
			this.storeId = storeId;
			this.defaultMonth = defaultMonth;
		}
	}

	private LoadedBatch loadBatch(BatchDetail detail, List<InvoiceSupplier> suppliers, ReviewLoadContext context) {
		BatchSummary batch = detail.batch();
		List<ScannedInvoice> scanned = new ArrayList<>();
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new IdentityHashMap<>();
		Map<ScannedInvoice, Long> documentIds = new IdentityHashMap<>();
		LocalDate periodStart = null;
		LocalDate periodEnd = null;
		Long statementAmount = null;
		Double statementConfidence = null;
		for (DocumentAiModels.ExtractedDocument document : detail.documents()) {
			for (int index = 0; index < document.rows().size(); index++) {
				ScannedInvoice row = SupplierMatcher.correlate(document.rows().get(index), suppliers);
				scanned.add(row);
				documentIds.put(row, document.id());
				if (index < document.evidenceByRow().size()) locations.put(row, document.evidenceByRow().get(index));
			}
			if (document.periodStart() != null) periodStart = document.periodStart();
			if (document.periodEnd() != null) periodEnd = document.periodEnd();
			if (document.statementAmountCents() != null) {
				statementAmount = document.statementAmountCents();
				statementConfidence = document.statementAmountConfidence();
			}
		}
		GeminiInvoiceScanService.ScanKind kind = batch.kind() == DocumentAiModels.BatchKind.STATEMENT
				? GeminiInvoiceScanService.ScanKind.STATEMENT : GeminiInvoiceScanService.ScanKind.INDIVIDUAL_INVOICE;
		List<Invoice> imported = loadImportedRows(scanned, kind, periodStart, periodEnd, context);
		ScanRun run = new ScanRun(scanned, InvoiceReconciler.reconcile(scanned, imported), locations,
			documentIds, periodStart, periodEnd, statementAmount, statementConfidence, suppliers);
		return new LoadedBatch(batch, run,
				detail.documents().stream().map(DocumentAiModels.ExtractedDocument::id).toList());
	}

	private void showLoadedBatches(List<LoadedBatch> loaded) {
		previewRequest++;
		currentBatches.clear(); currentBatches.addAll(loaded);
		completedBatchIds.clear();
		evidenceLocations.clear(); documentIdsByRow.clear(); batchIdsByDocument.clear();
		for (LoadedBatch batch : loaded) {
			evidenceLocations.putAll(batch.run().locations());
			documentIdsByRow.putAll(batch.run().documentIds());
			batch.documentIds().forEach(id -> batchIdsByDocument.put(id, batch.batch().id()));
		}
		LoadedBatch first = loaded.getFirst();
		currentResultIsStatement = loaded.size() == 1 && first.batch().kind() == DocumentAiModels.BatchKind.STATEMENT;
		currentStatementPeriodStart = first.run().periodStart();
		currentStatementPeriodEnd = first.run().periodEnd();
		currentStatementAmountCents = first.run().statementAmountCents();
		currentStatementAmountConfidence = first.run().statementAmountConfidence();
		resolvedCustomSupplierIds.clear();
		deactivateCustomSupplier();
		availableSuppliers = loaded.getFirst().run().suppliers();
		reviewSupplierChoice.getItems().setAll(availableSuppliers);
		acceptedDocuments.clear();
		showMatchedCheck.setSelected(false);
		allResults.setAll(loaded.stream().flatMap(batch -> batch.run().reconciliations().stream()).toList());
		applyResultFilter();
		queueView.setVisible(false); queueView.setManaged(false);
		resultsView.setVisible(true); resultsView.setManaged(true);
		reviewBatchControls.setVisible(true); reviewBatchControls.setManaged(true);
		long skipped = queuedBatches.stream().filter(batch -> batch.kind() == DocumentAiModels.BatchKind.INVOICE_BATCH
				&& batch.status() != BatchStatus.REVIEW_REQUIRED).count();
		combinedReviewHint.setText(loaded.size() > 1 ? "Reviewing " + loaded.size()
				+ " invoice batches together. Saving will complete each original batch. Statements are reviewed separately."
				+ (skipped > 0 ? " " + skipped + " failed or unfinished invoice batch(es) remain in the queue." : "") : "");
		combinedReviewHint.setVisible(loaded.size() > 1); combinedReviewHint.setManaged(loaded.size() > 1);
		setQueueBusy(false, null);
		updateSummary(allResults.size());
		if (!resultsTable.getItems().isEmpty()) resultsTable.getSelectionModel().selectFirst();
		prefetchSelectedInvoice();
	}

	@FXML
	private void backToQueue() {
		if (pdfCache != null) pdfCache.stopPrefetch();
		previewRequest++;
		currentReview = null;
		documentIdsByRow.clear();
		batchIdsByDocument.clear(); currentBatches.clear(); completedBatchIds.clear();
		currentResultIsStatement = false;
		currentStatementPeriodStart = null;
		currentStatementPeriodEnd = null;
		currentStatementAmountCents = null;
		currentStatementAmountConfidence = null;
		resultsView.setVisible(false); resultsView.setManaged(false);
		reviewView.setVisible(false); reviewView.setManaged(false);
		queueView.setVisible(true); queueView.setManaged(true);
		reviewBatchControls.setVisible(false); reviewBatchControls.setManaged(false);
		hideStatementTotals();
		evidenceLocations.clear();
		currentPreviewDocumentId = null;
		setReviewChromeVisible(true);
		refreshQueue();
	}

	private void setQueueBusy(boolean busy, String message) {
		queueBusy = busy;
		updateQueueVisualState();
		if (message != null) summaryLabel.setText(message);
	}

	private void updateQueueVisualState() {
		scanProgress.setVisible(queueBusy);
		scanButton.setDisable(queueBusy || commitBusy);
		refreshQueueButton.setDisable(queueBusy);
		queueTable.setDisable(queueBusy);
		long ready = readyInvoiceBatches().size();
		reviewAllButton.setDisable(queueBusy || commitBusy || ready == 0);
		reviewAllButton.setText(ready == 0 ? "Review all invoices" : "Review all invoices (" + ready + ")");
		reviewAllButton.setVisible(queueView.isVisible()); reviewAllButton.setManaged(queueView.isVisible());
	}

	private String queueSummary() {
		if (queuedBatches.isEmpty()) {
			return "Nothing currently requires review · Updated " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		}
		long queued = queuedBatches.stream().filter(batch -> batch.status() == BatchStatus.QUEUED).count();
		long scanning = queuedBatches.stream().filter(batch -> batch.status() == BatchStatus.SCANNING).count();
		long ready = queuedBatches.stream().filter(batch -> batch.status() == BatchStatus.REVIEW_REQUIRED).count();
		long failed = queuedBatches.stream().filter(batch -> batch.status() == BatchStatus.FAILED).count();
		return queuedBatches.size() + " batch(es) · " + scanning + " scanning · " + queued + " queued · "
				+ ready + " ready · " + failed + " failed · Updated "
				+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
	}

	private void queueFailure(String title, Throwable error) {
		setQueueBusy(false, title + ".");
		parent.getDialogPane().showError(title, errorMessage(error),
				error instanceof Exception exception ? exception : new RuntimeException(error));
	}

	private int storeId() { return main.getCurrentStore().getStoreID(); }

	void dispose() {
		disposed = true;
		previewRequest++;
		if (queueRefreshTimeline != null) queueRefreshTimeline.stop();
		if (pdfCache != null) pdfCache.close();
	}

	private List<Invoice> loadImportedRows(List<ScannedInvoice> scanned, GeminiInvoiceScanService.ScanKind kind,
			LocalDate periodStart, LocalDate periodEnd, ReviewLoadContext context) {
		List<YearMonth> months = new ArrayList<>();
		if (kind == GeminiInvoiceScanService.ScanKind.STATEMENT && periodStart != null && periodEnd != null) {
			YearMonth month = YearMonth.from(periodStart);
			YearMonth finalMonth = YearMonth.from(periodEnd);
			while (!month.isAfter(finalMonth)) {
				months.add(month);
				month = month.plusMonths(1);
			}
		} else {
			months.addAll(scanned.stream().map(ScannedInvoice::invoiceDate)
					.filter(java.util.Objects::nonNull).map(YearMonth::from).distinct().sorted().toList());
			if (months.isEmpty()) months.add(context.defaultMonth);
		}
		List<Invoice> imported = new ArrayList<>();
		for (YearMonth month : months) {
			imported.addAll(context.months.computeIfAbsent(month, requested -> {
				List<Invoice> tableRows = invoiceService.getReconciliationTableData(context.storeId, requested);
				List<Invoice> snapshot = loadImportSnapshot(context.storeId, requested, tableRows);
				return snapshot.isEmpty() ? tableRows : snapshot;
			}));
		}
		Map<String, List<Invoice>> byReference = InvoiceReconciler.indexImportedRows(imported);
		Map<String, Invoice> fetched = context.references;
		for (ScannedInvoice row : scanned) {
			if (!InvoiceReconciler.matchingImportedRows(row, byReference).isEmpty()) continue;
			for (String reference : InvoiceReconciler.lookupReferences(row)) {
				if (!fetched.containsKey(reference)) fetched.put(reference, invoiceService.getInvoice(reference, context.storeId));
				Invoice resolved = fetched.get(reference);
				if (resolved != null && resolved.isImportExists() && !imported.contains(resolved)) {
					imported.add(resolved);
					byReference.computeIfAbsent(InvoiceReconciler.reference(resolved.getInvoiceNo()), _ -> new ArrayList<>()).add(resolved);
				}
				if (!InvoiceReconciler.matchingImportedRows(row, byReference).isEmpty()) break;
			}
		}
		return imported;
	}

	private List<Invoice> loadImportSnapshot(int requestedStoreId, YearMonth month, List<Invoice> tableRows) {
		Properties properties = new Properties();
		File configuration = new File("appConfig.properties");
		if (!configuration.isFile()) return new ArrayList<>();
		try (FileInputStream input = new FileInputStream(configuration)) {
			properties.load(input);
			String key = InvoiceEntryController.importSnapshotKey(requestedStoreId, month);
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
		TableColumn<InvoiceReconciliation, String> source = new TableColumn<>("SOURCE PDF");
		source.setPrefWidth(180);
		source.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getSourceFile()));
		TableColumn<InvoiceReconciliation, String> status = column("STATUS", "statusLabel", 135);
		TableColumn<InvoiceReconciliation, String> supplier = confidenceColumn(
				"SUPPLIER", "supplierName", 180, InvoiceReconciliation::getSupplierConfidence,
				"supplier selection");
		TableColumn<InvoiceReconciliation, String> reference = confidenceColumn(
				"REFERENCE", "invoiceNo", 135, InvoiceReconciliation::getReferenceConfidence, "document AI");
		TableColumn<InvoiceReconciliation, String> type = confidenceColumn(
				"TYPE", "documentType", 80, InvoiceReconciliation::getTypeConfidence, "document AI");
		TableColumn<InvoiceReconciliation, String> scannedDate = confidenceColumn(
				"OCR DATE", "scannedDateString", 90, InvoiceReconciliation::getDateConfidence, "document AI");
		TableColumn<InvoiceReconciliation, String> scanned = confidenceColumn(
				"SCANNED", "scannedAmountString", 100, InvoiceReconciliation::getAmountConfidence, "document AI");
		TableColumn<InvoiceReconciliation, String> imported = column("EXPECTED", "importedAmountString", 100);
		TableColumn<InvoiceReconciliation, String> variance = column("VARIANCE", "varianceString", 100);
		TableColumn<InvoiceReconciliation, InvoiceReconciliation> action = new TableColumn<>("ACTION");
		action.setPrefWidth(165);
		action.setSortable(false);
		// Give virtualized cells a row value so filtering and reuse invalidate their contents.
		action.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
		action.setCellFactory(_ -> new TableCell<>() {
			private final Button reviewButton = new Button("Review");
			private final Button trashButton = new Button("Trash");
			private final HBox buttons = new HBox(5, reviewButton, trashButton);
			{
				reviewButton.getStyleClass().add("review-add-button");
				reviewButton.setOnAction(_ -> {
					if (!isEmpty() && !commitBusy) review(getItem());
				});
				trashButton.getStyleClass().add("delete-scan-button");
				trashButton.setOnAction(_ -> {
					if (!isEmpty() && !commitBusy) trashScannedRow(getItem());
				});
			}
			@Override
			protected void updateItem(InvoiceReconciliation item, boolean empty) {
				super.updateItem(item, empty);
				InvoiceReconciliation row = empty ? null : item;
				boolean saved = row != null && row.getStatus() == InvoiceReconciliation.Status.SAVED;
				reviewButton.setText(saved ? "Saved" : "Review");
				reviewButton.setDisable(saved || commitBusy);
				trashButton.setDisable(saved || commitBusy);
				setGraphic(row == null || row.getScanned() == null ? null : buttons);
			}
		});
		resultsTable.getColumns().setAll(status, supplier, reference, type, scannedDate, scanned, imported, variance, source, action);
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
		resultsTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> prefetchSelectedInvoice());
	}

	private void prefetchSelectedInvoice() {
		if (pdfCache == null || disposed || commitBusy || queueView.isVisible() || !resultsView.isVisible()) return;
		InvoiceReconciliation selected = resultsTable.getSelectionModel().getSelectedItem();
		if (selected == null || selected.getStatus() == InvoiceReconciliation.Status.SAVED) return;
		Long documentId = documentIdsByRow.get(selected.getScanned());
		if (documentId != null) pdfCache.prefetch(documentId, reviewStoreId);
	}

	private void prefetchNextInvoice(long currentDocumentId) {
		if (pdfCache == null || disposed || commitBusy) return;
		boolean foundCurrent = false;
		for (InvoiceReconciliation row : resultsTable.getItems()) {
			Long documentId = documentIdsByRow.get(row.getScanned());
			if (documentId == null) continue;
			if (documentId == currentDocumentId) { foundCurrent = true; continue; }
			if (foundCurrent && row.getStatus() != InvoiceReconciliation.Status.SAVED) {
				pdfCache.prefetch(documentId, reviewStoreId);
				return;
			}
		}
	}

	private void showEvidence(InvoiceReconciliation reconciliation) {
		if (reconciliation == null || reconciliation.getScanned() == null) return;
		ScannedInvoice scanned = reconciliation.getScanned();
		previewRequest++;
		currentPreviewDocumentId = documentIdsByRow.get(scanned);
		currentPreviewImage = null;
		previewRotationQuarterTurns = 0;
		currentEvidence = evidenceLocations.getOrDefault(scanned, Map.of());
		pdfPreviewImage.setImage(null);
		pdfOverlayPane.getChildren().clear();
		previousPageButton.setDisable(true);
		nextPageButton.setDisable(true);
		rotatePreviewButton.setDisable(true);
		previewDocumentLabel.setText(scanned.sourceFile() + " · " + scanned.invoiceNo());
		if (currentPreviewDocumentId == null) {
			previewPageLabel.setText("No PDF loaded");
			previewHintLabel.setText("The original PDF is no longer available for preview.");
			return;
		}
		previewPageLabel.setText("Loading PDF…");
		if (pdfCache != null) pdfCache.stopPrefetch();
		PdfEvidenceLocation primaryEvidence = primaryEvidence(currentEvidence);
		int page = primaryEvidence == null ? 0 : primaryEvidence.pageIndex();
		renderPreviewPage(page, true);
	}

	private void renderPreviewPage(int pageIndex, boolean fitWidth) {
		if (currentPreviewDocumentId == null || pdfCache == null || disposed) return;
		long request = ++previewRequest;
		long documentId = currentPreviewDocumentId;
		int requestedStoreId = reviewStoreId;
		int rotationQuarterTurns = previewRotationQuarterTurns;
		rotatePreviewButton.setDisable(true);
		previewHintLabel.setText("Loading PDF preview…");
		Task<PdfPreviewService.RenderedPage> task = new Task<>() {
			@Override
			protected PdfPreviewService.RenderedPage call() throws Exception {
				try (DocumentPdfCache.Source source = pdfCache.acquire(documentId, requestedStoreId)) {
					return PdfPreviewService.render(source.file(), pageIndex, rotationQuarterTurns);
				}
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
			rotatePreviewButton.setDisable(false);
			previewHintLabel.setText(currentEvidence.isEmpty()
					? "No searchable OCR values were found; inspect the rendered PDF manually."
					: "Colour-coded boxes mark the supplier, invoice reference, date, and amount when found.");
			prefetchNextInvoice(documentId);
		});
		task.setOnFailed(_ -> {
			if (request != previewRequest) return;
			rotatePreviewButton.setDisable(true);
			previewPageLabel.setText("PDF unavailable");
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
		boolean quarterTurned = previewRotationQuarterTurns % 2 != 0;
		double baseDisplayWidth = quarterTurned ? displayHeight : displayWidth;
		double baseDisplayHeight = quarterTurned ? displayWidth : displayHeight;
		double focusX = 0;
		double focusY = 0;
		boolean hasFocus = false;
		for (Map.Entry<PdfEvidenceField, PdfEvidenceLocation> entry : currentEvidence.entrySet()) {
			PdfEvidenceLocation evidence = entry.getValue();
			if (evidence.pageIndex() != currentPageIndex) continue;
			double x = evidence.x() / currentPageWidth * baseDisplayWidth;
			double y = evidence.y() / currentPageHeight * baseDisplayHeight;
			double width = evidence.width() / currentPageWidth * baseDisplayWidth;
			double height = evidence.height() / currentPageHeight * baseDisplayHeight;
			PreviewRectangle rotated = rotatePreviewRectangle(x, y, width, height,
					baseDisplayWidth, baseDisplayHeight, previewRotationQuarterTurns);
			x = rotated.x();
			y = rotated.y();
			width = rotated.width();
			height = rotated.height();
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

	@FXML
	private void rotatePreview() {
		if (currentPreviewDocumentId == null || currentPreviewImage == null) return;
		previewRotationQuarterTurns = (previewRotationQuarterTurns + 1) % 4;
		renderPreviewPage(currentPageIndex, false);
	}

	private static PreviewRectangle rotatePreviewRectangle(double x, double y, double width, double height,
			double canvasWidth, double canvasHeight, int quarterTurns) {
		return switch (Math.floorMod(quarterTurns, 4)) {
			case 1 -> new PreviewRectangle(canvasHeight - y - height, x, height, width);
			case 2 -> new PreviewRectangle(canvasWidth - x - width, canvasHeight - y - height, width, height);
			case 3 -> new PreviewRectangle(y, canvasWidth - x - width, height, width);
			default -> new PreviewRectangle(x, y, width, height);
		};
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
			double width, Function<InvoiceReconciliation, Double> confidence, String confidenceSource) {
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
					setTooltip(new Tooltip("Low " + confidenceSource + " confidence: " + percentage + "%"));
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
		deactivateCustomSupplier();
		ScannedInvoice.DocumentType type = accepted == null ? scanned.documentType() : accepted.documentType();
		reviewTitleLabel.setText("Review " + (type == ScannedInvoice.DocumentType.CREDIT
				? "credit" : "invoice"));
		reviewTypeChoice.getSelectionModel().select(
				type == ScannedInvoice.DocumentType.CREDIT ? "Credit" : "Invoice");
		reviewReferenceField.setText(accepted == null ? InvoiceReconciler.entryReference(row) : accepted.reference());
		reviewDateField.setValue(accepted == null ? scanned.invoiceDate() : accepted.documentDate());
		LocalDate dueDate = accepted == null ? scanned.dueDate() : accepted.dueDate();
		reviewDueDateField.setValue(type == ScannedInvoice.DocumentType.CREDIT
				? null : dueDateOrDefault(dueDate, scanned.invoiceDate()));
		double amount = accepted == null ? Math.abs(scanned.amount()) : accepted.amount();
		reviewAmountField.setText(String.format(java.util.Locale.ROOT, "%.2f", amount));
		updateReviewReconciliationValues();
		reviewNotesField.setText(accepted == null ? "" : accepted.notes());
		styleReviewConfidence(reviewSupplierChoice, row.getSupplierConfidence(), row.isAccepted(), "supplier selection");
		styleReviewConfidence(reviewTypeChoice, row.getTypeConfidence(), row.isAccepted(), "document AI");
		styleReviewConfidence(reviewReferenceField, row.getReferenceConfidence(), row.isAccepted(), "document AI");
		styleReviewConfidence(reviewDateField, row.getDateConfidence(), row.isAccepted(), "document AI");
		updateDueDateReviewPresentation();
		styleReviewConfidence(reviewAmountField, row.getAmountConfidence(), row.isAccepted(), "document AI");
		selectReviewSupplier(accepted == null ? scanned.supplierName() : accepted.supplierName());
		if (accepted != null && accepted.supplierId() == 0) {
			activateCustomSupplier(accepted.supplierName());
		} else if (reviewSupplierChoice.getValue() == null) {
			activateCustomSupplier(scanned.supplierName());
		}
		showReviewError(null);
		resultsView.setVisible(false);
		resultsView.setManaged(false);
		setReviewChromeVisible(false);
		reviewView.setVisible(true);
		reviewView.setManaged(true);
		Platform.runLater(() -> {
			if (!disposed && currentReview == row) showEvidence(row);
		});
	}

	private void trashScannedRow(InvoiceReconciliation row) {
		if (row == null || row.getScanned() == null
				|| row.getStatus() == InvoiceReconciliation.Status.SAVED || commitBusy) return;
		String reference = row.getInvoiceNo().isBlank() ? "this scanned document" : row.getInvoiceNo();
		Long documentId = documentIdsByRow.get(row.getScanned());
		if (documentId == null || documentId <= 0) {
			parent.getDialogPane().showError("Unable to trash source PDF",
					"The source document ID is unavailable; refresh the review batch and try again.");
			return;
		}
		parent.getDialogPane().showWarning("Move source PDF to trash?",
				"Move " + reference + " and its source PDF to Google Drive trash?\n"
						+ "All extracted rows from that PDF will be removed from this review batch.")
				.onClose(buttonType -> {
					if (!ButtonType.OK.equals(buttonType)) return;
					setCommitBusy(true);
					Task<Void> task = new Task<>() {
						@Override protected Void call() {
							documentAiService.dismissDocument(documentId, storeId());
							return null;
						}
					};
					task.setOnSucceeded(_ -> {
							List<InvoiceReconciliation> removed = allResults.stream()
									.filter(candidate -> documentId.equals(documentIdsByRow.get(candidate.getScanned())))
									.toList();
							for (InvoiceReconciliation removedRow : removed) {
								ScannedInvoice scanned = removedRow.getScanned();
								allResults.remove(removedRow);
								acceptedDocuments.remove(scanned);
								evidenceLocations.remove(scanned);
								documentIdsByRow.remove(scanned);
							}
							batchIdsByDocument.remove(documentId);
							setCommitBusy(false);
							if (allResults.isEmpty()) backToQueue();
							else {
								applyResultFilter();
								updateSummary(allResults.size());
								resultsTable.getSelectionModel().selectFirst();
							}
						});
						task.setOnFailed(_ -> {
							setCommitBusy(false);
							parent.getDialogPane().showError("Unable to trash source PDF",
									errorMessage(task.getException()), task.getException());
						});
						executor.submit(task);
				});
	}

	@FXML
	private void backToResults() {
		InvoiceReconciliation rowToSelect = currentReview;
		previewRequest++;
		currentReview = null;
		reviewView.setVisible(false);
		reviewView.setManaged(false);
		resultsView.setVisible(true);
		resultsView.setManaged(true);
		setReviewChromeVisible(true);
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

	@FXML
	private void toggleCustomSupplier() {
		if (customSupplierSelected) {
			deactivateCustomSupplier();
			selectReviewSupplier(currentReview == null ? "" : currentReview.getScanned().supplierName());
			return;
		}
		String suggested = currentReview == null ? "" : currentReview.getScanned().supplierName();
		activateCustomSupplier(suggested);
	}

	private void activateCustomSupplier(String suggestedName) {
		customSupplierSelected = true;
		customSupplierNameField.setText(suggestedName == null ? "" : suggestedName);
		customSupplierNameField.setVisible(true);
		customSupplierNameField.setManaged(true);
		customSupplierHelpLabel.setVisible(true);
		customSupplierHelpLabel.setManaged(true);
		reviewSupplierChoice.setValue(null);
		reviewSupplierChoice.setDisable(true);
		customSupplierButton.setAccessibleText("Use an existing supplier");
		customSupplierButton.setTooltip(new Tooltip("Use an existing supplier"));
		Platform.runLater(() -> {
			customSupplierNameField.requestFocus();
			customSupplierNameField.selectAll();
		});
	}

	private void deactivateCustomSupplier() {
		customSupplierSelected = false;
		customSupplierNameField.clear();
		customSupplierNameField.setVisible(false);
		customSupplierNameField.setManaged(false);
		customSupplierHelpLabel.setVisible(false);
		customSupplierHelpLabel.setManaged(false);
		reviewSupplierChoice.setDisable(false);
		customSupplierButton.setAccessibleText("Use a custom supplier name");
		customSupplierButton.setTooltip(new Tooltip("Use a custom supplier name"));
	}

	@FXML
	private void acceptReview() {
		if (currentReview == null) return;
		InvoiceSupplier supplier = reviewSupplierChoice.getValue();
		String supplierName;
		int supplierId;
		if (customSupplierSelected) {
			supplierName = customSupplierNameField.getText() == null ? "" : customSupplierNameField.getText().trim();
			if (supplierName.isBlank()) {
				showReviewError("Enter a custom supplier name.");
				customSupplierNameField.requestFocus();
				return;
			}
			supplierId = 0;
		} else {
			supplierName = supplier == null ? "" : supplier.getSupplierName();
			supplierId = supplier == null ? 0 : supplier.getContactID();
		}
		String reference = reviewReferenceField.getText() == null ? "" : reviewReferenceField.getText().trim();
		boolean credit = "Credit".equals(reviewTypeChoice.getValue());
		if (!customSupplierSelected && supplier == null) {
			showReviewError("Select an existing supplier or choose Use custom.");
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
			if (amount.signum() < 0) throw new NumberFormatException();
		} catch (NumberFormatException exception) {
			showReviewError("Enter an amount of zero or more, for example 0.00 or 206.03.");
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
				supplierId,
				supplierName,
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
		// Include rows saved by an earlier attempt: their PDFs may still need filing.
		List<EnteredDocument> enteredDocuments = buildEnteredDocuments();
		List<Completion> completions = DocumentReviewCommitService.completions(
				currentBatches.stream().map(LoadedBatch::batch).toList(), batchIdsByDocument,
				enteredDocuments, completedBatchIds);
		if (pending.isEmpty() && completions.isEmpty()) return;
		List<SaveOperation> saves = pending.stream().map(save -> new SaveOperation(save.row(),
				() -> saveDocument(save.row(), save.document()))).toList();
		int requestedStoreId = storeId();

		setCommitBusy(true);
		Task<DocumentReviewCommitService.Result> task = new Task<>() {
			@Override
			protected DocumentReviewCommitService.Result call() {
				return DocumentReviewCommitService.commit(saves, completions,
						completion -> documentAiService.complete(completion.batch().id(), requestedStoreId, completion.documents()));
			}
		};
		task.setOnSucceeded(_ -> {
			DocumentReviewCommitService.Result result = task.getValue();
			completedBatchIds.addAll(result.completedBatchIds());
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
				backToQueue();
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

	private List<EnteredDocument> buildEnteredDocuments() {
		Map<Long, LocalDate> datesByDocument = new LinkedHashMap<>();
		for (InvoiceReconciliation row : allResults) {
			ScannedInvoice scanned = row.getScanned();
			Long documentId = documentIdsByRow.get(scanned);
			AcceptedDocument accepted = acceptedDocuments.get(scanned);
			LocalDate date = accepted == null ? scanned.invoiceDate() : accepted.documentDate();
			if (documentId == null || documentId <= 0 || date == null) continue;
			datesByDocument.merge(documentId, date,
					(left, right) -> left.isBefore(right) ? left : right);
		}
		return datesByDocument.entrySet().stream()
				.map(entry -> new EnteredDocument(entry.getKey(), entry.getValue())).toList();
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
				InvoiceReconciler.entryReference(row),
				scanned.invoiceDate(),
				dueDate,
				Math.abs(scanned.amount()),
				"");
	}

	private void saveDocument(InvoiceReconciliation row, AcceptedDocument accepted) {
		int supplierId = resolveSupplierId(accepted);
		if (accepted.documentType() == ScannedInvoice.DocumentType.CREDIT) {
			Credit credit = new Credit();
			credit.setSupplierID(supplierId);
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
		invoice.setSupplierID(supplierId);
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

	private int resolveSupplierId(AcceptedDocument accepted) {
		if (accepted.supplierId() > 0) return accepted.supplierId();
		if (invoiceSupplierService == null) {
			throw new IllegalStateException("The supplier service is unavailable; the custom supplier cannot be saved.");
		}
		String name = accepted.supplierName().trim();
		String key = InvoiceReconciler.supplier(name).toLowerCase(Locale.ROOT);
		Integer cached = resolvedCustomSupplierIds.get(key);
		if (cached != null) return cached;
		InvoiceSupplier loaded = findAvailableSupplier(name);
		if (loaded != null && loaded.getContactID() > 0) {
			resolvedCustomSupplierIds.put(key, loaded.getContactID());
			return loaded.getContactID();
		}
		// Refresh the store list before creating anything. The by-name endpoint
		// uses an exact database comparison, while the review UI deliberately uses
		// the same normalized comparison as OCR matching.
		InvoiceSupplier supplier = findMatchingSupplier(
				invoiceSupplierService.getAllInvoiceSuppliers(storeId()), name);
		if (supplier == null) {
			InvoiceSupplier created = new InvoiceSupplier();
			created.setSupplierName(name);
			created.setStoreID(storeId());
			invoiceSupplierService.addInvoiceSupplier(created);
			// The create endpoint does not return the generated supplier ID. Read
			// the store list back and resolve by normalized name so punctuation,
			// case, and whitespace cannot make a successful insert look like a
			// failed save.
			supplier = findMatchingSupplier(
					invoiceSupplierService.getAllInvoiceSuppliers(storeId()), name);
		}
		if (supplier == null || supplier.getContactID() <= 0) {
			throw new IllegalStateException("The custom supplier could not be created: " + name);
		}
		resolvedCustomSupplierIds.put(key, supplier.getContactID());
		return supplier.getContactID();
	}

	private void updateReviewType(String value) {
		boolean invoice = !"Credit".equals(value);
		reviewDueDateLabel.setVisible(invoice);
		reviewDueDateLabel.setManaged(invoice);
		reviewDueDateField.setVisible(invoice);
		reviewDueDateField.setManaged(invoice);
		reviewDueDateField.setDisable(!invoice);
		updateDueDateReviewPresentation();
		reviewAcceptButton.setText("Accept");
		if (currentReview != null) reviewTitleLabel.setText(invoice ? "Review invoice" : "Review credit");
	}

	private void updateReviewReconciliationValues() {
		reviewExpectedAmountLabel.getStyleClass().remove("review-reconciliation-attention");
		reviewExpectedAmountLabel.setTooltip(null);
		if (currentReview == null || currentReview.getImported() == null
				|| !currentReview.getImported().isImportExists()) {
			reviewExpectedAmountLabel.setText("N/A");
			if (currentReview != null && !currentReview.isAccepted()) {
				reviewExpectedAmountLabel.getStyleClass().add("review-reconciliation-attention");
				reviewExpectedAmountLabel.setTooltip(new Tooltip(
						"No imported Z-Office unit amount is available; verify the scanned amount manually."));
			}
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

	private void updateDueDateReviewPresentation() {
		reviewDueDateField.getStyleClass().remove("estimated-date-input");
		boolean invoice = !"Credit".equals(reviewTypeChoice.getValue());
		if (!invoice || currentReview == null || currentReview.getScanned() == null) {
			reviewDueDateHintLabel.setVisible(false);
			reviewDueDateHintLabel.setManaged(false);
			reviewDueDateField.getStyleClass().remove("low-confidence-input");
			reviewDueDateField.setTooltip(null);
			return;
		}

		ScannedInvoice scanned = currentReview.getScanned();
		LocalDate estimatedDate = currentReview.isDueDateEstimated() && scanned.invoiceDate() != null
				? scanned.invoiceDate().plusDays(30) : null;
		boolean estimated = !currentReview.isAccepted()
				&& estimatedDate != null && estimatedDate.equals(reviewDueDateField.getValue());
		if (estimated) {
			reviewDueDateField.getStyleClass().remove("low-confidence-input");
			reviewDueDateField.getStyleClass().add("estimated-date-input");
			reviewDueDateField.setTooltip(new Tooltip("Due date was estimated 30 days from the invoice date."));
			reviewDueDateHintLabel.setText("Due date was estimated 30 days from the invoice date.");
			reviewDueDateHintLabel.setVisible(true);
			reviewDueDateHintLabel.setManaged(true);
		} else {
			reviewDueDateHintLabel.setVisible(false);
			reviewDueDateHintLabel.setManaged(false);
			styleReviewConfidence(reviewDueDateField, currentReview.getDueDateConfidence(),
					currentReview.isAccepted(), "document AI");
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
		return findMatchingSupplier(availableSuppliers, supplierName);
	}

	private static InvoiceSupplier findMatchingSupplier(List<InvoiceSupplier> suppliers, String supplierName) {
		String normalized = InvoiceReconciler.supplier(supplierName);
		if (suppliers == null || normalized.isBlank()) return null;
		return suppliers.stream()
				.filter(supplier -> supplier != null && supplier.getContactID() > 0)
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

	private static void styleReviewConfidence(Control control, Double score, boolean accepted, String confidenceSource) {
		control.getStyleClass().remove("low-confidence-input");
		if (accepted) {
			control.setTooltip(null);
			return;
		}
		Long percentage = score == null ? null : Math.round(score * 100);
		if (score != null && score < LOW_FIELD_CONFIDENCE) {
			control.setTooltip(new Tooltip("Low " + confidenceSource + " confidence: " + percentage + "%"));
			control.getStyleClass().add("low-confidence-input");
		} else if (score == null) {
			control.setTooltip(new Tooltip(confidenceSource + " confidence unavailable"));
		} else {
			control.setTooltip(null);
		}
	}

	private void setReviewChromeVisible(boolean visible) {
		scanSourceBar.setVisible(visible);
		scanSourceBar.setManaged(visible);
		scanSummaryBar.setVisible(visible);
		scanSummaryBar.setManaged(visible);
		boolean showStatementTotals = visible && resultsView.isVisible() && currentResultIsStatement
				&& currentStatementPeriodStart != null && currentStatementPeriodEnd != null;
		statementTotalsBar.setVisible(showStatementTotals);
		statementTotalsBar.setManaged(showStatementTotals);
	}

	private void hideStatementTotals() {
		statementTotalsBar.setVisible(false);
		statementTotalsBar.setManaged(false);
	}

	private void applyResultFilter() {
		FilteredList<InvoiceReconciliation> filtered = new FilteredList<>(allResults,
				row -> showMatchedCheck.isSelected() || row.isNotable());
		resultsTable.setItems(filtered);
	}

	private void updateSummary(int extractedCount) {
		long accepted = allResults.stream().filter(InvoiceReconciliation::isAccepted).count();
		long notable = allResults.size() - accepted;
		String period = currentBatches.size() > 1 ? currentBatches.size() + " batches · "
				: !currentResultIsStatement || currentStatementPeriodStart == null || currentStatementPeriodEnd == null ? ""
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
		boolean completionPending = currentBatches.stream().anyMatch(batch ->
				batchIdsByDocument.containsValue(batch.batch().id()) && !completedBatchIds.contains(batch.batch().id()));
		boolean showSave = allAccepted && (pendingSave > 0 || completionPending);
		saveAcceptedButton.setText(pendingSave == 0 && completionPending ? "Retry completion" : "Save accepted documents");
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
				&& resultsView.isVisible()
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

	private void setCommitBusy(boolean busy) {
		commitBusy = busy;
		if (busy && pdfCache != null) pdfCache.stopPrefetch();
		saveAcceptedProgress.setVisible(busy);
		saveAcceptedProgress.setManaged(busy);
		showMatchedCheck.setDisable(busy);
		resultsTable.setDisable(busy);
		scanButton.setDisable(busy);
		refreshQueueButton.setDisable(busy);
		reviewAllButton.setDisable(busy || queueBusy);
		resultsBackButton.setDisable(busy);
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

	private record LoadedBatch(BatchSummary batch, ScanRun run, List<Long> documentIds) {}
	private record PreviewRectangle(double x, double y, double width, double height) {}

	private record ScanRun(List<ScannedInvoice> scanned,
			List<InvoiceReconciliation> reconciliations,
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations,
			Map<ScannedInvoice, Long> documentIds,
			LocalDate periodStart,
			LocalDate periodEnd,
			Long statementAmountCents,
			Double statementAmountConfidence,
			List<InvoiceSupplier> suppliers) {}
}
