package controllers;

import io.github.palexdev.materialfx.controls.MFXProgressBar;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import services.UserService;

import java.io.IOException;
import java.util.concurrent.Executors;

public class LogController extends PageController {

    @FXML private Label logInError, confirmPasswordLabel;
    @FXML private Text subtitle;
    @FXML private TextField username;
    @FXML private PasswordField password, confirmPassword;
    @FXML private Button login;
    @FXML private Button maximize, minimize, close;
    @FXML private StackPane backgroundPane;
    @FXML private HBox windowControls;
    @FXML private MFXProgressBar progressBar;
    private UserService userService;

    @FXML
    private void initialize() {
        try {
            userService = new UserService();
            executor = Executors.newCachedThreadPool();
        }catch (IOException ex) {
            dialogPane.showError("Failed to initialize user service", ex);
        }
    }

    @Override
    public void fill() {
        this.main.getBs().setMoveControl(backgroundPane);
        close.setOnAction(_ -> this.main.getStg().close());
        close.setOnMouseEntered(_ -> colourWindowButton(close, "#c42b1c", "#FFFFFF"));
        close.setOnMouseExited(_ -> colourWindowButton(close, "#FFFFFF", "#000000"));
        minimize.setOnAction(_ -> this.main.getStg().setIconified(true));
        minimize.setOnMouseEntered(_ -> colourWindowButton(minimize, "#f5f5f5", "#000000"));
        minimize.setOnMouseExited(_ -> colourWindowButton(minimize, "#FFFFFF", "#000000"));
        maximize.setOnAction(_ -> this.main.getBs().maximizeStage());
        maximize.setOnMouseEntered(_ -> colourWindowButton(maximize, "#f5f5f5", "#000000"));
        maximize.setOnMouseExited(_ -> colourWindowButton(maximize, "#FFFFFF", "#000000"));
        this.main.getBs().maximizedProperty().addListener(_ -> {
            if (this.main.getBs().isMaximized()) {
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M13 0H6a2 2 0 0 0-2 2 2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm0 13V4a2 2 0 0 0-2-2H5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1zM3 4a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4z\n");
                newIcon.setScaleX(0.95);
                newIcon.setScaleY(0.75);
                windowControls.setPrefHeight(25);
            } else {
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z\n");
                newIcon.setScaleX(0.575);
                newIcon.setScaleY(0.575);
                windowControls.setPrefHeight(30);
            }
        });
    }

    public void userLogin() {
        String username = this.username.getText();
        String password = this.password.getText();
        if (username.isEmpty()) {
            logInError.setText("Please enter a valid username");
            return;
        }
        progressBar.setVisible(true);
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                return userService.verifyPassword(username, password);
            }
        };
        loginTask.setOnSucceeded(_ -> {
            progressBar.setVisible(false);
            handleLoginResult(loginTask.getValue());
        });
        loginTask.setOnFailed(_ -> {
            progressBar.setVisible(false);
            dialogPane.showError("Error", "Failed to login", loginTask.getException());
        });
        executor.submit(loginTask);
    }

    private void handleLoginResult(User loginResult) {
        if(loginResult == null) {
            logInError.setText("Login error, please ensure your username and password are correct");
        }else if(loginResult.getPassword() == null){
            try {
                setNewPasswordView(loginResult);
            } catch (Exception ex) {
                dialogPane.showError("Failed to set new password view", ex);
            }
        }else{
            loadUserAndChangeScene(loginResult);
        }
    }

    private void loadUserAndChangeScene(User currentUser) {
        Task<User> loadUserTask = new Task<>() {
            @Override
            protected User call() {
                currentUser.setPermissions(userService.getUserPermissions(currentUser.getUserID()));
                return currentUser;
            }
        };
        loadUserTask.setOnSucceeded(_ -> {
            progressBar.setVisible(false);
            main.setCurrentUser(loadUserTask.getValue());
            try {
                main.changeScene("/views/FXML/MainMenu.fxml");
            } catch (IOException ex) {
                dialogPane.showError("Failed to change scene", ex);
            }
        });
        loadUserTask.setOnFailed(_ -> {
            progressBar.setVisible(false);
            dialogPane.showError("Error","Failed to load user data", loadUserTask.getException());
        });
        progressBar.setVisible(true);
        executor.submit(loadUserTask);
    }

    public void userLoginWithPassword(User currentUser) {
        if (!password.getText().equals(confirmPassword.getText())) {
            logInError.setText("Passwords don't match");
            return;
        }
        progressBar.setVisible(true);
        Task<Void> updatePasswordTask = new Task<>() {
            @Override
            protected Void call() {
                userService.updateUserPassword(currentUser.getUserID(), password.getText());
                return null;
            }
        };
        updatePasswordTask.setOnSucceeded(_ -> {
            progressBar.setVisible(false);
            loadUserAndChangeScene(currentUser);
        });

        updatePasswordTask.setOnFailed(_ -> {
            progressBar.setVisible(false);
            dialogPane.showError("Error","Failed to update password", updatePasswordTask.getException());
        });

        executor.submit(updatePasswordTask);
    }

    public void setNewPasswordView(User currentUser) {
        Platform.runLater(() -> {
            confirmPasswordLabel.setVisible(true);
            confirmPassword.setVisible(true);
            username.setOnAction(_ -> userLoginWithPassword(currentUser));
            password.setOnAction(_ -> userLoginWithPassword(currentUser));
            confirmPassword.setOnAction(_ -> userLoginWithPassword(currentUser));
            login.setOnAction(_ -> userLoginWithPassword(currentUser));
            subtitle.setText("Please set a new password for this account before signing in");
        });
    }

    private void colourWindowButton(Button b, String backgroundHex, String strokeHex) {
        b.setStyle("-fx-background-color: " + backgroundHex + ";-fx-background-radius:0");
        SVGPath icon = (SVGPath) b.getGraphic();
        icon.setFill(Paint.valueOf(strokeHex));
        icon.setStroke(Paint.valueOf(strokeHex));
    }
}
