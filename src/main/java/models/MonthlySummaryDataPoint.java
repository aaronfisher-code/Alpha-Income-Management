package models;

import javafx.collections.ObservableList;
import utils.RosterUtils;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MonthlySummaryDataPoint {

	private LocalDate date;
	private double dayDuration;
	private double noOfScripts;
	private double noOfCustomers;
	private double noOfItems;
	private double noOfOTCItems;
	private double itemsPerCustomer;
	private double otcPerCustomer;
	private double dollarPerCustomer;
	private double otcDollarPerCustomer;
	private double totalIncome;
	private double gpDollars;
	private double gpPercentage;
	private double rentAndOutgoings;
	private double wages;
	private double zReportProfit;
	private double runningZProfit;
	private double tillBalance;
	private double runningTillBalance;

	private double grossProfitDollars;
	private double govtRecovery;
	private double totalGovtContribution;
	public MonthlySummaryDataPoint(LocalDate dayOfMonth, ObservableList<TillReportDataPoint> currentTillReportDataPoints, ObservableList<EODDataPoint> currentEODDataPoints, ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints, double dayDuration) {
		date = dayOfMonth;
		this.dayDuration = dayDuration;
		double totalTakings = 0;
		for(TillReportDataPoint t:currentTillReportDataPoints){
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Script Count"))
				noOfScripts = t.getQuantity();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Total Customers Served"))
				noOfCustomers = t.getQuantity();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Total Sales"))
				noOfItems = t.getQuantity();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Total Sales-OTC Sales"))
				noOfOTCItems = t.getQuantity();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Avg. OTC Sales Per Customer"))
				otcDollarPerCustomer = t.getAmount();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Govt Recovery"))
				govtRecovery = t.getAmount();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Gross Profit ($)"))
				grossProfitDollars = t.getAmount();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Total Government Contribution"))
				totalGovtContribution = t.getAmount();
			if(t.getAssignedDate().equals(date)&&t.getKey().equals("Total Takings"))
				totalTakings = t.getAmount();
		}
		for(EODDataPoint e: currentEODDataPoints){
			if(e.getDate().equals(date)){
				totalIncome=e.getCashAmount()
						+e.getAmexAmount()
						+e.getChequeAmount()
						+e.getEftposAmount()
						+e.getGoogleSquareAmount()
						+govtRecovery;
				tillBalance = e.getCashAmount()
						+e.getAmexAmount()
						+e.getChequeAmount()
						+e.getEftposAmount()
						+e.getGoogleSquareAmount()
						-totalTakings;
			}
		}
		itemsPerCustomer = (noOfCustomers==0)?0:noOfItems/noOfCustomers;
		otcPerCustomer = (noOfCustomers==0)?0:noOfOTCItems/noOfCustomers;
		dollarPerCustomer = (noOfCustomers==0)?0:totalIncome/noOfCustomers;
		gpDollars = grossProfitDollars+govtRecovery-totalGovtContribution;
		gpPercentage = gpDollars/totalIncome;

		rentAndOutgoings = 0; //TODO: Get this from equivalent of budget and expenses
		wages = 0; //TODO: Get this from wage calculator
		zReportProfit = gpDollars-rentAndOutgoings-wages;
		runningZProfit = 0;
		runningTillBalance = 0;
		runningZProfit += zReportProfit;
		runningTillBalance += tillBalance;
		for(MonthlySummaryDataPoint m: monthlySummaryPoints){
			runningZProfit += m.getZReportProfit();
			runningTillBalance += m.getTillBalance();
		}
	}

	public LocalDate getDate() {
		return date;
	}

	public String getDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(date);
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public double getDayDuration() {
		return dayDuration;
	}

	public void setDayDuration(double dayDuration) {
		this.dayDuration = dayDuration;
	}

	public double getNoOfScripts() {
		return noOfScripts;
	}

	public String getNoOfScriptsString() {
		return (noOfScripts == 0)?"": String.valueOf((int)noOfScripts);
	}

	public void setNoOfScripts(double noOfScripts) {
		this.noOfScripts = noOfScripts;
	}

	public double getNoOfCustomers() {
		return noOfCustomers;
	}

	public String getNoOfCustomersString() {
		return (noOfCustomers == 0)?"": String.valueOf((int)noOfCustomers);
	}

	public void setNoOfCustomers(double noOfCustomers) {
		this.noOfCustomers = noOfCustomers;
	}

	public double getNoOfItems() {
		return noOfItems;
	}

	public String getNoOfItemsString() {
		return (noOfItems == 0)?"": String.valueOf((int)noOfItems);
	}

	public void setNoOfItems(double noOfItems) {
		this.noOfItems = noOfItems;
	}

	public double getNoOfOTCItems() {
		return noOfOTCItems;
	}

	public String getNoOfOTCItemsString() {
		return (noOfOTCItems == 0)?"": String.valueOf((int)noOfOTCItems);
	}

	public void setNoOfOTCItems(double noOfOTCItems) {
		this.noOfOTCItems = noOfOTCItems;
	}

	public double getItemsPerCustomer() {
		return itemsPerCustomer;
	}

	public String getItemsPerCustomerString() {
		return (itemsPerCustomer == 0)?"": String.format("%.2f", itemsPerCustomer);
	}

	public void setItemsPerCustomer(double itemsPerCustomer) {
		this.itemsPerCustomer = itemsPerCustomer;
	}

	public double getOtcPerCustomer() {
		return otcPerCustomer;
	}

	public String getOtcPerCustomerString() {
		return (otcPerCustomer == 0)?"": String.format("%.2f", otcPerCustomer);
	}

	public void setOtcPerCustomer(double otcPerCustomer) {
		this.otcPerCustomer = otcPerCustomer;
	}

	public double getDollarPerCustomer() {
		return dollarPerCustomer;
	}

	public String getDollarPerCustomerString() {
		return (dollarPerCustomer == 0)?"": NumberFormat.getCurrencyInstance().format(dollarPerCustomer);
	}

	public void setDollarPerCustomer(double dollarPerCustomer) {
		this.dollarPerCustomer = dollarPerCustomer;
	}

	public double getOtcDollarPerCustomer() {
		return otcDollarPerCustomer;
	}

	public String getOtcDollarPerCustomerString() {
		return (otcDollarPerCustomer == 0)?"": NumberFormat.getCurrencyInstance().format(otcDollarPerCustomer);
	}

	public void setOtcDollarPerCustomer(double otcDollarPerCustomer) {
		this.otcDollarPerCustomer = otcDollarPerCustomer;
	}

	public double getTotalIncome() {
		return totalIncome;
	}

	public String getTotalIncomeString() {
		return (totalIncome == 0)?"": NumberFormat.getCurrencyInstance().format(totalIncome);
	}

	public void setTotalIncome(double totalIncome) {
		this.totalIncome = totalIncome;
	}

	public double getGpDollars() {
		return gpDollars;
	}

	public String getGpDollarsString() {
		return (gpDollars == 0)?"": NumberFormat.getCurrencyInstance().format(gpDollars);
	}

	public void setGpDollars(double gpDollars) {
		this.gpDollars = gpDollars;
	}

	public double getGpPercentage() {
		return gpPercentage;
	}

	public String getGpPercentageString() {
		return (Double.isNaN(gpPercentage) || gpPercentage == 0)?"": String.format("%.2f", gpPercentage*100)+"%";
	}

	public void setGpPercentage(double gpPercentage) {
		this.gpPercentage = gpPercentage;
	}

	public double getRentAndOutgoings() {
		return rentAndOutgoings;
	}

	public String getRentAndOutgoingsString() {
		return (rentAndOutgoings == 0)?"": NumberFormat.getCurrencyInstance().format(rentAndOutgoings);
	}

	public void setRentAndOutgoings(double rentAndOutgoings) {
		this.rentAndOutgoings = rentAndOutgoings;
	}

	public double getWages() {
		return wages;
	}

	public String getWagesString() {
		return (wages == 0)?"": NumberFormat.getCurrencyInstance().format(wages);
	}

	public void setWages(double wages) {
		this.wages = wages;
	}

	public double getZReportProfit() {
		return zReportProfit;
	}

	public String getZReportProfitString() {
		return (zReportProfit == 0)?"": NumberFormat.getCurrencyInstance().format(zReportProfit);
	}

	public void setZReportProfit(double zReportProfit) {
		this.zReportProfit = zReportProfit;
	}

	public double getRunningZProfit() {
		return runningZProfit;
	}

	public String getRunningZProfitString() {
		return (runningZProfit == 0)?"": NumberFormat.getCurrencyInstance().format(runningZProfit);
	}

	public void setRunningZProfit(double runningZProfit) {
		this.runningZProfit = runningZProfit;
	}

	public double getTillBalance() {
		return tillBalance;
	}

	public String getTillBalanceString() {
		return (tillBalance == 0)?"": NumberFormat.getCurrencyInstance().format(tillBalance);
	}

	public void setTillBalance(double tillBalance) {
		this.tillBalance = tillBalance;
	}

	public double getRunningTillBalance() {
		return runningTillBalance;
	}

	public String getRunningTillBalanceString() {
		return (runningTillBalance == 0)?"": NumberFormat.getCurrencyInstance().format(runningTillBalance);
	}

	public void setRunningTillBalance(double runningTillBalance) {
		this.runningTillBalance = runningTillBalance;
	}

	public double getGrossProfitDollars() {
		return grossProfitDollars;
	}

	public void setGrossProfitDollars(double grossProfitDollars) {
		this.grossProfitDollars = grossProfitDollars;
	}

	public double getGovtRecovery() {
		return govtRecovery;
	}

	public void setGovtRecovery(double govtRecovery) {
		this.govtRecovery = govtRecovery;
	}

	public double getTotalGovtContribution() {
		return totalGovtContribution;
	}

	public void setTotalGovtContribution(double totalGovtContribution) {
		this.totalGovtContribution = totalGovtContribution;
	}
}
