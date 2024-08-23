package utils;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import java.util.concurrent.atomic.DoubleAdder;

public class GUIUtils {
    public static void customResize(TableView<?> view,TableColumn<?,?> targetCol, Label label) {

        DoubleAdder width = new DoubleAdder();
        view.getColumns().forEach(col -> {
            width.add(col.getWidth());
        });
        double tableWidth = view.getWidth();

        if (tableWidth > width.doubleValue()) {
            targetCol.setPrefWidth(targetCol.getWidth()+(tableWidth-width.doubleValue())-7);
            label.setMinWidth(label.getWidth() +(tableWidth-width.doubleValue())-7);
        }
    }

    public static void customResize(TableView<?> view,TableColumn<?,?> targetCol) {
        DoubleAdder width = new DoubleAdder();
        view.getColumns().forEach(col -> width.add(col.getWidth()));
        double tableWidth = view.getWidth();

        if (targetCol!=null && tableWidth > width.doubleValue()) {
            targetCol.setPrefWidth(targetCol.getWidth()+(tableWidth-width.doubleValue())-7);
        }
    }

    public static void formatTabSelect(BorderPane b){
        adjustTabHighlighting(b, "#0F60FF");
    }

    public static void formatTabDeselect(BorderPane b){
        adjustTabHighlighting(b, "#A3A3A3");
    }

    public static void adjustTabHighlighting(BorderPane b, String color){
        for (Node n:b.getChildren()) {
            if(n.getAccessibleRole() == AccessibleRole.TEXT){
                Label a = (Label) n;
                a.setStyle("-fx-text-fill: "+color);
            }
            if(n.getAccessibleRole() == AccessibleRole.PARENT){
                Region a = (Region) n;
                a.setStyle("-fx-background-color: "+color);
            }
        }
    }
}
