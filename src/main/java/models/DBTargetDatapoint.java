package models;

import java.time.LocalDate;

public class DBTargetDatapoint {
    private LocalDate date;
    private int storeID;
    private String targetName;
    private double target1growth;
    private double target1actual;
    private boolean useTarget1Growth;
    private double target2growth;
    private double target2actual;
    private boolean useTarget2Growth;

    public DBTargetDatapoint(LocalDate date,int storeID, String targetName, double target1growth, double target1actual, boolean useTarget1Growth, double target2growth, double target2actual, boolean useTarget2Growth) {
        this.date = date;
        this.storeID = storeID;
        this.targetName = targetName;
        this.target1growth = target1growth;
        this.target1actual = target1actual;
        this.useTarget1Growth = useTarget1Growth;
        this.target2growth = target2growth;
        this.target2actual = target2actual;
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

    public double getTarget1growth() {
        return target1growth;
    }

    public void setTarget1growth(double target1growth) {
        this.target1growth = target1growth;
    }

    public double getTarget1actual() {
        return target1actual;
    }

    public void setTarget1actual(double target1actual) {
        this.target1actual = target1actual;
    }

    public boolean isUseTarget1Growth() {
        return useTarget1Growth;
    }

    public void setUseTarget1Growth(boolean useTarget1Growth) {
        this.useTarget1Growth = useTarget1Growth;
    }

    public double getTarget2growth() {
        return target2growth;
    }

    public void setTarget2growth(double target2growth) {
        this.target2growth = target2growth;
    }

    public double getTarget2actual() {
        return target2actual;
    }

    public void setTarget2actual(double target2actual) {
        this.target2actual = target2actual;
    }

    public boolean isUseTarget2Growth() {
        return useTarget2Growth;
    }

    public void setUseTarget2Growth(boolean useTarget2Growth) {
        this.useTarget2Growth = useTarget2Growth;
    }
}
