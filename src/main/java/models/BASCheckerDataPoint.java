package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class BASCheckerDataPoint {
    private LocalDate date;
    private int storeID;
    private double cashAdjustment;
    private double eftposAdjustment;
    private double amexAdjustment;
    private double googleSquareAdjustment;
    private double chequeAdjustment;
    private double medicareAdjustment;
    private double totalIncomeAdjustment;
    private boolean cashCorrect;
    private boolean eftposCorrect;
    private boolean amexCorrect;
    private boolean googleSquareCorrect;
    private boolean chequeCorrect;
    private boolean medicareCorrect;
    private boolean totalIncomeCorrect;
    private boolean gstCorrect;
    private double basDailyScript;

    public BASCheckerDataPoint(ResultSet resultSet){
        try {
            this.date = resultSet.getDate("date").toLocalDate();
            this.storeID = resultSet.getInt("storeID");
            this.cashAdjustment = resultSet.getDouble("cashAdjustment");
            this.eftposAdjustment = resultSet.getDouble("eftposAdjustment");
            this.amexAdjustment = resultSet.getDouble("amexAdjustment");
            this.googleSquareAdjustment = resultSet.getDouble("googleSquareAdjustment");
            this.chequeAdjustment = resultSet.getDouble("chequesAdjustment");
            this.medicareAdjustment = resultSet.getDouble("medicareAdjustment");
            this.totalIncomeAdjustment = resultSet.getDouble("totalIncomeAdjustment");
            this.cashCorrect = resultSet.getBoolean("cashCorrect");
            this.eftposCorrect = resultSet.getBoolean("eftposCorrect");
            this.amexCorrect = resultSet.getBoolean("amexCorrect");
            this.googleSquareCorrect = resultSet.getBoolean("googleSquareCorrect");
            this.chequeCorrect = resultSet.getBoolean("chequesCorrect");
            this.medicareCorrect = resultSet.getBoolean("medicareCorrect");
            this.totalIncomeCorrect = resultSet.getBoolean("totalIncomeCorrect");
            this.gstCorrect = resultSet.getBoolean("gstCorrect");
            this.basDailyScript = resultSet.getDouble("basDailyScript");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BASCheckerDataPoint(){}

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

    public double getCashAdjustment() {
        return cashAdjustment;
    }

    public void setCashAdjustment(double cashAdjustment) {
        this.cashAdjustment = cashAdjustment;
    }

    public double getEftposAdjustment() {
        return eftposAdjustment;
    }

    public void setEftposAdjustment(double eftposAdjustment) {
        this.eftposAdjustment = eftposAdjustment;
    }

    public double getAmexAdjustment() {
        return amexAdjustment;
    }

    public void setAmexAdjustment(double amexAdjustment) {
        this.amexAdjustment = amexAdjustment;
    }

    public double getGoogleSquareAdjustment() {
        return googleSquareAdjustment;
    }

    public void setGoogleSquareAdjustment(double googleSquareAdjustment) {
        this.googleSquareAdjustment = googleSquareAdjustment;
    }

    public double getChequeAdjustment() {
        return chequeAdjustment;
    }

    public void setChequeAdjustment(double chequeAdjustment) {
        this.chequeAdjustment = chequeAdjustment;
    }

    public double getMedicareAdjustment() {
        return medicareAdjustment;
    }

    public void setMedicareAdjustment(double medicareAdjustment) {
        this.medicareAdjustment = medicareAdjustment;
    }

    public double getTotalIncomeAdjustment() {
        return totalIncomeAdjustment;
    }

    public void setTotalIncomeAdjustment(double totalIncomeAdjustment) {
        this.totalIncomeAdjustment = totalIncomeAdjustment;
    }

    public boolean isCashCorrect() {
        return cashCorrect;
    }

    public void setCashCorrect(boolean cashCorrect) {
        this.cashCorrect = cashCorrect;
    }

    public boolean isEftposCorrect() {
        return eftposCorrect;
    }

    public void setEftposCorrect(boolean eftposCorrect) {
        this.eftposCorrect = eftposCorrect;
    }

    public boolean isAmexCorrect() {
        return amexCorrect;
    }

    public void setAmexCorrect(boolean amexCorrect) {
        this.amexCorrect = amexCorrect;
    }

    public boolean isGoogleSquareCorrect() {
        return googleSquareCorrect;
    }

    public void setGoogleSquareCorrect(boolean googleSquareCorrect) {
        this.googleSquareCorrect = googleSquareCorrect;
    }

    public boolean isChequeCorrect() {
        return chequeCorrect;
    }

    public void setChequeCorrect(boolean chequeCorrect) {
        this.chequeCorrect = chequeCorrect;
    }

    public boolean isMedicareCorrect() {
        return medicareCorrect;
    }

    public void setMedicareCorrect(boolean medicareCorrect) {
        this.medicareCorrect = medicareCorrect;
    }

    public boolean isTotalIncomeCorrect() {
        return totalIncomeCorrect;
    }

    public void setTotalIncomeCorrect(boolean totalIncomeCorrect) {
        this.totalIncomeCorrect = totalIncomeCorrect;
    }

    public boolean isGstCorrect() {
        return gstCorrect;
    }

    public void setGstCorrect(boolean gstCorrect) {
        this.gstCorrect = gstCorrect;
    }

    public double getBasDailyScript() {
        return basDailyScript;
    }

    public void setBasDailyScript(double basDailyScript) {
        this.basDailyScript = basDailyScript;
    }

}
