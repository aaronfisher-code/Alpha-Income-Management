package controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.DocumentAiModels.DriveFolder;
import models.DocumentAiModels.DriveStatus;
import models.DocumentAiModels.FolderKind;
import models.DocumentAiModels.ScanSchedule;
import models.DocumentAiModels.ScanScheduleInput;
import models.DocumentAiModels.WatchFolder;
import models.DocumentAiModels.WatchFolderInput;
import services.DocumentAiService;
import services.LiveZConfiguration;
import services.ZDataService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/** Store-scoped application settings for Google Drive document ingestion. */
public final class SettingsController extends PageController {
    private static final String CONFIGURE_PERMISSION = "Document AI - Configure";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm")
            .withZone(ZoneId.systemDefault());

    @FXML private Label storeLabel;
    @FXML private Label driveStatusLabel;
    @FXML private Label accountLabel;
    @FXML private Label scannerStatusLabel;
    @FXML private Label scheduleStatusLabel;
    @FXML private Label lastErrorLabel;
    @FXML private ListView<WatchFolder> watchedFoldersList;
    @FXML private CheckBox scheduleEnabledCheck;
    @FXML private ChoiceBox<String> scheduleIntervalChoice;
    @FXML private Button connectDriveButton;
    @FXML private Button copyAuthorizationLinkButton;
    @FXML private Button disconnectDriveButton;
    @FXML private Button configureFoldersButton;
    @FXML private Button saveScheduleButton;
    @FXML private Button refreshButton;
    @FXML private Button testZConnectionButton;
    @FXML private Label zConnectionTestLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final Map<String, Integer> scheduleIntervals = new LinkedHashMap<>();
    private DocumentAiService documentAiService;
    private DriveStatus driveStatus = new DriveStatus(false, false, "", false);
    private ScanSchedule currentSchedule;
    private List<WatchFolder> currentWatchFolders = List.of();
    private IOException initializationError;
    private ZDataService zDataService;
    private boolean zIntegrationEnabled;
    private String pendingAuthorizationUrl;
    private Task<SettingsLoad> connectionMonitorTask;
    private boolean busy;
    private boolean zTestBusy;

