package application;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SplashScreen {

    private Stage splashStage;

    public void showSplash() {
        System.out.println("Showing splash screen...");
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        // Load the splash screen image
        ImageView splashImage = new ImageView(new Image(getClass().getResourceAsStream("/images/splashScreen.jpg")));
        splashImage.setPreserveRatio(true);
        splashImage.setFitWidth(600); // Adjust the width as needed
        splashImage.setFitHeight(400); // Adjust the height as needed

        StackPane root = new StackPane(splashImage);
        Scene scene = new Scene(root);

        splashStage.setScene(scene);
        splashStage.show();
    }

    public void closeSplash() {
        if (splashStage != null) {
            splashStage.close();
        }
    }
}