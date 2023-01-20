package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EODDataPoint {

	private boolean existsInDB = false;
	private LocalDate date;
	private int storeID;
	private double cashAmount;
	private double eftposAmount;
	private double amexAmount;
	private double googleSquareAmount;
	private double chequeAmount;
	private int medschecks;
	private double stockOnHandAmount;
	private int scriptsOnFile;
	private int smsPatients;
	private double tillBalance;
	private double runningTillBalance;
	private String notes = "";

	public EODDataPoint(ResultSet resultSet) {
		try {
			this.date = resultSet.getDate("date").toLocalDate();
			this.storeID = resultSet.getInt("storeID");
			this.cashAmount = resultSet.getDouble("cash");
			this.eftposAmount = resultSet.getDouble("eftpos");
			this.amexAmount = resultSet.getDouble("amex");
			this.googleSquareAmount = resultSet.getDouble("googleSquare");
			this.chequeAmount = resultSet.getDouble("cheque");
			this.medschecks = resultSet.getInt("medschecks");
			this.scriptsOnFile = resultSet.getInt("scriptsOnFile");
			this.stockOnHandAmount = resultSet.getDouble("stockOnHand");
			this.smsPatients = resultSet.getInt("smsPatients");
			if(resultSet.getString("notes")!=null)
				this.notes = resultSet.getString("notes");
			existsInDB = true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	public EODDataPoint(LocalDate previousDay) {
		date = previousDay;
	}

	public EODDataPoint(boolean existsInDB, LocalDate date, int storeID, double cashAmount, double eftposAmount, double amexAmount, double googleSquareAmount, double chequeAmount, int medschecks, double stockOnHandAmount, int scriptsOnFile, int smsPatients, double tillBalance, double runningTillBalance, String notes) {
		this.existsInDB = existsInDB;
		this.date = date;
		this.storeID = storeID;
		this.cashAmount = cashAmount;
		this.eftposAmount = eftposAmount;
		this.amexAmount = amexAmount;
		this.googleSquareAmount = googleSquareAmount;
		this.chequeAmount = chequeAmount;
		this.medschecks = medschecks;
		this.stockOnHandAmount = stockOnHandAmount;
		this.scriptsOnFile = scriptsOnFile;
		this.smsPatients = smsPatients;
		this.tillBalance = tillBalance;
		this.runningTillBalance = runningTillBalance;
		this.notes = notes;
	}


	public void calculateTillBalances(double totalTakings, double previousRunningTillBalance){
		this.tillBalance = cashAmount+eftposAmount+amexAmount+googleSquareAmount+chequeAmount-totalTakings;
		this.runningTillBalance = previousRunningTillBalance+tillBalance;
	}
	public LocalDate getDate() {return date;}
	public String getDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(date);
	}
	public void setDate(LocalDate date) {this.date = date;}
	public double getCashAmount() {return cashAmount;}
	public String getCashAmountString(){return (cashAmount == 0)?"":NumberFormat.getCurrencyInstance().format(cashAmount);}

	public void setCashAmount(double cashAmount) {this.cashAmount = cashAmount;}
	public double getEftposAmount() {return eftposAmount;}
	public String getEftposAmountString(){return (eftposAmount == 0)?"":NumberFormat.getCurrencyInstance().format(eftposAmount);}
	public void setEftposAmount(double eftposAmount) {this.eftposAmount = eftposAmount;}
	public double getAmexAmount() {return amexAmount;}
	public String getAmexAmountString(){return (amexAmount == 0)?"":NumberFormat.getCurrencyInstance().format(amexAmount);}
	public void setAmexAmount(double amexAmount) {this.amexAmount = amexAmount;}
	public double getGoogleSquareAmount() {return googleSquareAmount;}
	public String getGoogleSquareAmountString(){return (googleSquareAmount == 0)?"":NumberFormat.getCurrencyInstance().format(googleSquareAmount);}
	public void setGoogleSquareAmount(double googleSquareAmount) {this.googleSquareAmount = googleSquareAmount;}
	public double getChequeAmount() {return chequeAmount;}
	public String getChequeAmountString(){return (chequeAmount == 0)?"":NumberFormat.getCurrencyInstance().format(chequeAmount);}
	public void setChequeAmount(double chequeAmount) {this.chequeAmount = chequeAmount;}
	public int getMedschecks() {return medschecks;}
	public String getMedschecksString() {return (medschecks==0)?"":String.valueOf(medschecks);}
	public void setMedschecks(int medschecks) {this.medschecks = medschecks;}
	public double getStockOnHandAmount() {return stockOnHandAmount;}
	public String getStockOnHandAmountString() {return (stockOnHandAmount==0)?"":NumberFormat.getCurrencyInstance().format(stockOnHandAmount);}
	public void setStockOnHandAmount(double stockOnHandAmount) {this.stockOnHandAmount = stockOnHandAmount;}
	public int getScriptsOnFile() {return scriptsOnFile;}
	public String getScriptsOnFileString() {return (scriptsOnFile==0)?"":String.valueOf(scriptsOnFile);}
	public void setScriptsOnFile(int scriptsOnFile) {this.scriptsOnFile = scriptsOnFile;}
	public int getSmsPatients() {return smsPatients;}
	public String getSmsPatientsString() {return (smsPatients==0)?"":String.valueOf(smsPatients);}
	public void setSmsPatients(int smsPatients) {this.smsPatients = smsPatients;}
	public double getTillBalance() {return tillBalance;}
	public String getTillBalanceString(){return NumberFormat.getCurrencyInstance().format(tillBalance);}
	public void setTillBalance(double tillBalance) {this.tillBalance = tillBalance;}
	public double getRunningTillBalance() {return runningTillBalance;}
	public String getRunningTillBalanceString(){return NumberFormat.getCurrencyInstance().format(runningTillBalance);}
	public void setRunningTillBalance(double runningTillBalance) {this.runningTillBalance = runningTillBalance;}
	public String getNotes() {return notes;}
	public void setNotes(String notes) {this.notes = notes;}
	public Boolean isInDB(){return existsInDB;}

}
