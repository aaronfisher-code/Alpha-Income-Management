package strategies;

import controllers.TargetGraphsPageController;
import javafx.scene.chart.XYChart;
import models.DBTargetDatapoint;
import models.TillReportDataPoint;
import utils.RosterUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class OTCDollarPerCustomerStrategy extends AbstractLineGraphStrategy {

    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "OTC $/Customer";
    public static final String AXIS_LABEL = "$/Customer";
    public static final boolean DOLLAR_FORMAT = true;

    private List<TillReportDataPoint> currentTillReportDataPoints;

    public OTCDollarPerCustomerStrategy(LocalDate startDate, LocalDate endDate, TargetGraphsPageController parent, List<TillReportDataPoint> currentTillReportDataPoints, RosterUtils rosterUtils, List<DBTargetDatapoint> targets) {
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
        int accumulatedQuantity = 0;  // Keep track of the accumulated quantity
        for(int i=0;i<Math.min(dayDifference,length);i++){
            LocalDate date = startDate.plusDays(i);
            //find current till report data point with date
            for(int j=0;j<currentTillReportDataPoints.size();j++){
                if(currentTillReportDataPoints.get(j).getAssignedDate().equals(date)){
                    accumulatedQuantity += currentTillReportDataPoints.get(j).getAmount();  // Add the quantity for the current day to the total
                    series.getData().add(new XYChart.Data(i, accumulatedQuantity));  // Add the accumulated quantity to the series
                    break;
                }
            }
        }
        return series;
    }
}
