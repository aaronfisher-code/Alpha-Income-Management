package utils;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.awt.*;
import java.awt.image.BufferedImage;
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
}
