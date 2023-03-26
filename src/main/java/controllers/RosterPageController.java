package controllers;


import application.Main;
//import com.jfoenix.controls.JFXDatePicker;
import com.dlsc.gemsfx.DialogPane;
import com.jfoenix.controls.JFXNodesList;
//import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.*;
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
import models.Shift;
import models.User;
import org.controlsfx.control.PopOver;
import utils.AnimationUtils;

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
    private Label popoverLabel;
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
    private Button openStartTimePicker,openEndTimePicker,deleteButton;
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
            if(!startTimeField.getText().isEmpty()){startTime = LocalTime.parse(startTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
            else {startTime = LocalTime.MIDNIGHT;}
            openTimePicker(startTimeField,startTime);
        });
        openEndTimePicker.setOnAction(actionEvent -> {
            if(!endTimeField.getText().isEmpty()) {endTime = LocalTime.parse(endTimeField.getText().toLowerCase(), DateTimeFormatter.ofPattern("h:mm a"));}
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

    public void updateDay(LocalDate date, VBox shiftContainer, int dayOfWeek, ArrayList<Shift> allShifts,ArrayList<Shift> allModifications) {
        //empty the contents of the current day VBox
        shiftContainer.getChildren().removeAll(shiftContainer.getChildren());
        long weekDay = date.getDayOfWeek().getValue();

        //Create Day Header card
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
            //add blue selection formatting if date is current
            rdc.select();
        }
        HBox.setHgrow(rosterDayCard, Priority.ALWAYS);
        weekdayBox.add(rosterDayCard,dayOfWeek-1,0);
        for (Shift s : allShifts) {
            boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), date.minusDays(weekDay - dayOfWeek)) >= 0);
            boolean equalDay = s.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek));
            boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(date.minusDays(weekDay - dayOfWeek));
            if ((equalDay || repeatShiftDay) && !pastEnd) {
                Shift updatedShift = s;
                boolean shiftIsModified = false;
                for(Shift m: allModifications){
                    if(m.getShiftID()==s.getShiftID() && m.getOriginalDate().equals(date.minusDays(weekDay - dayOfWeek))){
                        System.out.println("Modified Shift: "+m.getShiftID());
                        updatedShift = m;
                        shiftIsModified=true;
                    }
                }
                if(!shiftIsModified || (shiftIsModified&&updatedShift.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek)))){
                    try {
                        loader = new FXMLLoader(getClass().getResource("/views/FXML/ShiftCard.fxml"));
                        StackPane shiftCard = loader.load();
                        ShiftCardController sc = loader.getController();
                        sc.setMain(main);
                        sc.setConnection(con);
                        sc.setShift(updatedShift);
                        sc.setParent(this);
                        sc.fill();
                        sc.setDate(date.minusDays(weekDay - dayOfWeek));
                        if(shiftIsModified)
                            sc.showDifference(s,updatedShift);
                        Shift finalS = updatedShift;
                        shiftCard.setOnMouseClicked(event -> openPopover(finalS,sc.getDate()));
                        shiftContainer.getChildren().add(shiftCard);
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            }
        }

        for(Shift m: allModifications){
            if(m.getShiftStartDate().equals(date.minusDays(weekDay - dayOfWeek))&&(!(m.getShiftStartDate().equals(m.getOriginalDate())))){
                try {
                    loader = new FXMLLoader(getClass().getResource("/views/FXML/ShiftCard.fxml"));
                    StackPane shiftCard = loader.load();
                    ShiftCardController sc = loader.getController();
                    sc.setMain(main);
                    sc.setConnection(con);
                    sc.setShift(m);
                    sc.setParent(this);
                    sc.fill();
                    sc.setModification("test");
                    sc.setDate(date.minusDays(weekDay - dayOfWeek));
                    shiftCard.setOnMouseClicked(event -> openPopover(m,sc.getDate()));
                    shiftContainer.getChildren().add(shiftCard);
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }

    public void updatePage() {
        ArrayList<Shift> allShifts = new ArrayList<>();
        ArrayList<Shift> allModifications = new ArrayList<>();

        //Set date in case the value is still null
        if (datePkr.getValue() == null)
            datePkr.setValue(LocalDate.now());

        //Get Week start and End dates for search range
        long weekDay = datePkr.getValue().getDayOfWeek().getValue();
        LocalDate weekStart = datePkr.getValue().minusDays(weekDay-1);
        LocalDate weekEnd = datePkr.getValue().plusDays(7-weekDay);

        String sql = "SELECT * FROM shifts JOIN accounts a on a.username = shifts.username " +
                    "WHERE (shifts.repeating=TRUE AND (isNull(shiftEndDate) OR shiftEndDate>?) AND shiftStartDate<?)"+
                    "OR (shifts.repeating=false AND shiftStartDate>? AND shiftStartDate<?)"+
                    "ORDER BY shiftStartTime, a.first_name";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, Date.valueOf(weekStart));
            preparedStatement.setDate(2, Date.valueOf(weekEnd));
            preparedStatement.setDate(3, Date.valueOf(weekStart));
            preparedStatement.setDate(4, Date.valueOf(weekEnd));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allShifts.add(new Shift(resultSet));
            }

            sql = "SELECT * FROM shiftmodifications JOIN accounts a on a.username = shiftmodifications.username " +
                    "WHERE modificationID in (select max(modificationID) from shiftmodifications group by shift_id, originalDate) AND" +
                    "((shiftmodifications.shiftStartDate>? AND shiftmodifications.shiftStartDate<?) OR " +
                    "(shiftmodifications.originalDate>? AND shiftmodifications.originalDate<?))";

            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, Date.valueOf(weekStart));
            preparedStatement.setDate(2, Date.valueOf(weekEnd));
            preparedStatement.setDate(3, Date.valueOf(weekStart));
            preparedStatement.setDate(4, Date.valueOf(weekEnd));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allModifications.add(new Shift(resultSet));;
            }
            System.out.println(allModifications.size());
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        weekdayBox.getChildren().removeAll(weekdayBox.getChildren());
        updateDay(datePkr.getValue(), monBox, 1, allShifts,allModifications);
        updateDay(datePkr.getValue(), tueBox, 2, allShifts,allModifications);
        updateDay(datePkr.getValue(), wedBox, 3, allShifts,allModifications);
        updateDay(datePkr.getValue(), thuBox, 4, allShifts,allModifications);
        updateDay(datePkr.getValue(), friBox, 5, allShifts,allModifications);
        updateDay(datePkr.getValue(), satBox, 6, allShifts,allModifications);
        updateDay(datePkr.getValue(), sunBox, 7, allShifts,allModifications);

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
//        updatePage();
    }

    public void openPopover(){
        popoverLabel.setText("Add a new Shift");
        deleteButton.setVisible(false);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editShiftPopover,0);
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
        saveButton.setOnAction(actionEvent -> addShift(null,null,null));
    }

    public void openPopover(Shift s,LocalDate shiftCardDate){
        popoverLabel.setText("Edit shift");
        deleteButton.setVisible(true);
        contentDarken.setVisible(true);
        AnimationUtils.slideIn(editShiftPopover,0);
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
                dialog.setContent(createCalendarEditDialog(s,shiftCardDate));
                dialogPane.showDialog(dialog);
            }else{
                editShift(s);
            }
        });
    }

    public void closePopover(){
        AnimationUtils.slideIn(editShiftPopover,425);
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

    public void addShift(Shift previousShift, LocalDate manualStartDate, LocalDate originalShiftDate){
        System.out.println("Date being added as original: " + originalShiftDate);
        String usrname = ((User) employeeSelect.getValue()).getUsername();
        LocalDate sDate = manualStartDate==null?startDate.getValue():manualStartDate;
        LocalDate eDate = manualStartDate==null?startDate.getValue():manualStartDate;
        LocalTime sTime = LocalTime.parse(startTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime eTime = LocalTime.parse(endTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        if (!thirtyMinBreaks.getText().equals("")) {thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());}
        if (!tenMinBreaks.getText().equals("")) {tenMin = Integer.parseInt(tenMinBreaks.getText());}
        if(repeatingShiftToggle.isSelected()){
            int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        }
        try {
            String sql = "";
            if(previousShift!=null){
                sql = "INSERT INTO shiftModifications(username,shiftStartTime,shiftEndTime,shiftStartDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shift_id,originalDate) VALUES(?,?,?,?,?,?,?,?,?,?)";
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setInt(9, previousShift.getShiftID());
                preparedStatement.setDate(10, Date.valueOf(originalShiftDate));
            }else{
                sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat) VALUES(?,?,?,?,?,?,?,?)";
                preparedStatement = con.prepareStatement(sql);
            }
            preparedStatement.setString(1, usrname);
            preparedStatement.setTime(2, Time.valueOf(sTime));
            preparedStatement.setTime(3, Time.valueOf(eTime));
            preparedStatement.setDate(4, Date.valueOf(sDate));
            preparedStatement.setInt(5, thirtyMin);
            preparedStatement.setInt(6, tenMin);
            preparedStatement.setBoolean(7, repeatingShiftToggle.isSelected());
            preparedStatement.setInt(8, daysPerRepeat);
            preparedStatement.executeUpdate();
            updatePage();
            if(manualStartDate==null) {
                dialogPane.showInformation("Success", "Shift created succesfully");
            }else if(previousShift!=null){
                updatePage();
                dialogPane.showInformation("Success", "Shift edited succesfully");
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void editShift(Shift s){
        String usrname = ((User) employeeSelect.getValue()).getUsername();
        LocalDate sDate = startDate.getValue();
        LocalTime sTime = LocalTime.parse(startTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
        LocalTime eTime = LocalTime.parse(endTimeField.getText().toUpperCase(),DateTimeFormatter.ofPattern("h:mm a" , Locale.US ));
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
            addShift(null,shiftCardDate,null);
            updatePage();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void editCurrentShift(Shift s,LocalDate shiftCardDate){
        System.out.println("Original date on previous shift was null :" + s.getOriginalDate()==null);
        System.out.println(s.getOriginalDate());
        addShift(s,startDate.getValue(),s.getOriginalDate()==null?shiftCardDate:s.getOriginalDate());
        updatePage();
        dialogPane.showInformation("Success", "Shift edited succesfully");
    }

    private Node createCalendarEditDialog(Shift s,LocalDate shiftCardDate) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/CalendarEditDialog.fxml"));
        StackPane calendarEditDialog = null;
        try {
            calendarEditDialog = loader.load();

        } catch (IOException e) {
            e.printStackTrace();
        }
        CalendarEditDialogController dialogController = loader.getController();
        String sql = "SELECT * FROM shifts WHERE shift_id = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, s.getShiftID());
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next())
                s=new Shift(resultSet);
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
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

