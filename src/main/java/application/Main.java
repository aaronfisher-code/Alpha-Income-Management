package application;


import com.dlsc.gemsfx.DialogPane;
import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import controllers.PageController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Store;
import models.User;
import utils.LogRedirector;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;


public class Main extends Application {
	
	private static Stage stg;
	private User currentUser;
	private Store currentStore;
	public PageController c;
	private static BorderlessScene bs;
	private Image icon;
	private LocalDate currentDate;
	private String version;
	private SplashScreen splashScreen;
	private DialogPane dialogPane;

	@Override
	public void init() throws Exception {
		LogRedirector.redirectOutputToFile("app.log");
		Thread.setDefaultUncaughtExceptionHandler(this::handleGlobalException);
		System.out.println("Application initializing...");

		splashScreen = new SplashScreen();
		Platform.runLater(() -> splashScreen.showSplash());

		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Thin.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_ExtraLight.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Light.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Regular.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Medium.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_SemiBold.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Bold.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_ExtraBold.otf"), 16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Black.otf"), 16);

		try (InputStream iconStream = getClass().getResourceAsStream("/images/alpha logo.png")) {
			if (iconStream != null) {
				icon = new Image(iconStream);
			} else {
				System.err.println("Icon image not found!");
			}
		}
		setCurrentDate(LocalDate.now());

		Properties prop = new Properties();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			prop.load(in);
			version = prop.getProperty("VERSION");
		}
	}

	@Override
	public void start(Stage primaryStage) {
		// Use Platform.runLater to create and show the main window after a delay
		Platform.runLater(() -> {
			try {
				showMainStage(primaryStage);
			} catch (Exception e) {
				c.getDialogPane().showError("Error loading main stage", e);
			} finally {
				// Close the splash screen
				splashScreen.closeSplash();
			}
		});
	}

	private void showMainStage(Stage primaryStage) throws IOException {
		System.out.println("Main application stage loading...");
		stg = primaryStage;
		setupScene("/views/FXML/LogIn.fxml", true);
		System.out.println("Application started successfully. Running version " + version);
	}

	public void changeScene(String fxml) throws IOException {
		stg.close();
		stg = new Stage();
		setupScene(fxml, false);
	}

	private void setupScene(String fxmlPath, boolean isMainStage) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
		Parent root = loader.load();
		c = loader.getController();
		c.setMain(this);
		stg.setTitle("Alpha Income Management " + version);
		bs = new BorderlessScene(stg, StageStyle.TRANSPARENT, root);
		bs.removeDefaultCSS();
		bs.setFill(Color.TRANSPARENT);
		stg.setScene(bs);
		stg.getIcons().add(icon);
		stg.show();
		bs.maximizeStage();
		bs.setResizable(true);
		if (!isMainStage) {
			setBs(bs);
		}
		c.fill();
		dialogPane = c.getDialogPane();
		Platform.runLater(() -> {
			Thread.currentThread().setUncaughtExceptionHandler(this::handleGlobalException);
		});
	}

	public void handleGlobalException(Thread t, Throwable e) {
		System.err.println("An unexpected error occurred in thread " + t.getName());
		if(e instanceof RejectedExecutionException) {
			System.err.println("Tasks submitted on dead executor, ignoring...: " + e.getMessage());
			return;
		}
		e.printStackTrace(); // Print to console for debugging
		Platform.runLater(() -> {
			if (dialogPane != null) {
				if (e instanceof Exception) {
					dialogPane.showError("An unexpected error occurred", (Exception) e);
				} else {
					// If it's not an Exception, create a new Exception with the Throwable's message
					Exception exception = new Exception(e.getMessage(), e);
					dialogPane.showError("An unexpected error occurred", exception);
				}
			} else {
				// Fallback if the current PageController or DialogPane is not available
				System.err.println("Unable to show error dialog: " + e.getMessage());
			}
		});
	}
	
	public void setCurrentUser(User newUser){
		this.currentUser = newUser;
	}

	public Stage getStg(){
		return stg;
	}
	public BorderlessScene getBs() {
		return bs;
	}

	public void setBs(BorderlessScene newScene) {
		bs=newScene;
	}

	public User getCurrentUser() {return currentUser;}

	public Store getCurrentStore() {return currentStore;}

	public void setCurrentStore(Store currentStore) {this.currentStore = currentStore;}

	public LocalDate getCurrentDate() {return currentDate;}

	public void setCurrentDate(LocalDate currentDate) {this.currentDate = currentDate;}

	public void setDialogPane(DialogPane dialogPane) {this.dialogPane = dialogPane;}

	public DialogPane getDialogPane() {return dialogPane;}

	public PageController getController() {
		return c;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
