package controllers;

import com.dlsc.gemsfx.DialogPane;
import com.jfoenix.controls.JFXNodesList;
import components.CustomDateStringConverter;
import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.LeaveRequest;
import models.Shift;
import models.SpecialDateObj;
import models.User;
import org.controlsfx.control.PopOver;
import services.LeaveService;
import services.RosterService;
import services.UserService;
import utils.AnimationUtils;
import utils.ValidatorUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;
import static java.time.temporal.ChronoUnit.DAYS;


public class RosterPageController extends PageController {

    @FXML private Label popoverLabel;
    @FXML private MFXDatePicker startDate;
    @FXML private VBox monBox, tueBox, wedBox, thuBox, friBox, satBox, sunBox, editShiftPopover;
    @FXML private GridPane weekdayBox;
    @FXML private JFXNodesList addList;
    @FXML private GridPane shiftCardGrid;
    @FXML private FlowPane datePickerPane;
    @FXML private Region contentDarken;
    @FXML private MFXTextField startTimeField,endTimeField,repeatValue,thirtyMinBreaks,tenMinBreaks;
    @FXML private Button openStartTimePicker,openEndTimePicker,deleteButton,manageLeaveButton,exportDataButton;
    @FXML private MFXFilterComboBox<User> employeeSelect;
    @FXML private MFXToggleButton repeatingShiftToggle;
    @FXML private Label repeatLabel;
    @FXML private MFXComboBox<String> repeatUnit;
    @FXML private MFXButton saveButton;
    @FXML private Label employeeSelectValidationLabel, startDateValidationLabel, startTimeValidationLabel, endTimeValidationLabel, tenMinBreaksValidationLabel, thirtyMinBreaksValidationLabel, repeatValueValidationLabel;
    @FXML private MFXProgressSpinner progressSpinner;
    private MFXDatePicker datePkr;
    private PopOver currentTimePopover;
    private LocalTime startTime,endTime;
    private UserService userService;
    private RosterService rosterService;
    private LeaveService leaveService;

    @FXML
    private void initialize() {
        try {
            userService = new UserService();
            rosterService = new RosterService();
            leaveService = new LeaveService();
            executor = Executors.newCachedThreadPool();
        } catch (IOException ex) {
            dialogPane.showError("Failed to initialize services", ex);
        }
    }

