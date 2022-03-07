package controllers;


import application.Main;
//import com.jfoenix.controls.JFXDatePicker;
import com.dlsc.gemsfx.TimePicker;
import com.jfoenix.controls.JFXNodesList;
//import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.font.FontResources;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Shift;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.concurrent.Flow;

import static java.time.temporal.ChronoUnit.DAYS;


public class RosterPageController extends Controller {

    private MFXDatePicker datePkr;
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


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;

    public void setMain(Main main) {
        this.main = main;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void fill() {
        datePkr = new MFXDatePicker();
        datePkr.setOnAction(e -> updatePage());
        datePickerPane.getChildren().add(1,datePkr);
        datePkr.setValue(LocalDate.now());
        datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
        datePkr.getStyleClass().add("custDatePicker");
        datePkr.getStylesheets().add("/views/CSS/RosterPage.css");
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

    public void addNewShift(){
        contentDarken.setVisible(true);
        changeSize(editShiftPopover,0);

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

