package controllers;


import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXNodesList;
import components.CustomDateStringConverter;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.base.MFXCombo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.*;
import org.controlsfx.control.PopOver;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.TableUtils;
import utils.ValidatorUtils;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;
import static java.time.temporal.ChronoUnit.DAYS;


public class LeaveManagementController extends DateSelectController {

    @FXML
    private StackPane monthSelector;
    @FXML
    private MFXTextField monthSelectorField;

    private PopOver currentDatePopover;

    private PopOver currentTimePopover;

    @FXML
    private TableView<LeaveRequest>  leaveTable;

    @FXML
    private TableColumn<?, ?>  employeeNameCol,employeeRoleCol, leaveTypeCol, fromCol, toCol, reasonCol;

    @FXML
    private VBox controlBox, editLeavePopover;

    @FXML
    private Label popoverLabel;

    @FXML
    private Button deleteButton, closeButton;

    @FXML
    private Region contentDarken;

    @FXML
    private MFXFilterComboBox employeeSelect;

    @FXML
    private MFXComboBox<String> leaveTypeCombo;

    @FXML
    private MFXDatePicker startDate, endDate;

    @FXML
    private MFXButton openStartTimePicker, openEndTimePicker;

    @FXML
    private MFXTextField startTimeField, endTimeField;

    @FXML
    private TextArea reasonField;

    @FXML
    private MFXButton saveButton;

    @FXML
    private DialogPane dialogPane;

    @FXML
    private Label employeeSelectValidationLabel, leaveTypeValidationLabel, startDateValidationLabel, endDateValidationLabel, startTimeValidationLabel, endTimeValidationLabel;

    private FilterView<LeaveRequest> leaveRequestFilterView;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private DialogPane.Dialog<Object> dialog;

    private LocalTime startTime,endTime;

    private ObservableList<LeaveRequest> leaveRequests = FXCollections.observableArrayList();