    public void fill() {
        datePkr = new MFXDatePicker();
        datePkr.setOnAction(_ -> updatePage());
        datePickerPane.getChildren().add(1,datePkr);
        datePkr.setValue(main.getCurrentDate());
        datePkr.setText(main.getCurrentDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
        datePkr.getStyleClass().add("custDatePicker");
        datePkr.getStylesheets().add("/views/CSS/RosterPage.css");
        exportDataButton.setOnAction(_ -> {
            dialog = new DialogPane.Dialog<>(dialogPane, DialogPane.Type.BLANK);
            dialog.setPadding(false);
            dialog.setContent(createExportDialog());
            dialogPane.showDialog(dialog);
        });
        Task<List<User>> getUsersTask = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getAllUserEmployments(main.getCurrentStore().getStoreID());
            }
        };
        getUsersTask.setOnSucceeded(_ -> {
            List<User> currentUsers = getUsersTask.getValue();
            if (main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))) {
                employeeSelect.getItems().addAll(currentUsers);
            } else if (main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts"))) {
                employeeSelect.getItems().addAll(currentUsers.stream()
                        .filter(u -> u.getUserID()==main.getCurrentUser().getUserID())
                        .toList());
            } else {
                addList.setVisible(false);
            }
            progressSpinner.setVisible(false);
        });
        getUsersTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while fetching users", getUsersTask.getException());
            progressSpinner.setVisible(false);
        });
        progressSpinner.setVisible(true);
        executor.submit(getUsersTask);
        manageLeaveButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Manage Leave")));
        exportDataButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Export")));
        openStartTimePicker.setOnAction(_ -> {
            if(!startTimeField.getText().isEmpty()){
                try{
                    startTime = LocalTime.parse(startTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));
                }catch (DateTimeParseException e){
                    startTime = LocalTime.MIDNIGHT;
                }
            } else {
                startTime = LocalTime.MIDNIGHT;
            }
            openTimePicker(startTimeField,startTime);
        });
        openEndTimePicker.setOnAction(_ -> {
            if(!endTimeField.getText().isEmpty()) {
                try {
                    endTime = LocalTime.parse(endTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));
                } catch (DateTimeParseException e) {
                    endTime = LocalTime.MIDNIGHT;
                }
            } else{
                endTime = LocalTime.MIDNIGHT;
            }
            openTimePicker(endTimeField,endTime);
        });
        repeatUnit.getItems().add("Days");
        repeatUnit.getItems().add("Weeks");
        repeatingShiftToggle.setMainColor(Color.web("#0F60FF"));
        repeatingShiftToggle.setOnAction(_ -> {
            if(repeatingShiftToggle.isSelected()){
                repeatLabel.setDisable(false);
                repeatValue.setDisable(false);
                repeatUnit.setDisable(false);
            }else{
                repeatLabel.setDisable(true);
                repeatValue.setDisable(true);
                repeatUnit.setDisable(true);
            }
        });
        addList.setRotate(180);
        ValidatorUtils.setupRegexValidation(employeeSelect,employeeSelectValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(startDate,startDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(startTimeField,startTimeValidationLabel,ValidatorUtils.TIME_REGEX,ValidatorUtils.TIME_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(endTimeField,endTimeValidationLabel,ValidatorUtils.TIME_REGEX,ValidatorUtils.TIME_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(tenMinBreaks,tenMinBreaksValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(thirtyMinBreaks,thirtyMinBreaksValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(repeatValue,repeatValueValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(repeatUnit,repeatValueValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
        startDate.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
        datePkr.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
        updatePage();
    }

    public void updateDay(LocalDate date, VBox shiftContainer, int dayOfWeek, List<Shift> allShifts,List<Shift> allModifications,List<LeaveRequest> allLeaveRequests,List<SpecialDateObj> specialDates) {
        //empty the contents of the current day VBox
        shiftContainer.getChildren().removeAll(shiftContainer.getChildren());
        long weekDay = date.getDayOfWeek().getValue();
        //Create Day Header card
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/RosterDayCard.fxml"));
        VBox rosterDayCard = null;
        try {
            rosterDayCard = loader.load();
        } catch (IOException ex) {
            dialogPane.showError("Error","An error occurred while loading the day card", ex);
        }
        RosterDayCardController rdc = loader.getController();
        rdc.setMain(main);
        rdc.setDate(date.minusDays(weekDay - dayOfWeek));
        rdc.setParent(this);
        for(SpecialDateObj sd: specialDates){
            if(sd.getEventDate().equals(date.minusDays(weekDay - dayOfWeek))){
                rdc.setSpecialDate(sd);
            }
        }
        rdc.fill();
        if (rdc.getDate() == date) {
            //add blue selection formatting if date is current
            rdc.select();
        }
        HBox.setHgrow(rosterDayCard, Priority.ALWAYS);
        weekdayBox.add(rosterDayCard,dayOfWeek-1,0);
        //Create Shift Cards
        for (Shift s : allShifts) {
            boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) >= 0);
            boolean equalDay = s.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek));
            boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(date.minusDays(weekDay - dayOfWeek));
            if ((equalDay || repeatShiftDay) && !pastEnd) {
                Shift updatedShift = s;
                boolean shiftIsModified = false;
                for(Shift m: allModifications){
                    if(m.getShiftID()==s.getShiftID() && m.getOriginalDate().equals(date.minusDays(weekDay - dayOfWeek))){
                        updatedShift = m;
                        shiftIsModified=true;
                    }
                }
                if(!shiftIsModified || (updatedShift.getShiftStartDate()!=null&&updatedShift.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek)))){
                    try {
                        loader = new FXMLLoader(getClass().getResource("/views/FXML/ShiftCard.fxml"));
                        StackPane shiftCard = loader.load();
                        ShiftCardController sc = loader.getController();
                        sc.setMain(main);
                        sc.setShift(updatedShift);
                        sc.fill();
                        //Check for leave requests
                        for(LeaveRequest lr: allLeaveRequests){
                            LocalDateTime shiftStart = LocalDateTime.of(date.minusDays(weekDay - dayOfWeek),updatedShift.getShiftStartTime());
                            LocalDateTime shiftEnd = LocalDateTime.of(date.minusDays(weekDay - dayOfWeek),updatedShift.getShiftEndTime());
                            if(lr.getUserID()==updatedShift.getUserID()&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                                sc.setModification(lr.getLeaveType());
                            }
                        }
                        sc.setDate(date.minusDays(weekDay - dayOfWeek));
                        if(shiftIsModified)
                            sc.showDifference(s,updatedShift);
                        Shift finalS = updatedShift;
                        if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts") && finalS.getUserID()==main.getCurrentUser().getUserID())||
                                main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))){
                            shiftCard.setOnMouseClicked(_ -> openPopover(finalS,sc.getDate()));
                        }
                        shiftContainer.getChildren().add(shiftCard);
                    } catch (Exception ex) {
                        dialogPane.showError("Error","An error occurred while loading the shift card", ex);
                    }
                }
            }
        }
        for(Shift m: allModifications){
            if(m.getShiftStartDate()!=null&&m.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek))&&(!(m.getShiftStartDate().equals(m.getOriginalDate())))){
                try {
                    loader = new FXMLLoader(getClass().getResource("/views/FXML/ShiftCard.fxml"));
                    StackPane shiftCard = loader.load();
                    ShiftCardController sc = loader.getController();
                    sc.setMain(main);
                    sc.setShift(m);
                    sc.fill();
                    //Check for leave requests
                    for(LeaveRequest lr: allLeaveRequests){
                        LocalDateTime shiftStart = LocalDateTime.of(date.minusDays(weekDay - dayOfWeek),m.getShiftStartTime());
                        LocalDateTime shiftEnd = LocalDateTime.of(date.minusDays(weekDay - dayOfWeek),m.getShiftEndTime());
                        if(lr.getUserID()==m.getUserID()&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                            sc.setModification(lr.getLeaveType());
                        }
                    }
                    sc.setDate(date.minusDays(weekDay - dayOfWeek));
                    if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts") && m.getUserID()==main.getCurrentUser().getUserID())||
                            main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))){
                        shiftCard.setOnMouseClicked(_ -> openPopover(m,sc.getDate()));
                    }
                    shiftContainer.getChildren().add(shiftCard);
                } catch (Exception ex) {
                    dialogPane.showError("Error","An error occurred while loading the shift card", ex);
                }
            }
        }
    }

    public void updatePage() {
        progressSpinner.setVisible(true);
        Task<Void> updatePageTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long weekDay = datePkr.getValue().getDayOfWeek().getValue();
                LocalDate weekStart = datePkr.getValue().minusDays(weekDay - 1);
                LocalDate weekEnd = datePkr.getValue().plusDays(7 - weekDay);

                CompletableFuture<List<Shift>> shiftsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getShifts(main.getCurrentStore().getStoreID(), weekStart, weekEnd);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);

                CompletableFuture<List<Shift>> modificationsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), weekStart, weekEnd);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);

                CompletableFuture<List<LeaveRequest>> leaveRequestsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), weekStart, weekEnd);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);

                CompletableFuture<List<SpecialDateObj>> specialDatesFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getSpecialDates(weekStart, weekEnd);
                    } catch (Exception e) {
                        // Return an empty list instead of throwing an exception
                        return new ArrayList<>();
                    }
                }, executor);

                try {
                    List<Shift> allShifts = shiftsFuture.get();
                    List<Shift> allModifications = modificationsFuture.get();
                    List<LeaveRequest> allLeaveRequests = leaveRequestsFuture.get();
                    List<SpecialDateObj> specialDates = specialDatesFuture.get();

                    Platform.runLater(() -> {
                        weekdayBox.getChildren().clear();
                        updateDay(datePkr.getValue(), monBox, 1, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), tueBox, 2, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), wedBox, 3, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), thuBox, 4, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), friBox, 5, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), satBox, 6, allShifts, allModifications, allLeaveRequests, specialDates);
                        updateDay(datePkr.getValue(), sunBox, 7, allShifts, allModifications, allLeaveRequests, specialDates);
                        adjustGridSize();
                    });
                } catch (Exception e) {
                    throw new Exception("Error updating page: " + e.getMessage(), e);
                }
                return null;
            }
        };
        updatePageTask.setOnSucceeded(_ -> {
            progressSpinner.setVisible(false);
        });
        updatePageTask.setOnFailed(_ -> {
            Throwable exception = updatePageTask.getException();
            dialogPane.showError("Error", "An error occurred while updating the page", exception);
            progressSpinner.setVisible(false);
        });
        executor.submit(updatePageTask);
    }

    public void weekForward() {
        setDatePkr(datePkr.getValue().plusWeeks(1));
    }

    public void weekBackward() {
        setDatePkr(datePkr.getValue().minusWeeks(1));
    }

    public void setDatePkr(LocalDate date) {
        datePkr.setValue(date);
    }

    public void openPopover(){
        popoverLabel.setText("Add a new Shift");
        deleteButton.setVisible(false);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editShiftPopover,0);
        employeeSelect.setValue(null);
        employeeSelect.clearSelection();
        startDate.setValue(null);
        startTimeField.setText("");
        endTimeField.setText("");
        thirtyMinBreaks.setText("");
        tenMinBreaks.setText("");
        repeatingShiftToggle.setSelected(false);
        repeatValue.setDisable(true);
        repeatUnit.setDisable(true);
        repeatLabel.setDisable(true);
        repeatValue.setText("");
        repeatUnit.clearSelection();
        repeatUnit.setValue(null);
        repeatUnit.setText("");
        saveButton.setOnAction(_ -> addShift(null,null,null,false));
    }

    public void openPopover(Shift s,LocalDate shiftCardDate){
        popoverLabel.setText("Edit shift");
        deleteButton.setVisible(true);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editShiftPopover,0);
        try {
            employeeSelect.setValue(userService.getUserByID(s.getUserID()));
        } catch (Exception ex) {
            dialogPane.showError("Error","An error occurred while fetching user", ex);
        }
        startDate.setValue(shiftCardDate);
        startTimeField.setText(s.getShiftStartTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        endTimeField.setText(s.getShiftEndTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        thirtyMinBreaks.setText(String.valueOf(s.getThirtyMinBreaks()));
        tenMinBreaks.setText(String.valueOf(s.getTenMinBreaks()));
        if(s.isRepeating()){
            repeatingShiftToggle.setSelected(true);
            repeatValue.setDisable(false);
            repeatUnit.setDisable(false);
            repeatLabel.setDisable(false);
            repeatValue.setText(String.valueOf(s.getDaysPerRepeat()));
            repeatUnit.selectFirst();
        }else{
            repeatingShiftToggle.setSelected(false);
            repeatValue.setDisable(true);
            repeatUnit.setDisable(true);
            repeatLabel.setDisable(true);
            repeatValue.setText("");
            repeatUnit.clearSelection();
        }
        saveButton.setOnAction(_ -> {
            if(s.isRepeating()){
                dialog = new DialogPane.Dialog<>(dialogPane, BLANK);
                dialog.setPadding(false);
                dialog.setContent(createCalendarEditDialog(s,shiftCardDate));
                dialogPane.showDialog(dialog);
            }else{
                editShift(s);
            }
        });
        deleteButton.setOnAction(_ -> {
            dialog = new DialogPane.Dialog<>(dialogPane, BLANK);
            dialog.setPadding(false);
            dialog.setContent(createCalendarDeleteDialog(s,shiftCardDate));
            dialogPane.showDialog(dialog);
        });
    }

    public void closePopover(){
        AnimationUtils.slideIn(editShiftPopover,425);
        employeeSelectValidationLabel.setVisible(false);
        startTimeValidationLabel.setVisible(false);
        endTimeValidationLabel.setVisible(false);
        startDateValidationLabel.setVisible(false);
        thirtyMinBreaksValidationLabel.setVisible(false);
        tenMinBreaksValidationLabel.setVisible(false);
        repeatValueValidationLabel.setVisible(false);
        contentDarken.setVisible(false);
    }

    public void openTimePicker(MFXTextField parent, LocalTime time){
        if(currentTimePopover !=null&& currentTimePopover.isShowing()){
            currentTimePopover.hide();
        }else {
            PopOver timePickerMenu = new PopOver();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/TimePickerContent.fxml"));
            VBox timePickerContent = null;
            try {
                timePickerContent = loader.load();
            } catch (IOException ex) {
                dialogPane.showError("Error","An error occurred while loading the time picker", ex);
            }
            TimePickerContentController rdc = loader.getController();
            rdc.setMain(main);
            rdc.fill();
            rdc.setCurrentTime(time);
            timePickerMenu.setOpacity(1);
            timePickerMenu.setContentNode(timePickerContent);
            timePickerMenu.setArrowSize(0);
            timePickerMenu.setAnimated(true);
            timePickerMenu.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
            timePickerMenu.setAutoHide(true);
            timePickerMenu.setDetachable(false);
            timePickerMenu.setHideOnEscape(true);
            timePickerMenu.setCornerRadius(10);
            timePickerMenu.setArrowIndent(0);
            timePickerMenu.show(parent);
            currentTimePopover =timePickerMenu;
            timePickerMenu.setOnHidden(_ -> {
                rdc.updateTime();
                parent.setText(rdc.getTimeString());
            });
            parent.requestFocus();
        }
    }

    public void addShift(Shift previousShift, LocalDate manualStartDate, LocalDate originalShiftDate, boolean deleteShift) {
        if (!validateInputs()) {
            return;
        }
        progressSpinner.setVisible(true);
        Task<Void> addShiftTask = new Task<>() {
            @Override
            protected Void call() {
                User selectedUser = employeeSelect.getValue();
                LocalDate shiftDate = manualStartDate == null ? startDate.getValue() : manualStartDate;
                if (deleteShift) {
                    shiftDate = null;
                }
                Shift newShift = createShiftFromInputs(selectedUser.getUserID(), shiftDate);
                if (previousShift != null) {
                    Shift modification = createModificationFromShift(newShift, previousShift.getShiftID(), originalShiftDate);
                    rosterService.addShiftModification(modification);
                } else {
                    rosterService.addShift(newShift);
                }
                return null;
            }
        };
        addShiftTask.setOnSucceeded(_ -> {
            updatePage();
            if (manualStartDate == null) {
                dialogPane.showInformation("Success", "Shift created successfully");
            } else if (previousShift != null) {
                if (deleteShift) {
                    dialogPane.showInformation("Success", "Shift deleted successfully");
                } else {
                    dialogPane.showInformation("Success", "Shift edited successfully");
                }
            }
            progressSpinner.setVisible(false);
        });
        addShiftTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while saving the shift. Please try again.", addShiftTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(addShiftTask);
    }

    private boolean validateInputs() {
        if (!employeeSelect.isValid()) {
            employeeSelect.requestFocus();
            return false;
        }
        if (!startDate.isValid()) {
            startDate.requestFocus();
            return false;
        }
        if (!startTimeField.isValid()) {
            startTimeField.requestFocus();
            return false;
        }
        if (!endTimeField.isValid()) {
            endTimeField.requestFocus();
            return false;
        }
        if (!thirtyMinBreaks.isValid()) {
            thirtyMinBreaks.requestFocus();
            return false;
        }
        if (!tenMinBreaks.isValid()) {
            tenMinBreaks.requestFocus();
            return false;
        }
        if (!repeatValue.isValid()) {
            repeatValue.requestFocus();
            return false;
        }
        if(repeatingShiftToggle.isSelected() && repeatUnit.getValue() == null) {
            repeatUnit.validate();
            repeatUnit.requestFocus();
            return false;
        }
        LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
        LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
        if (startTime.isAfter(endTime)) {
            startTimeField.requestFocus();
            startTimeValidationLabel.setText("Start time must be before end time");
            startTimeValidationLabel.setVisible(true);
            return false;
        }
        startTimeValidationLabel.setVisible(false);
        return true;
    }

    private Shift createShiftFromInputs(int userID, LocalDate shiftDate) {
        LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
        LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
        int thirtyMin = thirtyMinBreaks.getText().isEmpty() ? 0 : Integer.parseInt(thirtyMinBreaks.getText());
        int tenMin = tenMinBreaks.getText().isEmpty() ? 0 : Integer.parseInt(tenMinBreaks.getText());
        int daysPerRepeat = 1;
        if (repeatingShiftToggle.isSelected()) {
            int multiplier = repeatUnit.getValue().equals("Weeks") ? 7 : 1;
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        }
        Shift shift = new Shift();
        shift.setStoreID(main.getCurrentStore().getStoreID());
        shift.setUserID(userID);
        shift.setShiftStartTime(startTime);
        shift.setShiftEndTime(endTime);
        shift.setShiftStartDate(shiftDate);
        shift.setThirtyMinBreaks(thirtyMin);
        shift.setTenMinBreaks(tenMin);
        shift.setRepeating(repeatingShiftToggle.isSelected());
        shift.setDaysPerRepeat(daysPerRepeat);
        return shift;
    }

    private Shift createModificationFromShift(Shift shift, int originalShiftId, LocalDate originalDate) {
        Shift modification = new Shift();
        modification.setStoreID(shift.getStoreID());
        modification.setUserID(shift.getUserID());
        modification.setShiftStartTime(shift.getShiftStartTime());
        modification.setShiftEndTime(shift.getShiftEndTime());
        modification.setShiftStartDate(shift.getShiftStartDate());
        modification.setThirtyMinBreaks(shift.getThirtyMinBreaks());
        modification.setTenMinBreaks(shift.getTenMinBreaks());
        modification.setRepeating(shift.isRepeating());
        modification.setDaysPerRepeat(shift.getDaysPerRepeat());
        modification.setShiftID(originalShiftId);
        modification.setOriginalDate(originalDate);
        return modification;
    }

    public void editShift(Shift s) {
        if (!validateInputs()) {
            return;
        }
        progressSpinner.setVisible(true);
        Task<Void> editShiftTask = new Task<>() {
            @Override
            protected Void call() {
                int userID = employeeSelect.getValue().getUserID();
                LocalDate sDate = startDate.getValue();
                LocalTime sTime = LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                LocalTime eTime = LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                int thirtyMin = thirtyMinBreaks.getText().isEmpty() ? 0 : Integer.parseInt(thirtyMinBreaks.getText());
                int tenMin = tenMinBreaks.getText().isEmpty() ? 0 : Integer.parseInt(tenMinBreaks.getText());
                int daysPerRepeat = 1;
                if (repeatingShiftToggle.isSelected()) {
                    int multiplier = repeatUnit.getSelectedItem().equals("Weeks") ? 7 : 1;
                    daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
                }
                s.setUserID(userID);
                s.setShiftStartTime(sTime);
                s.setShiftEndTime(eTime);
                s.setShiftStartDate(sDate);
                s.setThirtyMinBreaks(thirtyMin);
                s.setTenMinBreaks(tenMin);
                s.setRepeating(repeatingShiftToggle.isSelected());
                s.setDaysPerRepeat(daysPerRepeat);
                rosterService.updateShift(s);
                rosterService.deleteShiftModifications(s.getShiftID());
                return null;
            }
        };
        editShiftTask.setOnSucceeded(_ -> {
            updatePage();
            dialogPane.showInformation("Success", "Shifts edited successfully");
            progressSpinner.setVisible(false);
        });
        editShiftTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while saving the shift. Please try again.", editShiftTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(editShiftTask);
    }

    public void deleteShift(Shift s) {
        progressSpinner.setVisible(true);
        Task<Void> deleteShiftTask = new Task<>() {
            @Override
            protected Void call() {
                rosterService.deleteShift(s.getShiftID());
                rosterService.deleteShiftModifications(s.getShiftID());
                return null;
            }
        };
        deleteShiftTask.setOnSucceeded(_ -> {
            updatePage();
            dialogPane.showInformation("Success", "Shift deleted successfully");
            progressSpinner.setVisible(false);
        });
        deleteShiftTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while deleting the shift. Please try again.", deleteShiftTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(deleteShiftTask);
    }

    public void editFutureShifts(Shift s, LocalDate shiftCardDate) {
        progressSpinner.setVisible(true);
        Task<Void> editFutureShiftsTask = new Task<>() {
            @Override
            protected Void call() {
                rosterService.updateShiftEndDate(s.getShiftID(), shiftCardDate.minusDays(1));
                addShift(null, shiftCardDate, null, false);
                return null;
            }
        };
        editFutureShiftsTask.setOnSucceeded(_ -> {
            updatePage();
            dialogPane.showInformation("Success", "Future shifts have been updated successfully.");
            progressSpinner.setVisible(false);
        });
        editFutureShiftsTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while updating future shifts. Please try again.", editFutureShiftsTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(editFutureShiftsTask);
    }

    public void deleteFutureShifts(Shift s, LocalDate shiftCardDate) {
        progressSpinner.setVisible(true);
        Task<Void> deleteFutureShiftsTask = new Task<>() {
            @Override
            protected Void call() {
                rosterService.updateShiftEndDate(s.getShiftID(), shiftCardDate.minusDays(1));
                rosterService.deleteShiftModifications(s.getShiftID(), shiftCardDate.minusDays(1));
                return null;
            }
        };
        deleteFutureShiftsTask.setOnSucceeded(_ -> {
            updatePage();
            dialogPane.showInformation("Success", "Shifts deleted successfully");
            progressSpinner.setVisible(false);
        });
        deleteFutureShiftsTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while deleting the shifts. Please try again.", deleteFutureShiftsTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(deleteFutureShiftsTask);
    }

    public void editCurrentShift(Shift s, LocalDate shiftCardDate) {
        progressSpinner.setVisible(true);
        Task<Void> editCurrentShiftTask = new Task<>() {
            @Override
            protected Void call() {
                addShift(s, startDate.getValue(), s.getOriginalDate() == null ? shiftCardDate : s.getOriginalDate(), false);
                return null;
            }
        };
        editCurrentShiftTask.setOnSucceeded(_ -> {
            updatePage();
            progressSpinner.setVisible(false);
        });
        editCurrentShiftTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while editing the current shift. Please try again.", editCurrentShiftTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(editCurrentShiftTask);
    }

    public void deleteCurrentShift(Shift s, LocalDate shiftCardDate) {
        progressSpinner.setVisible(true);
        Task<Void> deleteCurrentShiftTask = new Task<>() {
            @Override
            protected Void call() {
                addShift(s, LocalDate.now(), s.getOriginalDate() == null ? shiftCardDate : s.getOriginalDate(), true);
                return null;
            }
        };
        deleteCurrentShiftTask.setOnSucceeded(_ -> {
            updatePage();
            progressSpinner.setVisible(false);
        });
        deleteCurrentShiftTask.setOnFailed(_ -> {
            dialogPane.showError("Error", "An error occurred while deleting the current shift. Please try again.", deleteCurrentShiftTask.getException());
            progressSpinner.setVisible(false);
        });
        executor.submit(deleteCurrentShiftTask);
    }

    private Node createCalendarEditDialog(Shift s,LocalDate shiftCardDate) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/CalendarEditDialog.fxml"));
        StackPane calendarEditDialog = null;
        try {
            calendarEditDialog = loader.load();

        } catch (IOException ex) {
            dialogPane.showError("Error","An error occurred while loading the edit dialog", ex);
        }
        CalendarEditDialogController dialogController = loader.getController();
        dialogController.fill(s,shiftCardDate);
        dialogController.setParent(this);
        return calendarEditDialog;
    }

    private Node createCalendarDeleteDialog(Shift s,LocalDate shiftCardDate) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/CalendarDeleteDialog.fxml"));
        StackPane calendarDeleteDialog = null;
        try {
            calendarDeleteDialog = loader.load();

        } catch (IOException ex) {
            dialogPane.showError("Error","An error occurred while loading the delete dialog", ex);
        }
        CalendarDeleteDialogController dialogController = loader.getController();
        dialogController.fill(s,shiftCardDate);
        dialogController.setParent(this);
        return calendarDeleteDialog;
    }

    public void createRosterDayEditDialog(LocalDate date){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/RosterDayEdit.fxml"));
        StackPane rosterDayEdit = null;
        try {
            rosterDayEdit = loader.load();

        } catch (IOException ex) {
            dialogPane.showError("Error","An error occurred while loading the edit dialog", ex);
        }
        EditRosterDayController dialogController = loader.getController();
        dialogController.setParent(this);
        dialogController.setRosterDayDate(date);
        dialogController.fill();
        dialog = new DialogPane.Dialog<>(dialogPane, BLANK);
        dialog.setPadding(false);
        dialog.setContent(rosterDayEdit);
        dialogPane.showDialog(dialog);
    }

    public void addNewLeave() {
        MainMenuController m = (MainMenuController) main.getController();
        m.changePage("/views/FXML/LeaveManagementPage.fxml");
    }

    private Node createExportDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ExportTool.fxml"));
        VBox exportDialog = null;
        try {
            exportDialog = loader.load();
        } catch (IOException e) {
            dialogPane.showError("Error","An error occurred while trying to open the export dialog",e);
        }
        ExportToolController dialogController = loader.getController();
        dialogController.setParent(this);
        dialogController.setMain(this.main);
        dialogController.fill();
        return exportDialog;
    }

    public void adjustGridSize(){
        double maxCards = 0;
        if(monBox.getChildren().size()>maxCards){maxCards=monBox.getChildren().size();}
        if(tueBox.getChildren().size()>maxCards){maxCards=tueBox.getChildren().size();}
        if(wedBox.getChildren().size()>maxCards){maxCards=wedBox.getChildren().size();}
        if(thuBox.getChildren().size()>maxCards){maxCards=thuBox.getChildren().size();}
        if(friBox.getChildren().size()>maxCards){maxCards=friBox.getChildren().size();}
        if(satBox.getChildren().size()>maxCards){maxCards=satBox.getChildren().size();}
        if(sunBox.getChildren().size()>maxCards){maxCards=sunBox.getChildren().size();}
        shiftCardGrid.setPrefHeight(100*(maxCards+1));
    }
}