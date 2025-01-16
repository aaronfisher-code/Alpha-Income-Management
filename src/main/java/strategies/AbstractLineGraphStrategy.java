package strategies;

import application.Main;
import controllers.TargetGraphsPageController;
import javafx.scene.chart.XYChart;
import models.DBTargetDatapoint;
import services.TargetService;
import utils.RosterUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractLineGraphStrategy implements LineGraphTargetStrategy {
    protected int length;
    protected LocalDate startDate;
    protected LocalDate endDate;
    protected List<DBTargetDatapoint> targets;
    protected RosterUtils rosterUtils;
    protected Main main;
    protected TargetGraphsPageController parent;

    public AbstractLineGraphStrategy(LocalDate startDate, LocalDate endDate,
                                     TargetGraphsPageController parent, RosterUtils rosterUtils, List<DBTargetDatapoint> targets) {
        this.length = (int) ChronoUnit.DAYS.between(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.parent = parent;
        this.main = parent.getMain();
        this.rosterUtils = rosterUtils;
        this.targets = targets;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public LocalDate getStartDate() {
        return startDate;
    }

    @Override
    public LocalDate getEndDate() {
        return endDate;
    }

    @Override
    public XYChart.Series<Number, Number> getTarget1Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        double accumulatedQuantity = 0;

        if (this.targets.isEmpty()) {
            return series;
        }

        targets.sort(Comparator.comparing(DBTargetDatapoint::getDate));
        int currentTargetIndex = -1;
        double currentTargetValue = 0;

        for (int i = 0; i < length + 1; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            while (currentTargetIndex < targets.size() - 1 &&
                    !currentDate.isBefore(targets.get(currentTargetIndex + 1).getDate())) {
                currentTargetIndex++;
                currentTargetValue = targets.get(currentTargetIndex).getTarget1Actual();
            }
            accumulatedQuantity += rosterUtils.getDayDuration(currentDate) * currentTargetValue;
            series.getData().add(new XYChart.Data<>(i, accumulatedQuantity));
        }

        return series;
    }

    @Override
    public XYChart.Series<Number, Number> getTarget2Series() {
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        double accumulatedQuantity = 0;

        if (this.targets.isEmpty()) {
            return series;
        }

        targets.sort(Comparator.comparing(DBTargetDatapoint::getDate));
        int currentTargetIndex = -1;
        double currentTargetValue = 0;

        for (int i = 0; i < length + 1; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            while (currentTargetIndex < targets.size() - 1 &&
                    !currentDate.isBefore(targets.get(currentTargetIndex + 1).getDate())) {
                currentTargetIndex++;
                currentTargetValue = targets.get(currentTargetIndex).getTarget2Actual();
            }
            accumulatedQuantity += rosterUtils.getDayDuration(currentDate) * currentTargetValue;
            series.getData().add(new XYChart.Data<>(i, accumulatedQuantity));
        }
        return series;
    }

    // Each strategy must implement these methods
    @Override
    public abstract String getStrategyName();

    @Override
    public abstract String getAxisLabel();

    @Override
    public abstract boolean getYAxisVisibility();

    @Override
    public abstract Boolean isDollarFormat();

    @Override
    public abstract XYChart.Series<Number, Number> getActualSeries();
}
