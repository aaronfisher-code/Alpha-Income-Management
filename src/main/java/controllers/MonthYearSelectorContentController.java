package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import models.Shift;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class MonthYearSelectorContentController extends Controller {

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Shift shift;
    private DateSelectController parent;

    @FXML
    private VBox buttonContainer;
    @FXML
    private MFXRectangleToggleNode janNode,febNode,marNode,aprNode,mayNode,junNode,julNode,augNode,sepNode,octNode,novNode,decNode;
    @FXML
    private Text yearValue;
    @FXML
    private MFXButton backwardButton,forwardButton;

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(DateSelectController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }


    @Override
    public void fill() {
        yearValue.setText(String.valueOf(main.getCurrentDate().getYear()));
        switch (main.getCurrentDate().getMonthValue()) {
            case 1: janNode.setSelected(true);break;
            case 2: febNode.setSelected(true);break;
            case 3: marNode.setSelected(true);break;
            case 4: aprNode.setSelected(true);break;
            case 5: mayNode.setSelected(true);break;
            case 6: junNode.setSelected(true);break;
            case 7: julNode.setSelected(true);break;
            case 8: augNode.setSelected(true);break;
            case 9: sepNode.setSelected(true);break;
            case 10: octNode.setSelected(true);break;
            case 11: novNode.setSelected(true);break;
            case 12: decNode.setSelected(true);break;
        }

        forwardButton.setOnAction(event -> {
            parent.setDate(main.getCurrentDate().plusYears(1));
            yearValue.setText(String.valueOf(main.getCurrentDate().getYear()));
        });
        backwardButton.setOnAction(event -> {
            parent.setDate(main.getCurrentDate().minusYears(1));
            yearValue.setText(String.valueOf(main.getCurrentDate().getYear()));
        });

        janNode.setOnAction(event -> {
            if(!janNode.isSelected()){janNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,1,day));
        });
        febNode.setOnAction(event -> {
            if(!febNode.isSelected()){febNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,2,day));
        });
        marNode.setOnAction(event -> {
            if(!marNode.isSelected()){marNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,3,day));
        });
        aprNode.setOnAction(event -> {
            if(!aprNode.isSelected()){aprNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,4,day));
        });
        mayNode.setOnAction(event -> {
            if(!mayNode.isSelected()){mayNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,5,day));
        });
        junNode.setOnAction(event -> {
            if(!junNode.isSelected()){junNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,6,day));
        });
        julNode.setOnAction(event -> {
            if(!julNode.isSelected()){julNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,7,day));
        });
        augNode.setOnAction(event -> {
            if(!augNode.isSelected()){augNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,8,day));
        });
        sepNode.setOnAction(event -> {
            if(!sepNode.isSelected()){sepNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,9,day));
        });
        octNode.setOnAction(event -> {
            if(!octNode.isSelected()){octNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,10,day));
        });
        novNode.setOnAction(event -> {
            if(!novNode.isSelected()){novNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,11,day));
        });
        decNode.setOnAction(event -> {
            if(!decNode.isSelected()){decNode.setSelected(true);}
            LocalDate currentDate = main.getCurrentDate();
            int day = currentDate.getDayOfMonth();
            int year = currentDate.getYear();
            parent.setDate(LocalDate.of(year,12,day));
        });

    }



}
