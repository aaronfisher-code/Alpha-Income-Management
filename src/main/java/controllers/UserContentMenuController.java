package controllers;

import application.Main;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import models.Shift;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserContentMenuController extends Controller {

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Shift shift;
    private MainMenuController parent;

    @FXML
    private VBox buttonContainer;

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(MainMenuController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }


    @Override
    public void fill() {
        for(Node b:buttonContainer.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.BUTTON){
                Button a = (Button) b;
                a.addEventHandler(MouseEvent.MOUSE_ENTERED,
                        e -> hoverSelect(a));

                a.addEventHandler(MouseEvent.MOUSE_EXITED,
                        e -> hoverDeSelect(a));
            }

        };
    }

    // On main page, go back to login page when user logout
    public void logOut() throws IOException {
        main.changeScene("/views/FXML/LogIn.fxml");
    }

    public void hoverSelect(Button i){
        i.setStyle("-fx-background-color: #E5EEFF;-fx-text-fill: #0F60FF");
        i.getGraphic().setStyle("-fx-fill: #0F60FF");
    }

    public void hoverDeSelect(Button i){
        i.setStyle("-fx-background-color: #FFFFFF;-fx-text-fill: #6f6c7b");
        i.getGraphic().setStyle("-fx-fill: #6f6c7b");
    }



}
