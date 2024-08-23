package models;

import java.sql.ResultSet;
import java.time.LocalDate;

public class BudgetAndExpensesDataPoint {
    private LocalDate date;
    private int storeID;
    private double monthlyRent;
    private double dailyOutgoings;
    private double monthlyLoan;
    private double cpaIncome;
    private double lanternIncome;
    private double otherIncome;
    private double atoGSTrefund;
    private double monthlyWages;


    public BudgetAndExpensesDataPoint(ResultSet resultSet){
        try {
            this.date = resultSet.getDate("date").toLocalDate();
            this.storeID = resultSet.getInt("storeID");
            this.monthlyRent = resultSet.getDouble("monthlyRent");
            this.dailyOutgoings = resultSet.getDouble("dailyOutgoings");
            this.monthlyLoan = resultSet.getDouble("monthlyLoan");
            this.cpaIncome = resultSet.getDouble("6CPAIncome");
            this.lanternIncome = resultSet.getDouble("LanternPayIncome");
            this.otherIncome = resultSet.getDouble("OtherIncome");
            this.atoGSTrefund = resultSet.getDouble("ATO_GST_BAS_Refund");
            this.monthlyWages = resultSet.getDouble("monthlyWages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BudgetAndExpensesDataPoint(){}

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

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public double getDailyOutgoings() {
        return dailyOutgoings;
    }

    public void setDailyOutgoings(double dailyOutgoings) {
        this.dailyOutgoings = dailyOutgoings;
    }

    public double getMonthlyLoan() {
        return monthlyLoan;
    }

    public void setMonthlyLoan(double monthlyLoan) {
        this.monthlyLoan = monthlyLoan;
    }

    public double getCpaIncome() {
        return cpaIncome;
    }

    public void setCpaIncome(double cpaIncome) {
        this.cpaIncome = cpaIncome;
    }

    public double getLanternIncome() {
        return lanternIncome;
    }

    public void setLanternIncome(double lanternIncome) {
        this.lanternIncome = lanternIncome;
    }

    public double getOtherIncome() {
        return otherIncome;
    }

    public void setOtherIncome(double otherIncome) {
        this.otherIncome = otherIncome;
    }

    public double getAtoGSTrefund() {
        return atoGSTrefund;
    }

    public void setAtoGSTrefund(double atoGSTrefund) {
        this.atoGSTrefund = atoGSTrefund;
    }

    public double getMonthlyWages() {
        return monthlyWages;
    }

    public void setMonthlyWages(double monthlyWages) {
        this.monthlyWages = monthlyWages;
    }



}
