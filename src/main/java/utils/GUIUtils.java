package utils;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.TableColumnHeader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.DoubleAdder;

public class GUIUtils {
    public static void customResize(TableView<?> view,TableColumn<?,?> targetCol) {

        DoubleAdder width = new DoubleAdder();
        view.getColumns().forEach(col -> {
            width.add(col.getWidth());
        });
        double tableWidth = view.getWidth();

        if (tableWidth > width.doubleValue()) {
            targetCol.setPrefWidth(targetCol.getWidth()+(tableWidth-width.doubleValue())-7);
        }

    }
}
