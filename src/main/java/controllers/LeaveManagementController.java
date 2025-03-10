package controllers;

import com.dlsc.gemsfx.FilterView;
import components.CustomDateStringConverter;
import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import models.*;
import org.controlsfx.control.PopOver;
import services.LeaveService;
import services.UserService;
import utils.AnimationUtils;
import utils.TableUtils;
import utils.ValidatorUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class LeaveManagementController extends DateSelectController {

    @FXML private TableView<LeaveRequest>  leaveTable;
    @FXML private TableColumn<?, ?>  employeeNameCol,employeeRoleCol, leaveTypeCol, fromCol, toCol, reasonCol;
    @FXML private VBox controlBox, editLeavePopover;
    @FXML private Label popoverLabel;
    @FXML private Button deleteButton;
    @FXML private Region contentDarken;
    @FXML private MFXFilterComboBox<User> employeeSelect;
    @FXML private MFXComboBox<String> leaveTypeCombo;
    @FXML private MFXDatePicker startDate, endDate;
    @FXML private MFXButton openStartTimePicker, openEndTimePicker;
    @FXML private MFXTextField startTimeField, endTimeField;
    @FXML private TextArea reasonField;
    @FXML private MFXButton saveButton;
    @FXML private Label employeeSelectValidationLabel, leaveTypeValidationLabel, startDateValidationLabel, endDateValidationLabel, startTimeValidationLabel, endTimeValidationLabel;
    private FilterView<LeaveRequest> leaveRequestFilterView;
    private PopOver currentTimePopover;
    private LocalTime startTime,endTime;
    private UserService userService;
    private LeaveService leaveService;

    @FXML
    private void initialize() {
        try{
            userService = new UserService();
            leaveService = new LeaveService();
        } catch (IOException ex) {
            dialogPane.showError("Error", "An error occurred while trying to create the user service",ex);
        }
    }

    public void fill() {
        try {
            List<User> currentUsers = userService.getAllUserEmployments(main.getCurrentStore().getStoreID());
            for(User u:currentUsers){
                employeeSelect.getItems().add(u);
            }
        } catch (Exception ex) {
            dialogPane.showError("Error", "An error occurred while trying to fetch employees",ex);
        }
        leaveTypeCombo.getItems().addAll("Sick Leave", "Annual Leave", "Unpaid Leave", "Maternity Leave", "Paternity Leave", "Bereavement Leave","Public holiday not worked", "Other");
        openStartTimePicker.setOnAction(_ -> {
            if(!startTimeField.getText().isEmpty()){startTime = LocalTime.parse(startTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
            else {startTime = LocalTime.MIDNIGHT;}
            openTimePicker(startTimeField,startTime);
        });
        openEndTimePicker.setOnAction(_ -> {
            if(!endTimeField.getText().isEmpty()) {endTime = LocalTime.parse(endTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
            else{endTime = LocalTime.MIDNIGHT;}
            openTimePicker(endTimeField,endTime);
        });
        controlBox.getChildren().clear();
        leaveRequestFilterView = new FilterView<>();
        leaveRequestFilterView.setTitle("Leave Requests this month");
        leaveRequestFilterView.setTextFilterProvider(text -> leaveRequest -> leaveRequest.getEmployeeName().toLowerCase().contains(text) || leaveRequest.getLeaveType().toLowerCase().contains(text));
        ObservableList<LeaveRequest> leaveRequests = leaveRequestFilterView.getFilteredItems();
        employeeNameCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        employeeRoleCol.setCellValueFactory(new PropertyValueFactory<>("employeeRole"));
        leaveTypeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        fromCol.setCellValueFactory(new PropertyValueFactory<>("fromDateString"));
        toCol.setCellValueFactory(new PropertyValueFactory<>("toDateString"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("leaveReason"));
        TableUtils.resizeTableColumns(leaveTable,reasonCol);
        leaveRequestFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
        controlBox.getChildren().addAll(leaveRequestFilterView,leaveTable);
        leaveTable.setItems(leaveRequests);
        Platform.runLater(this::addDoubleClickFunction);
        ValidatorUtils.setupRegexValidation(employeeSelect,employeeSelectValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(leaveTypeCombo,leaveTypeValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(startDate,startDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(endDate,endDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(startTimeField,startTimeValidationLabel,ValidatorUtils.TIME_REGEX,ValidatorUtils.TIME_ERROR,null,saveButton);
        ValidatorUtils.setupRegexValidation(endTimeField,endTimeValidationLabel,ValidatorUtils.TIME_REGEX,ValidatorUtils.TIME_ERROR,null,saveButton);
        startDate.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
        endDate.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
        setDate(main.getCurrentDate());
    }

    private void addDoubleClickFunction(){
        leaveTable.setRowFactory(_ -> {
            TableRow<LeaveRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    LeaveRequest rowData = row.getItem();
                    openPopover(rowData);
                }
            });
            return row ;
        });
    }

    public void fillTable(){
        List<LeaveRequest> currentLeaveRequests = FXCollections.observableArrayList();
        YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
        LocalDate endOfMonth = yearMonthObject.atEndOfMonth();
        LocalDate startOfMonth = yearMonthObject.atDay(1);
        try {
            currentLeaveRequests = leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), startOfMonth, endOfMonth);
        } catch (Exception ex) {
            dialogPane.showError("Error", "An error occurred while trying to fetch leave requests",ex);
        }
        leaveRequestFilterView.getItems().setAll(currentLeaveRequests);
        addDoubleClickFunction();
    }

    public void openPopover(){
        popoverLabel.setText("Add a new Leave Request");
        deleteButton.setVisible(false);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editLeavePopover,0);
        employeeSelect.setValue(null);
        employeeSelect.setText("");
        employeeSelect.clearSelection();
        leaveTypeCombo.setValue(null);
        leaveTypeCombo.setText("");
        leaveTypeCombo.clearSelection();
        startDate.setValue(null);
        startTimeField.setText("");
        endDate.setValue(null);
        endTimeField.setText("");
        reasonField.setText("");
        saveButton.setOnAction(_ -> addLeaveRequest());
    }

    public void openPopover(LeaveRequest leaveRequest){
        popoverLabel.setText("Edit Leave Request");
        deleteButton.setVisible(true);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editLeavePopover,0);
        try {
            employeeSelect.setValue(userService.getUserByID(leaveRequest.getUserID()));
        } catch (Exception ex) {
            dialogPane.showError("Error", "An error occurred while trying to find this employee",ex);
        }
        leaveTypeCombo.setValue(leaveRequest.getLeaveType());
        startDate.setValue(leaveRequest.getFromDate().toLocalDate());
        startTimeField.setText(leaveRequest.getFromDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        endDate.setValue(leaveRequest.getToDate().toLocalDate());
        endTimeField.setText(leaveRequest.getToDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        reasonField.setText(leaveRequest.getLeaveReason());
        saveButton.setOnAction(_ -> editLeaveRequest(leaveRequest));
        deleteButton.setOnAction(_ -> deleteLeaveRequest(leaveRequest));
    }

    public void closePopover(){
        AnimationUtils.slideIn(editLeavePopover,425);
        contentDarken.setVisible(false);
        employeeSelectValidationLabel.setVisible(false);
        leaveTypeValidationLabel.setVisible(false);
        startDateValidationLabel.setVisible(false);
        endDateValidationLabel.setVisible(false);
        startTimeValidationLabel.setVisible(false);
        endTimeValidationLabel.setVisible(false);
    }

    public void addLeaveRequest(){
        if(!employeeSelect.isValid()){employeeSelect.requestFocus();}
        else if(!leaveTypeCombo.isValid()){leaveTypeCombo.requestFocus();}
        else if(!startDate.isValid()){startDate.requestFocus();}
        else if(!endDate.isValid()){endDate.requestFocus();}
        else if(!startTimeField.isValid()){startTimeField.requestFocus();}
        else if(!endTimeField.isValid()){endTimeField.requestFocus();}
        else {
            LocalDateTime startDateTime = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
            LocalDateTime endDateTime = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
            if (startDateTime.isAfter(endDateTime)) {
                startDateValidationLabel.setVisible(true);
                startDateValidationLabel.setText("Start of leave must be before end of leave");
                startDate.requestFocus();
            } else {
                startDateValidationLabel.setVisible(false);
                LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                try {
                    LeaveRequest leaveRequest = new LeaveRequest();
                    leaveRequest.setUserID(employeeSelect.getValue().getUserID());
                    leaveRequest.setStoreID(main.getCurrentStore().getStoreID());
                    leaveRequest.setLeaveType(leaveTypeCombo.getValue());
                    leaveRequest.setFromDate(startDate.getValue().atTime(startTime));
                    leaveRequest.setToDate(endDate.getValue().atTime(endTime));
                    leaveRequest.setLeaveReason(reasonField.getText());
                    leaveService.addLeaveRequest(leaveRequest);
                } catch (Exception ex) {
                    dialogPane.showError("Error", "An error occurred while trying to add the leave request",ex);
                }
                employeeSelect.setValue(null);
                employeeSelect.setText("");
                employeeSelect.clearSelection();
                startDate.setValue(null);
                startTimeField.setText("");
                endDate.setValue(null);
                endTimeField.setText("");
                reasonField.setText("");
                fillTable();
                dialogPane.showInformation("Success", "Leave request successfully added");
            }
        }
    }

    public void editLeaveRequest(LeaveRequest leaveRequest){
        if(!employeeSelect.isValid()){employeeSelect.requestFocus();}
        else if(!leaveTypeCombo.isValid()){leaveTypeCombo.requestFocus();}
        else if(!startDate.isValid()){startDate.requestFocus();}
        else if(!endDate.isValid()){endDate.requestFocus();}
        else if(!startTimeField.isValid()){startTimeField.requestFocus();}
        else if(!endTimeField.isValid()){endTimeField.requestFocus();}
        else {
            LocalDateTime startDateTime = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
            LocalDateTime endDateTime = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
            if (startDateTime.isAfter(endDateTime)) {
                startDateValidationLabel.setVisible(true);
                startDateValidationLabel.setText("Start of leave must be before end of leave");
                startDate.requestFocus();
            } else {
                startDateValidationLabel.setVisible(false);
                LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
                try {
                    leaveRequest.setUserID(employeeSelect.getValue().getUserID());
                    leaveRequest.setLeaveType(leaveTypeCombo.getValue());
                    leaveRequest.setFromDate(startDate.getValue().atTime(startTime));
                    leaveRequest.setToDate(endDate.getValue().atTime(endTime));
                    leaveRequest.setLeaveReason(reasonField.getText());
                    leaveService.updateLeaveRequest(leaveRequest);
                } catch (Exception ex) {
                    dialogPane.showError("Error", "An error occurred while trying to update the leave request",ex);
                }
                closePopover();
                fillTable();
                dialogPane.showInformation("Success", "Leave request successfully edited");
            }
        }
    }

    public void deleteLeaveRequest(LeaveRequest leaveRequest){
        dialogPane.showWarning("Confirm Delete",
                "This action will permanently delete this Leave Request from all systems,\n" +
                        "Are you sure you still want to delete this Leave Request?").thenAccept(buttonType -> {
            if (buttonType.equals(ButtonType.OK)) {
                try {
                    leaveService.deleteLeaveRequest(leaveRequest.getLeaveID());
                } catch (Exception ex) {
                    dialogPane.showError("Error", "An error occurred while trying to delete the leave request",ex);
                }
                closePopover();
                fillTable();
                dialogPane.showInformation("Success","Leave request successfully deleted");
            }
        });
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
                dialogPane.showError("Error", "An error occurred while trying to load the time picker",ex);
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

    public void returnToRoster() {
        MainMenuController m = (MainMenuController) main.getController();
        m.changePage("/views/FXML/RosterPage.fxml");
    }

    @Override
    public void setDate(LocalDate date) {
        main.setCurrentDate(date);
        updateMonthSelectorField();
        fillTable();
    }
}
