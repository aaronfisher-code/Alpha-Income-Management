package strategies;

import application.Main;
import controllers.TargetGraphsPageController;
import javafx.scene.chart.XYChart;
import models.TillReportDataPoint;
import services.TargetService;
import services.TillReportService;
import utils.RosterUtils;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class OTCDollarPerCustomerStrategy implements LineGraphTargetStrategy {

    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "OTC $/Customer";
    public static final String AXIS_LABEL = "$/Customer";

    public static final boolean DOLLAR_FORMAT = true;

    public int length;

    public double target1;
    public double target2;
    public LocalDate startDate;
    public LocalDate endDate;
    public TargetGraphsPageController parent;

    public Main main;
    private List<TillReportDataPoint> currentTillReportDataPoints;
    private RosterUtils rosterUtils;
    private TillReportService tillReportService;
    private TargetService targetService;

    public OTCDollarPerCustomerStrategy(LocalDate startDate, LocalDate endDate, TargetGraphsPageController parent) {
        this.length = (int) ChronoUnit.DAYS.between(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.parent = parent;
        this.main = parent.getMain();
        this.tillReportService = new TillReportService();
        this.targetService = new TargetService();

        try {
            this.rosterUtils = new RosterUtils(main, startDate, endDate);
            double[] targets = targetService.getTargets(main.getCurrentStore().getStoreID(), "otcDollarPerCustomer");
            this.target1 = targets[0];
            this.target2 = targets[1];

            this.currentTillReportDataPoints = tillReportService.getTillReportDataPointsByKey(
                    main.getCurrentStore().getStoreID(),
                    startDate,
                    endDate,
                    "Avg. OTC Sales Per Customer"
            );
        } catch (SQLException e) {
            parent.throwError(e);
        }
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

    @Override
    public Boolean isDollarFormat() {
        return DOLLAR_FORMAT;
    }

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

    @Override
    public XYChart.Series<Number, Number> getTarget1Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        double accumulatedQuantity = 0;
        for(int i=0;i<length+1;i++){
            LocalDate date = startDate.plusDays(i);
            accumulatedQuantity += rosterUtils.getDayDuration(date)*target1;
            series.getData().add(new XYChart.Data(i, accumulatedQuantity));
        }

        return series;
    }

    @Override
    public XYChart.Series<Number, Number> getTarget2Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        double accumulatedQuantity = 0;
        for(int i=0;i<length+1;i++){
            LocalDate date = startDate.plusDays(i);
            accumulatedQuantity += rosterUtils.getDayDuration(date)*target2;
            series.getData().add(new XYChart.Data(i, accumulatedQuantity));
        }

        return series;
    }
}