    public void setMain(Main main) {
        this.main = main;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public DialogPane.Dialog<Object> getDialog() {
        return dialog;
    }

    public void fill() {
        ObservableList<User> currentUsers = FXCollections.observableArrayList();
        String sql = "SELECT * FROM accounts JOIN employments e on accounts.username = e.username WHERE storeID = ? AND inactiveDate IS NULL ";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                currentUsers.add(new User(resultSet));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        for(User u:currentUsers){
            employeeSelect.getItems().add(u);
        }

        leaveTypeCombo.getItems().addAll("Sick Leave", "Annual Leave", "Unpaid Leave", "Maternity Leave", "Paternity Leave", "Bereavement Leave", "Other");


        openStartTimePicker.setOnAction(actionEvent -> {
            if(!startTimeField.getText().isEmpty()){startTime = LocalTime.parse(startTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
            else {startTime = LocalTime.MIDNIGHT;}
            openTimePicker(startTimeField,startTime);
        });

        openEndTimePicker.setOnAction(actionEvent -> {
            if(!endTimeField.getText().isEmpty()) {endTime = LocalTime.parse(endTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
            else{endTime = LocalTime.MIDNIGHT;}
            openTimePicker(endTimeField,endTime);
        });

        controlBox.getChildren().clear();
        leaveRequestFilterView = new FilterView<>();
        leaveRequestFilterView.setTitle("Leave Requests this month");
        leaveRequestFilterView.setTextFilterProvider(text -> leaveRequest -> leaveRequest.getEmployeeName().toLowerCase().contains(text) || leaveRequest.getLeaveType().toLowerCase().contains(text));
        leaveRequests = leaveRequestFilterView.getFilteredItems();

        employeeNameCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        employeeRoleCol.setCellValueFactory(new PropertyValueFactory<>("employeeRole"));
        leaveTypeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        fromCol.setCellValueFactory(new PropertyValueFactory<>("fromDateString"));
        toCol.setCellValueFactory(new PropertyValueFactory<>("toDateString"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("leaveReason"));

        leaveTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        leaveTable.setMaxWidth(Double.MAX_VALUE);
        leaveTable.setMaxHeight(Double.MAX_VALUE);
        leaveRequestFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
        controlBox.getChildren().addAll(leaveRequestFilterView,leaveTable);
        leaveTable.setFixedCellSize(25.0);
        VBox.setVgrow(leaveTable, Priority.ALWAYS);
        leaveTable.setItems(leaveRequests);
        for(TableColumn tc: leaveTable.getColumns()){
            tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
        }
        Platform.runLater(() -> GUIUtils.customResize(leaveTable,reasonCol));
        Platform.runLater(() -> addDoubleClickfunction());
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

    private void addDoubleClickfunction(){
        leaveTable.setRowFactory( tv -> {
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
        ObservableList<LeaveRequest> currentLeaveRequests = FXCollections.observableArrayList();
        YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
        LocalDate endOfMonth = yearMonthObject.atEndOfMonth();
        LocalDate startOfMonth = yearMonthObject.atDay(1);
        String sql;
        try {
            sql = "SELECT * FROM leaveRequests JOIN accounts a on a.username = leaverequests.employeeID where storeID = ? AND leaveStartDate <= ? AND leaveEndDate >= ?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setDate(2, Date.valueOf(endOfMonth));
            preparedStatement.setDate(3, Date.valueOf(startOfMonth));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                currentLeaveRequests.add(new LeaveRequest(resultSet));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        leaveRequestFilterView.getItems().setAll(currentLeaveRequests);
        addDoubleClickfunction();
    }

    public void openPopover(){
        popoverLabel.setText("Add a new Leave Request");
        deleteButton.setVisible(false);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editLeavePopover,0);
        employeeSelect.setValue(null);
        leaveTypeCombo.setValue(null);
        startDate.setValue(null);
        startTimeField.setText("");
        endDate.setValue(null);
        endTimeField.setText("");
        reasonField.setText("");
        saveButton.setOnAction(actionEvent -> addLeaveRequest());
    }

    public void openPopover(LeaveRequest leaveRequest){
        popoverLabel.setText("Edit Leave Request");
        deleteButton.setVisible(true);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editLeavePopover,0);
        String sql = "SELECT * FROM accounts WHERE username = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, leaveRequest.getEmployeeID());
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next())
                employeeSelect.setValue(new User(resultSet));
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        leaveTypeCombo.setValue(leaveRequest.getLeaveType());
        startDate.setValue(leaveRequest.getFromDate().toLocalDate());
        startTimeField.setText(leaveRequest.getFromDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        endDate.setValue(leaveRequest.getToDate().toLocalDate());
        endTimeField.setText(leaveRequest.getToDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        reasonField.setText(leaveRequest.getLeaveReason());
        saveButton.setOnAction(actionEvent -> editLeaveRequest(leaveRequest));
        deleteButton.setOnAction(actionEvent -> deleteLeaveRequest(leaveRequest));
    }

    public void closePopover(){
        AnimationUtils.slideIn(editLeavePopover,425);
        contentDarken.setVisible(false);
    }

    public void addLeaveRequest(){
        User employee = (User) employeeSelect.getValue();
        String leaveType = leaveTypeCombo.getValue();
        LocalDate fromDate = startDate.getValue();
        LocalDate toDate = endDate.getValue();
        LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        String reason = reasonField.getText();
        String sql = "INSERT INTO leaverequests (employeeID, storeID, leaveType, leaveStartDate, leaveEndDate, reason) VALUES (?,?,?,?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, employee.getUsername());
            preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
            preparedStatement.setString(3, leaveType);
            preparedStatement.setTimestamp(4, Timestamp.valueOf(fromDate.atTime(startTime)));
            preparedStatement.setTimestamp(5, Timestamp.valueOf(toDate.atTime(endTime)));
            preparedStatement.setString(6, reason);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        closePopover();
        fillTable();
        dialogPane.showInformation("Success","Leave request succesfully added");
    }

    public void editLeaveRequest(LeaveRequest leaveRequest){
        User employee = (User) employeeSelect.getValue();
        String leaveType = leaveTypeCombo.getValue();
        LocalDate fromDate = startDate.getValue();
        LocalDate toDate = endDate.getValue();
        LocalTime startTime = LocalTime.parse(startTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime endTime = LocalTime.parse(endTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        String reason = reasonField.getText();
        String sql = "UPDATE leaverequests SET employeeID = ?, leaveType = ?, leaveStartDate = ?, leaveEndDate = ?, reason = ? WHERE leaveID = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, employee.getUsername());
            preparedStatement.setString(2, leaveType);
            preparedStatement.setTimestamp(3, Timestamp.valueOf(fromDate.atTime(startTime)));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(toDate.atTime(endTime)));
            preparedStatement.setString(5, reason);
            preparedStatement.setInt(6, leaveRequest.getLeaveID());
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        closePopover();
        fillTable();
        dialogPane.showInformation("Success","Leave request succesfully edited");
    }

    public void deleteLeaveRequest(LeaveRequest leaveRequest){
        dialogPane.showWarning("Confirm Delete",
                "This action will permanently delete this Leave Request from all systems,\n" +
                        "Are you sure you still want to delete this Leave Request?").thenAccept(buttonType -> {
            if (buttonType.equals(ButtonType.OK)) {
                String sql = "DELETE FROM leaverequests WHERE leaveID = ?";
                try {
                    preparedStatement = con.prepareStatement(sql);
                    preparedStatement.setInt(1, leaveRequest.getLeaveID());
                    preparedStatement.executeUpdate();
                } catch (SQLException ex) {
                    System.err.println(ex.getMessage());
                }
                closePopover();
                fillTable();
                dialogPane.showInformation("Success","Leave request succesfully deleted");
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            TimePickerContentController rdc = loader.getController();
            rdc.setMain(main);
            rdc.setConnection(con);
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
            timePickerMenu.setOnHidden(event -> {
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

    public void monthForward() {
        setDate(main.getCurrentDate().plusMonths(1));
    }

    public void monthBackward() {
        setDate(main.getCurrentDate().minusMonths(1));
    }

    @Override
    public void setDate(LocalDate date) {
        main.setCurrentDate(date);
        String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        fieldText += ", ";
        fieldText += main.getCurrentDate().getYear();
        monthSelectorField.setText(fieldText);
        fillTable();
    }

    public void openMonthSelector(){
        if(currentDatePopover!=null&&currentDatePopover.isShowing()){
            currentDatePopover.hide();
        }else {
            PopOver monthSelectorMenu = new PopOver();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/MonthYearSelectorContent.fxml"));
            VBox monthSelectorMenuContent = null;
            try {
                monthSelectorMenuContent = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            MonthYearSelectorContentController rdc = loader.getController();
            rdc.setMain(main);
            rdc.setConnection(con);
            rdc.setParent(this);
            rdc.fill();

            monthSelectorMenu.setOpacity(1);
            monthSelectorMenu.setContentNode(monthSelectorMenuContent);
            monthSelectorMenu.setArrowSize(0);
            monthSelectorMenu.setAnimated(true);
            monthSelectorMenu.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
            monthSelectorMenu.setAutoHide(true);
            monthSelectorMenu.setDetachable(false);
            monthSelectorMenu.setHideOnEscape(true);
            monthSelectorMenu.setCornerRadius(10);
            monthSelectorMenu.setArrowIndent(0);
            monthSelectorMenu.show(monthSelector);
            currentDatePopover=monthSelectorMenu;
            monthSelectorField.requestFocus();
        }
    }
}

