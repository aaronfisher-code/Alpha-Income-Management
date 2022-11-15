package controllers;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.Main;


public class LogController extends Controller {
	
	@FXML private Label logInError, confirmPasswordLabel;
    @FXML private Text subtitle;
	@FXML private TextField username;
	@FXML private PasswordField password, confirmPassword;
	@FXML private Button login, create_acc;
    @FXML private Button maximize,minimize,close;
    @FXML private StackPane backgroundPane;
    @FXML private HBox windowControls;
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main = null;

    public void setMain(Main main) {
        this.main = main;

    }

    @Override
    public void setConnection(Connection c) {
        this.con = c;
    }

    @Override
    public void fill() {

        this.main.getBs().setMoveControl(backgroundPane);

        close.setOnAction(a -> this.main.getStg().close());
        close.setOnMouseEntered(a-> colourWindowButton(close,"#c42b1c","#FFFFFF"));
        close.setOnMouseExited(a-> colourWindowButton(close,"#FFFFFF","#000000"));

        minimize.setOnAction(a -> this.main.getStg().setIconified(true));
        minimize.setOnMouseEntered(a-> colourWindowButton(minimize,"#f5f5f5","#000000"));
        minimize.setOnMouseExited(a-> colourWindowButton(minimize,"#FFFFFF","#000000"));

        maximize.setOnAction(a -> this.main.getBs().maximizeStage());
        maximize.setOnMouseEntered(a-> colourWindowButton(maximize,"#f5f5f5","#000000"));
        maximize.setOnMouseExited(a-> colourWindowButton(maximize,"#FFFFFF","#000000"));

        this.main.getBs().maximizedProperty().addListener(e->
        {
            if(this.main.getBs().isMaximized()){
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M13 0H6a2 2 0 0 0-2 2 2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm0 13V4a2 2 0 0 0-2-2H5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1zM3 4a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4z\n");
                newIcon.setScaleX(0.95);
                newIcon.setScaleY(0.75);
                windowControls.setPrefHeight(25);
            }else{
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z\n");
                newIcon.setScaleX(0.575);
                newIcon.setScaleY(0.575);
                windowControls.setPrefHeight(30);
            }
        });


    }
    

	public void userLogin() {
        String status = "Success";
        String usrname = username.getText();
        String pswrd = password.getText();
        if(usrname.isEmpty()) {
        	logInError.setText("Please enter a valid username");
            status = "Error";
        } else {
            //query
            String sql = "SELECT * FROM accounts Where username = ?";
            try {
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, usrname);
                resultSet = preparedStatement.executeQuery();
                if (resultSet == null || !resultSet.next()) {
                	logInError.setText("Unrecognized username");
                    status = "Error";
                }else{
                    User user = new User(resultSet);
                    if(user.getPassword()==null){
                        status = "PasswordReset";
                        setNewPasswordView(user);
                    }else if(user.getPassword().equals(password.getText())){
                        status = "Success";
                    }else{
                        status = "Error";
                    }
                }
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                status = "Exception";
            }
        }
        
        if (status.equals("Success")) {
            try {
            	main.setCurrentUser(new User(resultSet));
            	main.changeScene("/views/FXML/MainMenu.fxml");
            } catch (IOException ex) {
            	System.err.println("Failed login");
                System.err.println(ex.getMessage());
            }
        }else{
            logInError.setText("Login error, please ensure your username and password are correct");
        }
    }

    public void userLoginWithPassword(User user) {
        //TODO new Password login doesnt re-verify user

        String status = "Success";
        if(!password.getText().equals(confirmPassword.getText())) {
            logInError.setText("Passwords dont match");
            status = "Error";
        } else {
            String sql = "UPDATE accounts SET password = ? WHERE username = ?";
            try {
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, password.getText());
                preparedStatement.setString(2, user.getUsername());
                preparedStatement.executeUpdate();
                status="Success";
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
            }
        }

        if (status.equals("Success")) {
            try {
                main.setCurrentUser(user);
                main.changeScene("/views/FXML/MainMenu.fxml");
            } catch (IOException ex) {
                System.err.println("Failed login");
                System.err.println(ex.getMessage());
            }
        }
    }

    public void setNewPasswordView(User user){
        //TODO no option to return to standard login after requesting new password
        confirmPasswordLabel.setVisible(true);
        confirmPassword.setVisible(true);
        login.setOnAction(actionEvent -> userLoginWithPassword(user));
        subtitle.setText("Please set a new password for this account before signing in");

    }

    private void colourWindowButton(Button b, String backgroundHex, String strokeHex){
        b.setStyle("-fx-background-color: "+ backgroundHex+ ";-fx-background-radius:0");
        SVGPath icon = (SVGPath) b.getGraphic();
        icon.setFill(Paint.valueOf(strokeHex));
        icon.setStroke(Paint.valueOf(strokeHex));
    }



	
}








