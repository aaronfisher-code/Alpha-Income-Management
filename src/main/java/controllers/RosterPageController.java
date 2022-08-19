package controllers;


import application.Main;
//import com.jfoenix.controls.JFXDatePicker;
import com.dlsc.gemsfx.DialogPane;
import com.jfoenix.controls.JFXNodesList;
//import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.util.Duration;
import models.AccountPayment;
import models.Shift;
import models.User;
import org.controlsfx.control.PopOver;

import javax.swing.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Locale;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;
import static java.time.temporal.ChronoUnit.DAYS;


public class RosterPageController extends Controller {

    private MFXDatePicker datePkr;
    @FXML
    private MFXDatePicker startDate;
    @FXML
    private VBox monBox, tueBox, wedBox, thuBox, friBox, satBox, sunBox, editShiftPopover;
    @FXML
    private GridPane weekdayBox;
    @FXML
    private JFXNodesList addList;
    @FXML
    private GridPane shiftCardGrid;
    @FXML
    private FlowPane datePickerPane;
    @FXML
    private Region contentDarken;
    @FXML
    private StackPane startTimePicker;
    @FXML
    private MFXTextField startTimeField,endTimeField,repeatValue,thirtyMinBreaks,tenMinBreaks;
    @FXML
    private Button openStartTimePicker,openEndTimePicker;
    @FXML
    private MFXFilterComboBox employeeSelect;
    @FXML
    private MFXToggleButton repeatingShiftToggle;
    @FXML
    private Label repeatLabel;
    @FXML
    private MFXComboBox repeatUnit;
    @FXML
    private MFXButton saveButton;
    @FXML
    private DialogPane dialogPane;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private PopOver currentTimePopover;
    private LocalTime startTime,endTime;
    private DialogPane.Dialog<Object> dialog;

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
        datePkr = new MFXDatePicker();
        datePkr.setOnAction(e -> updatePage());
        datePickerPane.getChildren().add(1,datePkr);
        datePkr.setValue(LocalDate.now());
        datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
        datePkr.getStyleClass().add("custDatePicker");
        datePkr.getStylesheets().add("/views/CSS/RosterPage.css");
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



