package strategies;

import application.Main;
import controllers.TargetGraphsPageController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import models.EODDataPoint;
import utils.RosterUtils;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ScriptsOnFileStrategy implements LineGraphTargetStrategy {

    public static final boolean SHOW_Y_AXIS = true;
    public static final String GRAPH_TITLE = "Scripts on File";
    public static final String AXIS_LABEL = "No. of scripts on file";

    public static final boolean DOLLAR_FORMAT = false;

    public int length;

    public double target1;
    public double target2;
    public LocalDate startDate;
    public LocalDate endDate;

    public TargetGraphsPageController parent;

    public Connection con;

    public PreparedStatement preparedStatement;

    public Main main;

    public ResultSet resultSet;
    private ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
    private RosterUtils rosterUtils;

    public ScriptsOnFileStrategy(LocalDate startDate, LocalDate endDate, TargetGraphsPageController parent) {
        this.length = (int) ChronoUnit.DAYS.between(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.parent = parent;
        this.con = parent.getConnection();
        this.preparedStatement = parent.getPreparedStatement();
        this.main = parent.getMain();
        this.resultSet = parent.getResultSet();
        this.rosterUtils = new RosterUtils(con, main, startDate, endDate);

        String sql;
        try {
            sql = "SELECT * FROM eoddatapoints where storeID = ? AND date>=? AND date<=?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                currentEODDataPoints.add(new EODDataPoint(resultSet));
            }
            sql = "SELECT * FROM targets where storeID = ? AND targetName = ?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setString(2, "ScriptsOnFile");
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                target1 = resultSet.getDouble("target1");
                target2 = resultSet.getDouble("target2");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
        int currentValue = 0;
        for(int i=0;i<Math.min(dayDifference,length);i++){
            LocalDate date = startDate.plusDays(i);
            //find current till report data point with date
            for(int j=0;j<currentEODDataPoints.size();j++){
                if(currentEODDataPoints.get(j).getDate().equals(date)){
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
