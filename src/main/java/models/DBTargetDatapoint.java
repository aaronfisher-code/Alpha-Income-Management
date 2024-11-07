package models;

import java.time.LocalDate;

public class DBTargetDatapoint {
    private LocalDate date;
    private int storeID;
    private String targetName;
    private double target1Growth;
    private double target1Actual;
    private boolean useTarget1Growth;
    private double target2Growth;
    private double target2Actual;
    private boolean useTarget2Growth;

    public DBTargetDatapoint() {}

    public DBTargetDatapoint(LocalDate date, int storeID, String targetName, double target1growth, double target1Actual, boolean useTarget1Growth, double target2growth, double target2actual, boolean useTarget2Growth) {
        this.date = date;
        this.storeID = storeID;
        this.targetName = targetName;
        this.target1Growth = target1growth;
        this.target1Actual = target1Actual;
        this.useTarget1Growth = useTarget1Growth;
        this.target2Growth = target2growth;
        this.target2Actual = target2actual;
        this.useTarget2Growth = useTarget2Growth;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getStoreID() {
        return storeID;
    }

    public void setStoreID(int storeID) {
        this.storeID = storeID;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public double getTarget1Growth() {
        return target1Growth;
    }

    public void setTarget1Growth(double target1Growth) {
        this.target1Growth = target1Growth;
    }

    public double getTarget1Actual() {
        return target1Actual;
    }

    public void setTarget1Actual(double target1Actual) {
        this.target1Actual = target1Actual;
    }

    public boolean isUseTarget1Growth() {
        return useTarget1Growth;
    }

    public void setUseTarget1Growth(boolean useTarget1Growth) {
        this.useTarget1Growth = useTarget1Growth;
    }

    public double getTarget2Growth() {
        return target2Growth;
    }

    public void setTarget2Growth(double target2Growth) {
        this.target2Growth = target2Growth;
    }

    public double getTarget2Actual() {
        return target2Actual;
    }

    public void setTarget2Actual(double target2Actual) {
        this.target2Actual = target2Actual;
    }

    public boolean isUseTarget2Growth() {
        return useTarget2Growth;
    }

    public void setUseTarget2Growth(boolean useTarget2Growth) {
        this.useTarget2Growth = useTarget2Growth;
    }

    @Override
    public String toString() {
        return "DBTargetDatapoint{" +
                "date=" + date +
                ", storeID=" + storeID +
                ", targetName='" + targetName + '\'' +
                ", target1Growth=" + target1Growth +
                ", target1Actual=" + target1Actual +
                ", useTarget1Growth=" + useTarget1Growth +
                ", target2Growth=" + target2Growth +
                ", target2Actual=" + target2Actual +
                ", useTarget2Growth=" + useTarget2Growth +
                '}';
    }
}
