package controllers;

import application.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class RosterDayCardController extends Controller {

    @FXML
    private Label weekdayLbl, dateLbl, eventLbl;
    @FXML
    private Region selectionHighlight;

    private Main main;
    private Connection con;
    private LocalDate date;
    private RosterPageController parent;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private boolean selected = false;

    public RosterDayCardController() {
    }

    @Override
    public void setMain(Main newMain) {
        this.main = newMain;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void setDate(LocalDate d) {
        this.date = d;
    }

    public void setParent(RosterPageController parent) {
        this.parent = parent;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public void fill() {
        weekdayLbl.setText(String.valueOf(date.getDayOfWeek()));
        dateLbl.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth());
        String sql = "SELECT * FROM specialDates Where eventDate = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, date.toString());
            resultSet = preparedStatement.executeQuery();
            if (!(resultSet == null || !resultSet.next())) {
                eventLbl.setText(resultSet.getString("note"));
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void editDay() {
        parent.setDatePkr(date);
        Stage dayEditStage = new Stage();
        EditRosterDayController c;
        dayEditStage.setResizable(false);
        dayEditStage.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/RosterDayEdit.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        c = loader.getController();
        c.setMain(main);
        c.setConnection(con);
        c.setDate(date);
        c.setParent(parent);
        c.fill();
        dayEditStage.setTitle("Edit Details for " + weekdayLbl.getText() + " " + dateLbl.getText());
        dayEditStage.setScene(new Scene(root));
        dayEditStage.showAndWait();
    }

    public void singleClick(){
        if(!selected)
            parent.setDatePkr(date);
        else {
            editDay();
        }
    }

    public void select() {
        selectionHighlight.setStyle("-fx-background-color: #0F60FF;");
        selected = true;
    }

    public void deSelect() {
        selectionHighlight.setStyle("-fx-background-color: #FFFFFF;");
    }
}
