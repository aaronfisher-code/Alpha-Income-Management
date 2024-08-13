package application;


import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import controllers.Controller;
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
import utils.DBConnector;
import utils.LogRedirector;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;


public class Main extends Application {
	
	private static Stage stg;
	private User currentUser;
	private Store currentStore;
	public Controller c;
	private static BorderlessScene bs;
	private Image icon;
	private LocalDate currentDate;
	private String version;
	private SplashScreen splashScreen;

	@Override
	public void init() throws Exception {
//		LogRedirector.redirectOutputToFile("app.log");
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
				e.printStackTrace();
			} finally {
				// Close the splash screen
				splashScreen.closeSplash();
			}
		});
	}

	private void showMainStage(Stage primaryStage) throws IOException {
		System.out.println("Main application stage loading...");
		stg = primaryStage;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/LogIn.fxml"));
		Parent root = loader.load();
		c = loader.getController();
		c.setMain(this);

		primaryStage.setTitle("Alpha Income Management " + version);
		bs = new BorderlessScene(primaryStage, StageStyle.TRANSPARENT, root);
		bs.removeDefaultCSS();
		bs.setFill(Color.TRANSPARENT);
		primaryStage.setScene(bs);
		primaryStage.getIcons().add(icon);
		primaryStage.show();
		bs.maximizeStage();
		bs.setResizable(true);
		c.fill();
		System.out.println("Application started successfully. Running version " + version);
	}
	
	public void changeScene(String fxml) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
		Parent root = loader.load();
		c = loader.getController();
		c.setMain(this);
		stg.close();
		stg = new Stage();
		stg.setTitle("Alpha Income Management "+ version);
		bs = new BorderlessScene(stg,StageStyle.UNDECORATED,root);
		setBs(bs);
		stg.setScene(bs);
		stg.getIcons().add(icon);
		stg.show();
		bs.maximizeStage();
		bs.setResizable(true);
//		stg.setMinWidth(950);
		bs.removeDefaultCSS();
		c.fill();
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

	public Controller getController() {
		return c;
	}

	public static void main(String[] args) {
		launch(args);
	}
	
}
