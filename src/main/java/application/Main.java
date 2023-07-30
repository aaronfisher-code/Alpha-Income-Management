package application;


import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import controllers.Controller;
import javafx.application.Application;
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Properties;


public class Main extends Application {
	
	private static Stage stg;
	private User currentUser;
	private Store currentStore;
	private Connection con;
	public Controller c;
	private static BorderlessScene bs;
	private Image icon = new Image(getClass().getResourceAsStream("/images/alpha logo.png"));
	private LocalDate currentDate;

	private String version;

	@Override
	public void start(Stage primaryStage) throws Exception{
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Thin.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_ExtraLight.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Light.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Regular.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Medium.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_SemiBold.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Bold.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_ExtraBold.otf"),16);
		Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat/Montserrat_Black.otf"),16);
		setCurrentDate(LocalDate.now());
		stg = primaryStage;
		System.setProperty("prism.lcdtext", "false");
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/LogIn.fxml"));
		Parent root = loader.load();
		c = loader.getController();
		c.setMain(this);
		con = DBConnector.conDB();
		c.setConnection(con);
		Properties prop = new Properties();
		InputStream in = DBConnector.class.getClassLoader().getResourceAsStream("application.properties");
		prop.load(in);
		assert in != null;
		in.close();
		version = prop.getProperty("VERSION");
		primaryStage.setTitle("Alpha Income Management "+ version);
		bs = new BorderlessScene(primaryStage,StageStyle.TRANSPARENT,root);
		bs.removeDefaultCSS();
		bs.setFill(Color.TRANSPARENT);
		primaryStage.setScene(bs);
		primaryStage.getIcons().add(icon);
		primaryStage.show();
		bs.maximizeStage();
//		stg.setMinWidth(600);
		bs.setResizable(true);
		c.fill();
	}
	
	public void changeScene(String fxml) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
		Parent root = loader.load();
		c = loader.getController();
		c.setMain(this);
		c.setConnection(con);
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
