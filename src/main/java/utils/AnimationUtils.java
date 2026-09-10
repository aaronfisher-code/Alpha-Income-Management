package utils;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableDoubleValue;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Map;
import java.util.WeakHashMap;

public final class AnimationUtils {
    private static final Map<Node, RunningAnimation> ACTIVE_ANIMATIONS = new WeakHashMap<>();

    private AnimationUtils() {
    }

    public static void slideIn(final VBox pane, double width){
        animateTransform(pane, pane.translateXProperty(), width, 200);
    }

    public static void changeSize(final VBox pane, double width){
        stop(pane);
        if (!PerformanceSettings.layoutAnimationsEnabled()) {
            pane.setPrefWidth(width);
            return;
        }
        play(pane, pane.prefWidthProperty(), width, 200, false);
    }

    public static void translateY(Node node, double target, double requestedMillis) {
        animateTransform(node, node.translateYProperty(), target, requestedMillis);
    }

    public static void stop(Node node) {
        RunningAnimation active = ACTIVE_ANIMATIONS.remove(node);
        if (active != null) {
            active.animation().stop();
            active.cleanup().run();
        }
    }

    private static void animateTransform(Node node, WritableDoubleValue property,
                                         double target, double requestedMillis) {
        stop(node);
        if (!PerformanceSettings.animationsEnabled()) {
            property.set(target);
            return;
        }
        play(node, property, target, requestedMillis, true);
    }

    private static void play(Node node, WritableDoubleValue property, double target,
                             double requestedMillis, boolean cacheDuringAnimation) {
        double millis = PerformanceSettings.animationDuration(requestedMillis);
        if (millis <= 0) {
            property.set(target);
            return;
        }

        boolean wasCached = node.isCache();
        CacheHint previousHint = node.getCacheHint();
        if (cacheDuringAnimation) {
            node.setCache(true);
            node.setCacheHint(CacheHint.SPEED);
        }

        Runnable cleanup = () -> {
            if (cacheDuringAnimation) {
                node.setCache(wasCached);
                node.setCacheHint(previousHint);
            }
        };
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(millis),
                new KeyValue(property, target, Interpolator.EASE_BOTH)));
        RunningAnimation running = new RunningAnimation(timeline, cleanup);
        ACTIVE_ANIMATIONS.put(node, running);
        timeline.setOnFinished(_ -> {
            if (ACTIVE_ANIMATIONS.remove(node, running)) {
                cleanup.run();
            }
        });
        timeline.play();
    }

    private record RunningAnimation(Animation animation, Runnable cleanup) {
    }
}
