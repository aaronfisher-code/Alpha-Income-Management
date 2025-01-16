package strategies;

import controllers.TargetGraphsPageController;
import javafx.scene.chart.XYChart;
import models.DBTargetDatapoint;
import models.EODDataPoint;
import utils.RosterUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ScriptsOnFileStrategy extends AbstractLineGraphStrategy {
    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "Scripts on File";
    public static final String AXIS_LABEL = "No. of scripts on file";
    public static final boolean DOLLAR_FORMAT = false;

    private List<EODDataPoint> currentEODDataPoints;

    public ScriptsOnFileStrategy(LocalDate startDate, LocalDate endDate,
                                 TargetGraphsPageController parent, List<EODDataPoint> currentEODDataPoints, RosterUtils rosterUtils, List<DBTargetDatapoint> targets) {
        super(startDate, endDate, parent, rosterUtils, targets);
        this.currentEODDataPoints = currentEODDataPoints;
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
    public XYChart.Series<Number,Number> getActualSeries() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        int dayDifference = (int) ChronoUnit.DAYS.between(startDate, LocalDate.now());
        int accumulatedQuantity = 0;
        int currentValue = 0;

        for(int i = 0; i < Math.min(dayDifference, length); i++) {
            LocalDate date = startDate.plusDays(i);
            for(int j = 0; j < currentEODDataPoints.size(); j++) {
                if(currentEODDataPoints.get(j).getDate().equals(date)) {
                    if (currentEODDataPoints.get(j).getScriptsOnFile() != 0) {
                        currentValue = currentEODDataPoints.get(j).getScriptsOnFile();
                    }
                    accumulatedQuantity += currentValue;
                    series.getData().add(new XYChart.Data(i, accumulatedQuantity));
                    break;
                }
            }
        }
        return series;
    }
}