        //TODO fix dateTime parsing from strings on till computer
        openStartTimePicker.setOnAction(actionEvent -> {
            if(!startTimeField.getText().isEmpty()){startTime = LocalTime.parse(startTimeField.getText(), DateTimeFormatter.ofPattern("h:m a"));}
            else {startTime = LocalTime.MIDNIGHT;}
            openTimePicker(startTimeField,startTime);
        });
        openEndTimePicker.setOnAction(actionEvent -> {
            if(!endTimeField.getText().isEmpty()) {endTime = LocalTime.parse(endTimeField.getText(), DateTimeFormatter.ofPattern("h:m a"));}
            else{endTime = LocalTime.MIDNIGHT;}
            openTimePicker(endTimeField,endTime);
        });
        repeatUnit.getItems().add("Days");
        repeatUnit.getItems().add("Weeks");
        repeatingShiftToggle.setMainColor(Color.web("#0F60FF"));
        repeatingShiftToggle.setOnAction(event -> {
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
        updatePage();
    }

    public void updateDay(LocalDate date, VBox shiftContainer, int dayOfWeek, ArrayList<Shift> allShifts) {
        shiftContainer.getChildren().removeAll(shiftContainer.getChildren());

        long weekDay = date.getDayOfWeek().getValue();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/RosterDayCard.fxml"));
        VBox rosterDayCard = null;
        try {
            rosterDayCard = loader.load();
        } catch (IOException e) {

            e.printStackTrace();
        }
        RosterDayCardController rdc = loader.getController();
        rdc.setMain(main);
        rdc.setConnection(con);
        rdc.setDate(date.minusDays(weekDay - dayOfWeek));
        rdc.setParent(this);
        rdc.fill();
        if (rdc.getDate() == date) {
            rdc.select();
        }
        HBox.setHgrow(rosterDayCard, Priority.ALWAYS);
        weekdayBox.add(rosterDayCard,dayOfWeek-1,0);



        for (Shift s : allShifts) {
            boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) >= 0);
            boolean equalDay = s.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek));
            boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(date.minusDays(weekDay - dayOfWeek));
            if ((equalDay || repeatShiftDay) && !pastEnd) {
                loader = new FXMLLoader(getClass().getResource("/views/FXML/ShiftCard.fxml"));
                StackPane shiftCard = null;
                try {
                    shiftCard = loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ShiftCardController sc = loader.getController();
                sc.setMain(main);
                sc.setConnection(con);
                sc.setShift(s);
                sc.setDate(date.minusDays(weekDay - dayOfWeek));
                sc.setParent(this);
                sc.fill();
                sc.checkForLeaveFormat();
                shiftCard.setOnMouseClicked(event -> openPopover(s,sc.getDate()));
                shiftContainer.getChildren().add(shiftCard);
            }
        }
    }

    public void updatePage() {
        ArrayList<Shift> allShifts = new ArrayList<>();
        String sql = "SELECT * FROM shifts JOIN accounts a on a.username = shifts.username ORDER BY shiftStartTime, a.first_name";
        try {
            preparedStatement = con.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allShifts.add(new Shift(resultSet));
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        if (datePkr.getValue() == null)
            datePkr.setValue(LocalDate.now());

        weekdayBox.getChildren().removeAll(weekdayBox.getChildren());
        updateDay(datePkr.getValue(), monBox, 1, allShifts);
        updateDay(datePkr.getValue(), tueBox, 2, allShifts);
        updateDay(datePkr.getValue(), wedBox, 3, allShifts);
        updateDay(datePkr.getValue(), thuBox, 4, allShifts);
        updateDay(datePkr.getValue(), friBox, 5, allShifts);
        updateDay(datePkr.getValue(), satBox, 6, allShifts);
        updateDay(datePkr.getValue(), sunBox, 7, allShifts);

        adjustGridSize();
    }

    public void weekForward() {
        setDatePkr(datePkr.getValue().plusWeeks(1));
    }

    public void weekBackward() {
        setDatePkr(datePkr.getValue().minusWeeks(1));
    }

    public void setDatePkr(LocalDate date) {
        datePkr.setValue(date);
        updatePage();
    }

    public void openPopover(){
        contentDarken.setVisible(true);
        changeSize(editShiftPopover,0);
        employeeSelect.setValue(null);
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
        repeatUnit.setValue(null);
        saveButton.setOnAction(actionEvent -> addShift(null));
    }

    public void openPopover(Shift s,LocalDate shiftCardDate){
        contentDarken.setVisible(true);
        changeSize(editShiftPopover,0);
        String sql = "SELECT * FROM accounts WHERE username = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, s.getUsername());
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next())
                employeeSelect.setValue(new User(resultSet));
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        startDate.setValue(s.getShiftStartDate());
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
            repeatUnit.setValue("Days");
        }

        saveButton.setOnAction(actionEvent -> {
            if(s.isRepeating()){
                dialog = new DialogPane.Dialog(dialogPane, BLANK);
                dialog.setPadding(false);
                dialog.setContent(createAddNewContactDialog(s,shiftCardDate));
                dialogPane.showDialog(dialog);
            }else{
                editShift(s);
            }
        });
    }

    public void closePopover(){
        changeSize(editShiftPopover,375);
        contentDarken.setVisible(false);
    }

    public void changeSize(final VBox pane, double width) {
        Duration cycleDuration = Duration.millis(200);
        Timeline timeline = new Timeline(
                new KeyFrame(cycleDuration,
                        new KeyValue(pane.translateXProperty(),width, Interpolator.EASE_BOTH))
        );
        timeline.play();
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
            rdc.setParent(this);
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

    public void addShift(LocalDate manualStartDate){
        String usrname = ((User) employeeSelect.getValue()).getUsername();
        LocalDate sDate = manualStartDate==null?startDate.getValue():manualStartDate;
        LocalTime sTime = LocalTime.parse(startTimeField.getText(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime eTime = LocalTime.parse(endTimeField.getText(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        if (!thirtyMinBreaks.getText().equals("")) {thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());}
        if (!tenMinBreaks.getText().equals("")) {tenMin = Integer.parseInt(tenMinBreaks.getText());}
        if(repeatingShiftToggle.isSelected()){
            int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        }

        String sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat) VALUES(?,?,?,?,?,?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, usrname);
            preparedStatement.setTime(2, Time.valueOf(sTime));
            preparedStatement.setTime(3, Time.valueOf(eTime));
            preparedStatement.setDate(4, Date.valueOf(sDate));
            preparedStatement.setInt(5, thirtyMin);
            preparedStatement.setInt(6, tenMin);
            preparedStatement.setBoolean(7, repeatingShiftToggle.isSelected());
            preparedStatement.setInt(8, daysPerRepeat);
            preparedStatement.executeUpdate();
            if(manualStartDate!=null) {
                updatePage();
                dialogPane.showInformation("Success", "Shift created succesfully");
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void editShift(Shift s){
        String usrname = ((User) employeeSelect.getValue()).getUsername();
        LocalDate sDate = startDate.getValue();
        LocalTime sTime = LocalTime.parse(startTimeField.getText(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime eTime = LocalTime.parse(endTimeField.getText(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        if (!thirtyMinBreaks.getText().equals("")) {thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());}
        if (!tenMinBreaks.getText().equals("")) {tenMin = Integer.parseInt(tenMinBreaks.getText());}
        if(repeatingShiftToggle.isSelected()){
            int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        }

        String sql = "UPDATE shifts SET username=?,shiftStartTime=?,shiftEndTime=?,shiftStartDate=?,thirtyMinBreaks=?,tenMinBreaks=?,repeating=?,daysPerRepeat=? WHERE shift_id=?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, usrname);
            preparedStatement.setTime(2, Time.valueOf(sTime));
            preparedStatement.setTime(3, Time.valueOf(eTime));
            preparedStatement.setDate(4, Date.valueOf(sDate));
            preparedStatement.setInt(5, thirtyMin);
            preparedStatement.setInt(6, tenMin);
            preparedStatement.setBoolean(7, repeatingShiftToggle.isSelected());
            preparedStatement.setInt(8, daysPerRepeat);
            preparedStatement.setInt(9, s.getShiftID());
            preparedStatement.executeUpdate();
            updatePage();
            dialogPane.showInformation("Success", "Shift edited succesfully");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void editFutureShifts(Shift s,LocalDate shiftCardDate){
        //end original shift
        String sql = "UPDATE shifts SET shiftEndDate=? WHERE shift_id=?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, Date.valueOf(shiftCardDate.minusDays(1)));
            preparedStatement.setInt(2,s.getShiftID());
            preparedStatement.executeUpdate();
            //start new shift with new data
            addShift(shiftCardDate);
            updatePage();
            dialogPane.showInformation("Success", "Shift edited succesfully");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

    }

    public void createShiftModification(Shift s,LocalDate shiftCardDate){

    }

    private Node createAddNewContactDialog(Shift s,LocalDate shiftCardDate) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/CalendarEditDialog.fxml"));
        StackPane calendarEditDialog = null;
        try {
            calendarEditDialog = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CalendarEditDialogController dialogController = loader.getController();
        dialogController.fill(s,shiftCardDate);
        dialogController.setParent(this);
        dialogController.setConnection(this.con);
        return calendarEditDialog;
    }

    public void addNewLeave() throws IOException {
        Stage leaveEditStage = new Stage();
        EditLeaveController c;
        leaveEditStage.setResizable(false);
        leaveEditStage.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/LeaveEdit.fxml"));
        Parent root = loader.load();
        c = loader.getController();
        c.setMain(main);
        c.setConnection(con);
        c.fill();
        c.setParent(this);
        leaveEditStage.setTitle("Add a new Leave Request");
        leaveEditStage.setScene(new Scene(root));
        leaveEditStage.showAndWait();
    }

    public void exportData() throws IOException {
        Stage exportToolStage = new Stage();
        ExportToolController c;
        exportToolStage.setResizable(false);
        exportToolStage.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ExportTool.fxml"));
        Parent root = loader.load();
        c = loader.getController();
        c.setMain(main);
        c.setConnection(con);
        c.fill();
        c.setParent(this);
        exportToolStage.setTitle("Export roster info to clipboard");
        exportToolStage.setScene(new Scene(root));
        exportToolStage.showAndWait();
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

