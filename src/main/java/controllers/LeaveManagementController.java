package controllers;


import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXNodesList;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private MFXFilterComboBox<String> employeeSelect;

    @FXML
    private MFXComboBox<String> leaveTypeCombo;

    @FXML
    private MFXDatePicker startDate, endDate;

    private FilterView<LeaveRequest> leaveRequestFilterView;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private DialogPane.Dialog<Object> dialog;

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
//        Platform.runLater(() -> addInvoiceDoubleClickfunction());
        fillTable();
//        plusButton.setOnAction(actionEvent -> openInvoicePopover());
//        contentDarken.setOnMouseClicked(actionEvent -> closeInvoicePopover());
        setDate(main.getCurrentDate());
    }

    public void fillTable(){
//		System.out.println("fill table method called");
        ObservableList<LeaveRequest> currentLeaveRequests = FXCollections.observableArrayList();
        YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
        String sql;
        try {
            sql = "SELECT * FROM leaveRequests JOIN accounts a on a.username = leaverequests.employeeID where storeID = ? AND month(leaveStartDate) = ? and YEAR(leaveStartDate) = ? "; //TODO: possibly filter for month/year?
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setInt(2, yearMonthObject.getMonthValue());
            preparedStatement.setInt(3, yearMonthObject.getYear());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                currentLeaveRequests.add(new LeaveRequest(resultSet));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        leaveRequestFilterView.getItems().setAll(currentLeaveRequests);
    }

    public void openPopover(){
        popoverLabel.setText("Add a new Shift");
        deleteButton.setVisible(false);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editLeavePopover,0);
        employeeSelect.setValue(null);
//        startDate.setValue(null);
//        startTimeField.setText("");
//        endTimeField.setText("");
//        thirtyMinBreaks.setText("");
//        tenMinBreaks.setText("");
//        repeatingShiftToggle.setSelected(false);
//        repeatValue.setDisable(true);
//        repeatUnit.setDisable(true);
//        repeatLabel.setDisable(true);
//        repeatValue.setText("");
//        repeatUnit.setValue(null);
//        saveButton.setOnAction(actionEvent -> addShift(null,null,null,false));
    }

    public void closePopover(){
        AnimationUtils.slideIn(editLeavePopover,425);
        contentDarken.setVisible(false);
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

