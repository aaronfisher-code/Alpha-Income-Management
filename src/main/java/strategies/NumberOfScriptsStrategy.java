package strategies;

import controllers.TargetGraphsPageController;
import javafx.scene.chart.XYChart;
import models.DBTargetDatapoint;
import models.TillReportDataPoint;
import utils.RosterUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NumberOfScriptsStrategy extends AbstractLineGraphStrategy {
    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "Number of Scripts";
    public static final String AXIS_LABEL = "Script #";
    public static final boolean DOLLAR_FORMAT = false;
    List<TillReportDataPoint> currentTillReportDataPoints;

    public NumberOfScriptsStrategy(LocalDate startDate, LocalDate endDate, TargetGraphsPageController parent, List<TillReportDataPoint> currentTillReportDataPoints, RosterUtils rosterUtils, List<DBTargetDatapoint> targets) {
        super(startDate, endDate, parent, rosterUtils, targets);
        this.currentTillReportDataPoints = currentTillReportDataPoints;
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

    @Override
    public Boolean isDollarFormat() {
        return DOLLAR_FORMAT;
    }

    @Override
    public XYChart.Series<Number,Number> getActualSeries(){
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        int dayDifference = (int) ChronoUnit.DAYS.between(startDate, LocalDate.now());
        int accumulatedQuantity = 0;
        for(int i=0;i<Math.min(dayDifference,length);i++){
            LocalDate date = startDate.plusDays(i);
            for(int j=0;j<currentTillReportDataPoints.size();j++){
                if(currentTillReportDataPoints.get(j).getAssignedDate().equals(date)){
                    accumulatedQuantity += currentTillReportDataPoints.get(j).getQuantity();
                    series.getData().add(new XYChart.Data(i, accumulatedQuantity));
                    break;
                }
            }
        }
        return series;
    }
}
