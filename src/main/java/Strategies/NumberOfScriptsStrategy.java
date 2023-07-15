package Strategies;

import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class NumberOfScriptsStrategy implements LineGraphTargetStrategy {

    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "Number of Scripts";
    public static final String AXIS_LABEL = "Script #";

    public int length;
    public LocalDate startDate;
    public LocalDate endDate;

    public NumberOfScriptsStrategy(LocalDate startDate, LocalDate endDate) {
        this.length = (int) ChronoUnit.DAYS.between(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getLength() {
        return length;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
    @Override
    public String getStrategyName() {
        return GRAPH_TITLE;
    }

    @Override
    public boolean getYAxisVisibility() {
        return SHOW_Y_AXIS;
    }

    @Override
    public String getAxisLabel() {
        return AXIS_LABEL;
    }

    public XYChart.Series<Number,Number> getActualSeries(){
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        for(int i=0;i<Math.round(length*0.75);i++)
            series.getData().add(new XYChart.Data(i, ThreadLocalRandom.current().nextInt((50000/length)*(i), (50000/length)*(i+3) + 1)));
        return series;
    }

    @Override
    public XYChart.Series<Number, Number> getTarget1Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        for(int i=0;i<length+1;i++)
            series.getData().add(new XYChart.Data(i, ThreadLocalRandom.current().nextInt((50000/length)*(i-1), (50000/length)*(i+1) + 1)));
        return series;
    }

    @Override
    public XYChart.Series<Number, Number> getTarget2Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        for(int i=0;i<length+1;i++)
            series.getData().add(new XYChart.Data(i, ThreadLocalRandom.current().nextInt((50000/length)*(i), (50000/length)*(i+3) + 1)));
        return series;
    }
}
