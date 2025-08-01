package models;

import javafx.collections.ObservableList;
import utils.RosterUtils;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

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
	private double outgoings;

	private String dateValue;
	private String dateDurationValue;
	private String noOfScriptsValue;
	private String noOfCustomersValue;
	private String noOfItemsValue;
	private String noOfOTCItemsValue;
	private String itemsPerCustomerValue;
	private String otcPerCustomerValue;
	private String dollarPerCustomerValue;
	private String otcDollarPerCustomerValue;
	private String totalIncomeValue;
	private String gpDollarsValue;
	private String gpPercentageValue;
	private String rentAndOutgoingsValue;
	private String wagesValue;
	private String zReportProfitValue;
	private String runningZProfitValue;
	private String tillBalanceValue;
	private String runningTillBalanceValue;
	private String outgoingsValue;


	private double grossProfitDollars;
	private double govtRecovery;
	private double totalGovtContribution;
	public MonthlySummaryDataPoint(LocalDate dayOfMonth, ObservableList<TillReportDataPoint> currentTillReportDataPoints, ObservableList<EODDataPoint> currentEODDataPoints, ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints, RosterUtils rosterUtils, double monthlyRent, double dailyOutgoing, double openDuration, double monthlyWages, double monthlyBuildingOutgoings){
		date = dayOfMonth;
		this.dayDuration = rosterUtils.getDayDuration(date);
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

		rentAndOutgoings = ((monthlyRent+monthlyBuildingOutgoings)/openDuration)*this.dayDuration;
		outgoings = (dailyOutgoing*rosterUtils.getTotalDays())/openDuration*this.dayDuration;
		wages = ((monthlyWages/openDuration)*this.dayDuration);
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

	public MonthlySummaryDataPoint(ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints, boolean totals, double openDuration){
		if(totals){
			dateValue = "Total";
			dateDurationValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getDayDuration).sum());
			noOfScriptsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfScripts).sum());
			noOfCustomersValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfCustomers).sum());
			noOfItemsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfItems).sum());
			noOfOTCItemsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfOTCItems).sum());
			itemsPerCustomerValue = "-";
			otcPerCustomerValue = "-";
			dollarPerCustomerValue = "-";
			otcDollarPerCustomerValue = "-";
			totalIncome = monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTotalIncome).sum();
			totalIncomeValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTotalIncome).sum());
			gpDollarsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getGpDollars).sum());
			gpPercentageValue = "-";
			rentAndOutgoingsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getRentAndOutgoings).sum());
			wagesValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getWages).sum());
			outgoingsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getOutgoings).sum());
			zReportProfitValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getZReportProfit).sum());
			runningZProfitValue = "-";
			tillBalanceValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTillBalance).sum());
			runningTillBalanceValue = "-";
		}else{
			dateValue = "Average";
			dateDurationValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getDayDuration).sum()/openDuration);
			noOfScriptsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfScripts).sum()/openDuration);
			noOfCustomersValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfCustomers).sum()/openDuration);
			noOfItemsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfItems).sum()/openDuration);
			noOfOTCItemsValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getNoOfOTCItems).sum()/openDuration);
			itemsPerCustomerValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getItemsPerCustomer).sum()/openDuration);
			otcPerCustomerValue = String.format("%.2f", monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getOtcPerCustomer).sum()/openDuration);
			dollarPerCustomerValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getDollarPerCustomer).sum()/openDuration);
			otcDollarPerCustomerValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getOtcDollarPerCustomer).sum()/openDuration);
			totalIncome = monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTotalIncome).sum()/openDuration;
			totalIncomeValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTotalIncome).sum()/openDuration);
			gpDollarsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getGpDollars).sum()/openDuration);
			OptionalDouble optional = monthlySummaryPoints.stream()
					.filter(Objects::nonNull) // Ignore null MonthlySummaryDataPoint objects
					.map(MonthlySummaryDataPoint::getGpPercentage)
					.filter(Objects::nonNull) // Ignore null gpPercentage values
					.filter(value -> !value.isNaN() && value != 0) // Ignore NaN or 0 gpPercentage values
					.mapToDouble(Double::doubleValue) // Convert to primitive double
					.average();
			if (optional.isPresent()) {
				gpPercentageValue = String.format("%.2f", optional.getAsDouble()*100) + "%";
			} else {
				gpPercentageValue = "-";
			}
			rentAndOutgoingsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getRentAndOutgoings).sum()/openDuration);
			outgoingsValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getOutgoings).sum()/openDuration);
			wagesValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getWages).sum()/openDuration);
			zReportProfitValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getZReportProfit).sum()/openDuration);
			runningZProfitValue = "-";
			tillBalanceValue = NumberFormat.getCurrencyInstance(Locale.US).format(monthlySummaryPoints.stream().mapToDouble(MonthlySummaryDataPoint::getTillBalance).sum()/openDuration);
			runningTillBalanceValue = "-";
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
		return (dollarPerCustomer == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(dollarPerCustomer);
	}

	public void setDollarPerCustomer(double dollarPerCustomer) {
		this.dollarPerCustomer = dollarPerCustomer;
	}

	public double getOtcDollarPerCustomer() {
		return otcDollarPerCustomer;
	}

	public String getOtcDollarPerCustomerString() {
		return (otcDollarPerCustomer == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(otcDollarPerCustomer);
	}

	public void setOtcDollarPerCustomer(double otcDollarPerCustomer) {
		this.otcDollarPerCustomer = otcDollarPerCustomer;
	}

	public double getTotalIncome() {
		return totalIncome;
	}

	public String getTotalIncomeString() {
		return (totalIncome == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(totalIncome);
	}

	public void setTotalIncome(double totalIncome) {
		this.totalIncome = totalIncome;
	}

	public double getGpDollars() {
		return gpDollars;
	}

	public String getGpDollarsString() {
		return (gpDollars == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(gpDollars);
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
		return (rentAndOutgoings == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(rentAndOutgoings);
	}

	public void setRentAndOutgoings(double rentAndOutgoings) {
		this.rentAndOutgoings = rentAndOutgoings;
	}

	public double getWages() {
		return wages;
	}

	public String getWagesString() {
		return (wages == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(wages);
	}

	public void setWages(double wages) {
		this.wages = wages;
	}

	public double getZReportProfit() {
		return zReportProfit;
	}

	public String getZReportProfitString() {
		return (zReportProfit == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(zReportProfit);
	}

	public void setZReportProfit(double zReportProfit) {
		this.zReportProfit = zReportProfit;
	}

	public double getRunningZProfit() {
		return runningZProfit;
	}

	public String getRunningZProfitString() {
		return (runningZProfit == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(runningZProfit);
	}

	public void setRunningZProfit(double runningZProfit) {
		this.runningZProfit = runningZProfit;
	}

	public double getTillBalance() {
		return tillBalance;
	}

	public String getTillBalanceString() {
		return (tillBalance == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(tillBalance);
	}

	public void setTillBalance(double tillBalance) {
		this.tillBalance = tillBalance;
	}

	public double getRunningTillBalance() {
		return runningTillBalance;
	}

	public String getRunningTillBalanceString() {
		return (runningTillBalance == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(runningTillBalance);
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

	public String getDateValue() {
		return dateValue;
	}

	public String getDateDurationValue() {
		return dateDurationValue;
	}

	public String getNoOfScriptsValue() {
		return noOfScriptsValue;
	}

	public String getNoOfCustomersValue() {
		return noOfCustomersValue;
	}

	public String getNoOfItemsValue() {
		return noOfItemsValue;
	}

	public String getNoOfOTCItemsValue() {
		return noOfOTCItemsValue;
	}

	public String getItemsPerCustomerValue() {
		return itemsPerCustomerValue;
	}

	public String getOtcPerCustomerValue() {
		return otcPerCustomerValue;
	}

	public String getDollarPerCustomerValue() {
		return dollarPerCustomerValue;
	}

	public String getOtcDollarPerCustomerValue() {
		return otcDollarPerCustomerValue;
	}

	public String getTotalIncomeValue() {
		return totalIncomeValue;
	}

	public String getGpDollarsValue() {
		return gpDollarsValue;
	}

	public String getGpPercentageValue() {
		return gpPercentageValue;
	}

	public String getRentAndOutgoingsValue() {
		return rentAndOutgoingsValue;
	}

	public String getWagesValue() {
		return wagesValue;
	}

	public String getZReportProfitValue() {
		return zReportProfitValue;
	}

	public String getRunningZProfitValue() {
		return runningZProfitValue;
	}

	public String getTillBalanceValue() {
		return tillBalanceValue;
	}

	public String getRunningTillBalanceValue() {
		return runningTillBalanceValue;
	}

	public String getOutgoingsValue() {
		return outgoingsValue;
	}
	public void setOutgoingsValue(String outgoingsValue) {
		this.outgoingsValue = outgoingsValue;
	}
	public double getOutgoings() {
		return outgoings;
	}
	public void setOutgoings(double outgoings) {
		this.outgoings = outgoings;
	}

	public String getOutgoingsString() {
		return (outgoings == 0)?"": NumberFormat.getCurrencyInstance(Locale.US).format(outgoings);
	}
}
