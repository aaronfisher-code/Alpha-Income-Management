package controllers;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
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

        minimize.setOnAction(a -> this.main.getStg().setIconified(true));

        maximize.setOnAction(a -> this.main.getBs().maximizeStage());

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



	
}








