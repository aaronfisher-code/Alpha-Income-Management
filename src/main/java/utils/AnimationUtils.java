package utils;

import eu.iamgio.animated.AnimatedPosition;
import javafx.animation.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import org.apache.poi.ss.formula.functions.T;

public class AnimationUtils {
    public static void slideIn(final VBox pane, double width){
        StackPane parent = (StackPane) pane.getParent();
        int index = parent.getChildren().indexOf(pane);
        Duration cycleDuration = Duration.millis(200);
        Timeline timeline = new Timeline(
                new KeyFrame(cycleDuration,
                        new KeyValue(pane.translateXProperty(),width, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    public static void changeSize(final VBox pane, double width){
        Duration cycleDuration = Duration.millis(200);
        Timeline timeline = new Timeline(
                new KeyFrame(cycleDuration,
                        new KeyValue(pane.prefWidthProperty(),width, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

}