    @FXML
    private void initialize() {
        scheduleIntervals.put("Every 15 minutes", 15);
        scheduleIntervals.put("Every 30 minutes", 30);
        scheduleIntervals.put("Every hour", 60);
        scheduleIntervals.put("Every 3 hours", 180);
        scheduleIntervals.put("Every 6 hours", 360);
        scheduleIntervals.put("Every 12 hours", 720);
        scheduleIntervals.put("Daily", 1440);
        scheduleIntervalChoice.getItems().setAll(scheduleIntervals.keySet());
        scheduleIntervalChoice.getSelectionModel().selectFirst();
        watchedFoldersList.setPlaceholder(new Label("No watched folders configured for this store."));
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "document-ai-settings");
            thread.setDaemon(true);
            return thread;
        });
        try {
            documentAiService = new DocumentAiService();
        } catch (IOException exception) {
            initializationError = exception;
        }
        try {
            zIntegrationEnabled = LiveZConfiguration.load().enabled();
            if (zIntegrationEnabled) zDataService = new ZDataService();
            zConnectionTestLabel.setText(zIntegrationEnabled ? "Not tested" : "Disabled in this installation");
        } catch (IOException exception) {
            zConnectionTestLabel.setText("Unable to load pharmacy connection settings");
        }
        updateDisabledState();
    }

    @Override
    public void fill() {
        if (!hasPermission(CONFIGURE_PERMISSION)) {
            throw new SecurityException(CONFIGURE_PERMISSION + " permission is required");
        }
        storeLabel.setText(main.getCurrentStore().getStoreName());
        if (initializationError != null) {
            setBusy(false, "Document AI settings are unavailable.");
            dialogPane.showError("Document AI settings unavailable", initializationError);
            return;
        }
        refreshSettings();
    }

    @FXML
    private void refreshSettings() {
        if (documentAiService == null || busy) return;
        setBusy(true, "Loading Google Drive settings…");
        Task<SettingsLoad> task = new Task<>() {
            @Override protected SettingsLoad call() {
                int storeId = storeId();
                DriveStatus status = documentAiService.driveStatus(storeId);
                List<WatchFolder> folders = status.connected()
                        ? documentAiService.watchFolders(storeId) : List.of();
                return new SettingsLoad(status, folders, documentAiService.schedule(storeId));
            }
        };
        task.setOnSucceeded(_ -> applySettings(task.getValue()));
        task.setOnFailed(_ -> failure("Unable to load Google Drive settings", task.getException()));
        executor.submit(task);
    }

    @FXML
    private void connectDrive() {
        if (documentAiService == null || busy) return;
        setBusy(true, "Creating a secure Google authorization request…");
        Task<String> task = new Task<>() {
            @Override protected String call() {
                return documentAiService.connect(storeId()).authorizationUrl();
            }
        };
        task.setOnSucceeded(_ -> {
            try {
                URI authorizationUri = URI.create(task.getValue());
                pendingAuthorizationUrl = authorizationUri.toString();
                copyAuthorizationLinkButton.setManaged(true);
                copyAuthorizationLinkButton.setVisible(true);
                setBusy(false, "Google sign-in is opening in your browser. If it does not open, copy the authorization link.");
                startConnectionMonitor();
                openBrowserInBackground(authorizationUri);
            } catch (Exception exception) {
                failure("Google returned an invalid authorization link", exception);
            }
        });
        task.setOnFailed(_ -> failure("Unable to connect Google Drive", task.getException()));
        executor.submit(task);
    }

    private void openBrowserInBackground(URI authorizationUri) {
        Task<Void> browserTask = new Task<>() {
            @Override protected Void call() throws IOException {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IOException("This computer cannot open a browser automatically");
                }
                Desktop.getDesktop().browse(authorizationUri);
                return null;
            }
        };
        browserTask.setOnFailed(_ -> {
            scheduleStatusLabel.setText("The browser could not be opened automatically. Use Copy authorization link, then paste it into your browser.");
            System.err.println("Unable to open Google authorization in the default browser: "
                    + errorMessage(browserTask.getException()));
        });
        executor.submit(browserTask);
    }

    private void startConnectionMonitor() {
        stopConnectionMonitor();
        int requestedStoreId = storeId();
        boolean initiallyConnected = driveStatus.connected();
        String initialAccount = driveStatus.account();
        connectionMonitorTask = new Task<>() {
            @Override protected SettingsLoad call() throws Exception {
                RuntimeException lastFailure = null;
                for (int attempt = 0; attempt < 300 && !isCancelled(); attempt++) {
                    Thread.sleep(2_000);
                    try {
                        DriveStatus status = documentAiService.driveStatus(requestedStoreId);
                        boolean connectionChanged = status.connected() && (!initiallyConnected
                                || !Objects.equals(initialAccount, status.account()));
                        if (connectionChanged) {
                            return new SettingsLoad(status, documentAiService.watchFolders(requestedStoreId),
                                    documentAiService.schedule(requestedStoreId));
                        }
                        lastFailure = null;
                    } catch (RuntimeException exception) {
                        // A temporary API failure should not make the user restart the OAuth flow.
                        lastFailure = exception;
                    }
                }
                if (isCancelled()) return null;
                if (lastFailure != null) throw lastFailure;
                throw new TimeoutException("Google authorization was not completed within ten minutes");
            }
        };
        Task<SettingsLoad> monitor = connectionMonitorTask;
        monitor.setOnSucceeded(_ -> {
            if (monitor != connectionMonitorTask || monitor.getValue() == null) return;
            applySettings(monitor.getValue());
            scheduleStatusLabel.setText("Google Drive connected as " + driveStatus.account() + ".");
            connectionMonitorTask = null;
        });
        monitor.setOnFailed(_ -> {
            if (monitor != connectionMonitorTask || monitor.isCancelled()) return;
            connectionMonitorTask = null;
            scheduleStatusLabel.setText("Automatic connection check stopped. Complete Google sign-in, then click Refresh.");
            System.err.println("Unable to monitor Google Drive authorization: "
                    + errorMessage(monitor.getException()));
        });
        executor.submit(monitor);
    }

    private void stopConnectionMonitor() {
        if (connectionMonitorTask != null && !connectionMonitorTask.isDone()) {
            connectionMonitorTask.cancel(true);
        }
        connectionMonitorTask = null;
    }

    @FXML
    private void copyAuthorizationLink() {
        if (pendingAuthorizationUrl == null || pendingAuthorizationUrl.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(pendingAuthorizationUrl);
        if (Clipboard.getSystemClipboard().setContent(content)) {
            scheduleStatusLabel.setText("Google authorization link copied. Paste it into your browser to continue.");
        } else {
            dialogPane.showError("Unable to copy Google authorization link",
                    new IOException("The system clipboard is unavailable"));
        }
    }

    @FXML
    private void disconnectDrive() {
        if (!driveStatus.connected() || busy) return;
        dialogPane.showWarning("Disconnect Google Drive?",
                "This removes the saved Google authorization and watched folders for "
                        + main.getCurrentStore().getStoreName() + ". Existing review items are retained.")
                .onClose(button -> {
                    if (!ButtonType.OK.equals(button)) return;
                    setBusy(true, "Disconnecting Google Drive…");
                    Task<Void> task = new Task<>() {
                        @Override protected Void call() {
                            documentAiService.disconnect(storeId());
                            return null;
                        }
                    };
                    task.setOnSucceeded(_ -> refreshAfterMutation("Google Drive disconnected."));
                    task.setOnFailed(_ -> failure("Unable to disconnect Google Drive", task.getException()));
                    executor.submit(task);
                });
    }

    @FXML
    private void configureFolders() {
        if (!driveStatus.connected() || busy) return;
        showFolderConfiguration();
    }

    @FXML
    private void saveSchedule() {
        Integer minutes = scheduleIntervals.get(scheduleIntervalChoice.getValue());
        if (documentAiService == null || minutes == null || busy) return;
        boolean enabled = scheduleEnabledCheck.isSelected();
        setBusy(true, "Saving the automatic scan schedule…");
        Task<ScanSchedule> task = new Task<>() {
            @Override protected ScanSchedule call() {
                return documentAiService.saveSchedule(storeId(), new ScanScheduleInput(enabled, minutes));
            }
        };
        task.setOnSucceeded(_ -> {
            currentSchedule = task.getValue();
            updateChrome(currentSchedule.enabled()
                    ? "Automatic scan schedule saved." : "Automatic scans are paused.");
            setBusy(false, scheduleStatusLabel.getText());
        });
        task.setOnFailed(_ -> failure("Unable to save the scan schedule", task.getException()));
        executor.submit(task);
    }

    @FXML
    private void testZConnection() {
        if (!zIntegrationEnabled || zDataService == null || busy || zTestBusy) return;
        zTestBusy = true;
        zConnectionTestLabel.getStyleClass().removeAll("settings-success", "settings-test-warning", "settings-test-error");
        zConnectionTestLabel.setText("Testing Alpha API → Z forwarder → SQL Server…");
        updateDisabledState();

        Task<ZDataService.ConnectionTest> task = new Task<>() {
            @Override protected ZDataService.ConnectionTest call() {
                return zDataService.testConnection(storeId());
            }
        };
        task.setOnSucceeded(_ -> {
            zTestBusy = false;
            var result = task.getValue();
            if (result.agentConnected() && result.sqlQuerySucceeded()) {
                String range = result.fromDate() + " to " + result.toDateExclusive() + " (exclusive)";
                if (result.rowCount() == 0) {
                    zConnectionTestLabel.setText("Connection passed · SQL Server responded, but no ScriptTotals rows were found for "
                            + range + ".");
                    zConnectionTestLabel.getStyleClass().add("settings-test-warning");
                } else {
                    zConnectionTestLabel.setText("End-to-end test passed · SQL Server responded with "
                            + result.rowCount() + " rows in " + result.elapsedMs() + " ms (" + range + ").");
                    zConnectionTestLabel.getStyleClass().add("settings-success");
                }
            } else {
                zConnectionTestLabel.setText("End-to-end test failed · the pharmacy query did not complete.");
                zConnectionTestLabel.getStyleClass().add("settings-test-error");
            }
            updateDisabledState();
        });
        task.setOnFailed(_ -> {
            zTestBusy = false;
            zConnectionTestLabel.setText("End-to-end test failed · " + errorMessage(task.getException()));
            zConnectionTestLabel.getStyleClass().add("settings-test-error");
            updateDisabledState();
        });
        executor.submit(task);
    }

    private void applySettings(SettingsLoad load) {
        driveStatus = load.driveStatus();
        if (driveStatus.connected()) clearPendingAuthorization();
        currentWatchFolders = load.watchFolders();
        currentSchedule = load.schedule();
        watchedFoldersList.getItems().setAll(currentWatchFolders);
        scheduleEnabledCheck.setSelected(currentSchedule.enabled());
        selectScheduleInterval(currentSchedule.intervalMinutes());
        updateChrome(null);
        setBusy(false, scheduleStatusLabel.getText());
    }

    private void updateChrome(String confirmation) {
        connectDriveButton.setText(driveStatus.connected() ? "Reconnect account" : "Connect Google Drive");
        driveStatusLabel.setText(!driveStatus.configured() ? "Server configuration required"
                : driveStatus.connected() ? "Connected" : "Not connected");
        accountLabel.setText(driveStatus.connected() ? driveStatus.account() : "No Google account connected");
        scannerStatusLabel.setText(driveStatus.scannerConfigured()
                ? "Gemini document extraction is available"
                : "Gemini document extraction is not configured on Alpha API");

        String timing;
        if (confirmation != null) timing = confirmation;
        else if (currentSchedule == null) timing = "Schedule unavailable";
        else if (!currentSchedule.enabled()) timing = "Automatic scans are paused";
        else if (currentSchedule.running()) timing = "A Drive scan is currently running";
        else timing = "Next scan " + formatTime(currentSchedule.nextRunAt())
                    + " · Last scan " + formatTime(currentSchedule.lastRunAt());
        scheduleStatusLabel.setText(timing);

        String lastError = currentSchedule == null ? null : currentSchedule.lastError();
        boolean hasError = lastError != null && !lastError.isBlank();
        lastErrorLabel.setText(hasError ? "Last scan error: " + lastError : "");
        lastErrorLabel.setVisible(hasError);
        lastErrorLabel.setManaged(hasError);
        updateDisabledState();
    }

    private void clearPendingAuthorization() {
        pendingAuthorizationUrl = null;
        copyAuthorizationLinkButton.setManaged(false);
        copyAuthorizationLinkButton.setVisible(false);
    }

    private void showFolderConfiguration() {
        Dialog<ButtonType> folderDialog = new Dialog<>();
        folderDialog.initOwner(main.getStg());
        folderDialog.setTitle("Google Drive watched folders");
        ButtonType save = new ButtonType("Save folders", ButtonBar.ButtonData.OK_DONE);
        folderDialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);

        Label pathLabel = new Label("My Drive");
        Label loadingLabel = new Label("");
        ListView<DriveFolder> folders = new ListView<>();
        folders.setPrefHeight(300);
        ListView<WatchFolderInput> watched = new ListView<>();
        watched.setPrefHeight(170);
        watched.setCellFactory(_ -> new ListCell<>() {
            @Override protected void updateItem(WatchFolderInput item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : (item.kind() == FolderKind.INVOICE ? "Invoices · " : "Statements · ")
                        + item.folderName());
            }
        });
        watched.getItems().setAll(currentWatchFolders.stream()
                .map(folder -> new WatchFolderInput(folder.folderId(), folder.folderName(), folder.kind())).toList());

        ChoiceBox<FolderKind> kind = new ChoiceBox<>(
                FXCollections.observableArrayList(FolderKind.INVOICE, FolderKind.STATEMENT));
        kind.setValue(FolderKind.INVOICE);
        kind.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(FolderKind value) {
                return value == FolderKind.STATEMENT ? "Statement folder" : "MARKED_OFF invoice folder";
            }
            @Override public FolderKind fromString(String value) { return FolderKind.INVOICE; }
        });

        Button open = new Button("Open folder");
        Button back = new Button("‹ Back");
        Button add = new Button("Watch selected folder");
        Button remove = new Button("Remove selected");
        back.setDisable(true);
        open.disableProperty().bind(folders.getSelectionModel().selectedItemProperty().isNull());
        add.disableProperty().bind(folders.getSelectionModel().selectedItemProperty().isNull());
        remove.disableProperty().bind(watched.getSelectionModel().selectedItemProperty().isNull());

        VBox root = new VBox(9,
                new Label("Browse Google Drive"),
                new HBox(8, back, pathLabel), folders, loadingLabel,
                new HBox(8, open, kind, add),
                new Label("Watched by this store"), watched, remove);
        root.setPadding(new Insets(12));
        root.setPrefWidth(670);
        folderDialog.getDialogPane().setContent(root);

        class Browser {
            final List<DriveFolder> path = new ArrayList<>();
            void load(String parentId) {
                loadingLabel.setText("Loading folders…");
                folders.setDisable(true);
                Task<List<DriveFolder>> task = new Task<>() {
                    @Override protected List<DriveFolder> call() {
                        return documentAiService.folders(storeId(), parentId);
                    }
                };
                task.setOnSucceeded(_ -> {
                    folders.getItems().setAll(task.getValue());
                    folders.setDisable(false);
                    loadingLabel.setText(task.getValue().isEmpty() ? "No child folders here." : "");
                    pathLabel.setText(path.isEmpty() ? "My Drive" : "My Drive / " + path.stream()
                            .map(DriveFolder::name).collect(java.util.stream.Collectors.joining(" / ")));
                    back.setDisable(path.isEmpty());
                });
                task.setOnFailed(_ -> {
                    folders.setDisable(false);
                    loadingLabel.setText(folderLoadErrorMessage(task.getException()));
                });
                executor.submit(task);
            }
        }

        Browser browser = new Browser();
        open.setOnAction(_ -> {
            DriveFolder selected = folders.getSelectionModel().getSelectedItem();
            if (selected != null) {
                browser.path.add(selected);
                browser.load(selected.id());
            }
        });
        back.setOnAction(_ -> {
            if (!browser.path.isEmpty()) browser.path.remove(browser.path.size() - 1);
            browser.load(browser.path.isEmpty() ? null : browser.path.get(browser.path.size() - 1).id());
        });
        add.setOnAction(_ -> {
            DriveFolder selected = folders.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            WatchFolderInput candidate = new WatchFolderInput(selected.id(), selected.name(), kind.getValue());
            boolean duplicate = watched.getItems().stream().anyMatch(existing ->
                    existing.folderId().equals(candidate.folderId()) && existing.kind() == candidate.kind());
            if (!duplicate) watched.getItems().add(candidate);
        });
        remove.setOnAction(_ -> watched.getItems().remove(watched.getSelectionModel().getSelectedItem()));
        folders.setOnMouseClicked(event -> { if (event.getClickCount() == 2) open.fire(); });
        browser.load(null);

        Optional<ButtonType> result = folderDialog.showAndWait();
        if (result.isEmpty() || result.get() != save) return;
        List<WatchFolderInput> selection = List.copyOf(watched.getItems());
        setBusy(true, "Saving watched folders…");
        Task<List<WatchFolder>> task = new Task<>() {
            @Override protected List<WatchFolder> call() {
                return documentAiService.saveWatchFolders(storeId(), selection);
            }
        };
        task.setOnSucceeded(_ -> refreshAfterMutation("Watched folders saved."));
        task.setOnFailed(_ -> failure("Unable to save watched folders", task.getException()));
        executor.submit(task);
    }

    private void refreshAfterMutation(String message) {
        busy = false;
        scheduleStatusLabel.setText(message);
        refreshSettings();
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        progressIndicator.setVisible(value);
        progressIndicator.setManaged(value);
        if (message != null) scheduleStatusLabel.setText(message);
        updateDisabledState();
    }

    private void updateDisabledState() {
        boolean connected = driveStatus.connected();
        connectDriveButton.setDisable(busy || !driveStatus.configured());
        disconnectDriveButton.setDisable(busy || !connected);
        configureFoldersButton.setDisable(busy || !connected);
        scheduleEnabledCheck.setDisable(busy);
        scheduleIntervalChoice.setDisable(busy);
        saveScheduleButton.setDisable(busy);
        refreshButton.setDisable(busy);
        watchedFoldersList.setDisable(busy);
        testZConnectionButton.setDisable(busy || zTestBusy || !zIntegrationEnabled || zDataService == null);
    }

    private void selectScheduleInterval(int minutes) {
        scheduleIntervals.entrySet().stream().filter(entry -> entry.getValue() == minutes)
                .map(Map.Entry::getKey).findFirst().ifPresent(scheduleIntervalChoice::setValue);
    }

    private boolean hasPermission(String permissionName) {
        return main.getCurrentUser().getPermissions().stream()
                .anyMatch(permission -> permissionName.equals(permission.getPermissionName()));
    }

    private int storeId() { return main.getCurrentStore().getStoreID(); }

    private static String formatTime(Instant value) {
        return value == null ? "not scheduled" : DATE_TIME.format(value);
    }

    private void failure(String title, Throwable error) {
        setBusy(false, title + ".");
        Throwable root = rootCause(error);
        dialogPane.showError(title, errorMessage(error),
                root instanceof Exception exception ? exception : new RuntimeException(root));
    }

    private static String errorMessage(Throwable value) {
        Throwable root = rootCause(value);
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    private static String folderLoadErrorMessage(Throwable value) {
        String message = errorMessage(value);
        if (message.contains("403") || message.contains("insufficient authentication scopes")
                || message.contains("ACCESS_TOKEN_SCOPE_INSUFFICIENT")) {
            return "Google Drive file-management access was not granted. Disconnect Google Drive, reconnect it, "
                    + "and approve the Drive file management permission.";
        }
        return "Unable to load folders: " + message;
    }

    private static Throwable rootCause(Throwable value) {
        if (value == null) return new IllegalStateException("No error details were returned");
        Throwable current = value;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }

    private record SettingsLoad(DriveStatus driveStatus, List<WatchFolder> watchFolders,
            ScanSchedule schedule) {}

    @Override
    public void shutdownExecutor() {
        stopConnectionMonitor();
        super.shutdownExecutor();
    }
}
