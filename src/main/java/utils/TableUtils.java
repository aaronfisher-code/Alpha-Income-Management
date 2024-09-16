package utils;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.AccountPayment;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.DoubleAdder;

public class TableUtils {
    public static int getColumnWidth(TableColumn tc){
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.dispose();
        int width = 0;
        for(String s: tc.getText().split("\n")){
            if(fm.stringWidth(s)>width){
                width=fm.stringWidth(s);
            }
        }
        return	width;
    }

    public static int getColumnWidth(Label tc){
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.dispose();
        int width = 0;
        for(String s: tc.getText().split("\n")){
            if(fm.stringWidth(s)>width){
                width=fm.stringWidth(s);
            }
        }
        return	width;
    }

    public static void formatTextFields(GridPane table, Runnable updateTotals) {
        for (Node n : table.getChildren()) {
            if (n instanceof MFXTextField textField) {
                textField.setLeadingIcon(new Label("$"));
                textField.setAlignment(Pos.CENTER_RIGHT);
                textField.delegateFocusedProperty().addListener((_, _, _) -> {
                    if (textField.isValid()) {
                        updateTotals.run();
                    }
                });
            }
        }
    }

    public enum formatStyle {
        CURRENCY, PERCENTAGE, DECIMAL, INTEGER
    }

    public static void formatTextField(MFXTextField textField, Runnable updateTotals, formatStyle style) {
        textField.delegateFocusedProperty().addListener((_, _, _) -> {
            if (textField.isValid()) {
                updateTotals.run();
            }
        });
        switch (style) {
            case CURRENCY -> {
                textField.setLeadingIcon(new Label("$"));
                textField.setAlignment(Pos.CENTER_RIGHT);
            }
            case PERCENTAGE -> {
                textField.setTrailingIcon(new Label("%"));
                textField.setMeasureUnitGap(2);
                textField.setAlignment(Pos.CENTER_RIGHT);
            }
            case DECIMAL -> {
                textField.setAlignment(Pos.CENTER_RIGHT);
            }
            case INTEGER -> {
                textField.setAlignment(Pos.CENTER_RIGHT);
            }
        }
    }

    public static void resizeTableColumns(TableView<?> table, TableColumn<?, ?> extendedCol) {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setMaxWidth(Double.MAX_VALUE);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setFixedCellSize(25.0);
        VBox.setVgrow(table, Priority.ALWAYS);
        for(TableColumn<?, ?> tc: table.getColumns()){
            tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
        }
        Platform.runLater(() -> GUIUtils.customResize(table,extendedCol));
    }
}
