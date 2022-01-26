package controllers;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import models.User;
import javafx.scene.control.Hyperlink;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.Main;



public class LogController extends Controller {
	
	@FXML private Label logInError, signUpError;
	@FXML private TextField username, sign_up_username, firstName, lastName, email;	
	@FXML private PasswordField password, sign_up_pass, re_password;	
	@FXML private Button login, create_acc;	
	@FXML private Hyperlink register, back_to_login;
    @FXML private Button maximize,minimize,close;
    @FXML private StackPane backgroundPane;
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main = null;
    

	public void userLogin() {
        String status = "Success";
        String usrname = username.getText();
        String pswrd = password.getText();
        if(usrname.isEmpty() || pswrd.isEmpty()) {
        	logInError.setText("Please enter all fields!");
            status = "Error";
        } else {
            //query
            String sql = "SELECT * FROM accounts Where username = ? and password = ?";
            try {
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, usrname);
                preparedStatement.setString(2, pswrd);
                resultSet = preparedStatement.executeQuery();
                if (resultSet == null || !resultSet.next()) {
                	logInError.setText("Incorrect username or password!");
                    status = "Error";
                }
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                status = "Exception";
            }
        }
        
        if (status.equals("Success")) {
            try {
            	main.changeUser(new User(resultSet));
            	main.changeScene("/views/FXML/MainMenu.fxml");
            } catch (IOException ex) {
            	System.err.println("Failed login");
                System.err.println(ex.getMessage());
            }
        }
    }
	
	public boolean searchAccount(String userquery) {
        String usrname = userquery;
        if(usrname.isEmpty()) {
        	return false;
        } else {
            //query
            String sql = "SELECT * FROM accounts Where username = ?";
            try {
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, usrname);
                resultSet = preparedStatement.executeQuery();
                if (resultSet == null || !resultSet.next()) {
                	return false;
                } else {
                    return true;
                }
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                return false;
            }
        }
	}


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
	
}








