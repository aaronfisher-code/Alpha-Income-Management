package components;

import com.dlsc.gemsfx.DialogPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Dialog pane with a close action for every dialog it displays.
 *
 * <p>GemsFX 4.x hides the header unconditionally for blank dialogs. Blank
 * dialogs therefore receive a close button overlaid on their content, while
 * standard dialogs continue to use the built-in header button.</p>
 */
public class CloseableDialogPane extends DialogPane {

    public CloseableDialogPane() {
        getDialogs().addListener((ListChangeListener<Dialog<?>>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::enableCloseButton);
                }
            }
        });
    }

    private void enableCloseButton(Dialog<?> dialog) {
        dialog.setShowCloseButton(true);
        if (dialog.getType() == DialogPane.Type.BLANK) {
            dialog.setTitle("");
            dialog.setShowHeader(false);
            addContentCloseButton(dialog);
        } else {
            dialog.setShowHeader(true);
        }
    }

    private void addContentCloseButton(Dialog<?> dialog) {
        Pane contentPane = findContentPane(this);
        if (contentPane == null) {
            if (getDialogs().contains(dialog)) {
                Platform.runLater(() -> addContentCloseButton(dialog));
            }
            return;
        }

        if (contentPane.getChildren().stream()
                .anyMatch(child -> child.getStyleClass().contains("dialog-close-button"))) {
            return;
        }

        Button closeButton = new Button();
        closeButton.setMnemonicParsing(false);
        closeButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        closeButton.setAccessibleText("Close dialog");
        closeButton.getStyleClass().add("dialog-close-button");
        closeButton.setStyle(
                "-fx-background-color: rgba(100, 112, 122, 0.2);"
                        + "-fx-background-radius: 3px;"
                        + "-fx-padding: 2px;"
                        + "-fx-min-width: 24px;"
                        + "-fx-min-height: 24px;"
                        + "-fx-pref-width: 24px;"
                        + "-fx-pref-height: 24px;"
                        + "-fx-max-width: 24px;"
                        + "-fx-max-height: 24px;"
                        + "-fx-cursor: hand;"
        );
        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent(
                "M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8 2.146 2.854Z"
        );
        closeIcon.setFill(javafx.scene.paint.Color.web("#6e6b7b"));
        closeIcon.setScaleX(0.8);
        closeIcon.setScaleY(0.8);
        closeButton.setGraphic(closeIcon);
        closeButton.setOnAction(_ -> dialog.cancel());

        contentPane.getChildren().add(closeButton);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(8));
    }

    private Pane findContentPane(Node node) {
        if (node instanceof Pane pane && pane.getStyleClass().contains("content-pane")) {
            return pane;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Pane contentPane = findContentPane(child);
                if (contentPane != null) {
                    return contentPane;
                }
            }
        }
        return null;
    }
}
