package Strategies;

import javafx.scene.chart.XYChart;

import java.time.LocalDate;

public interface LineGraphTargetStrategy {

    String getStrategyName();
    String getAxisLabel();
    boolean getYAxisVisibility();
    int getLength();
    LocalDate getStartDate();
    LocalDate getEndDate();

    Boolean isDollarFormat();

    XYChart.Series<Number, Number> getActualSeries();
    XYChart.Series<Number, Number> getTarget1Series();
    XYChart.Series<Number, Number> getTarget2Series();
}
