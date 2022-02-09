package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class EODDataPoint {

	private LocalDate date;
	private double cashAmount;
	private double eftposAmount;
	private double amexAmount;
	private double googleSquareAmount;
	private double chequeAmount;
	private int clinicalInterventions;
	private int medschecks;
	private int stockOnHandAmount;
	private int scriptsOnFile;
	private int smsPatients;
	private double tillBalance;
	private double runningTillBalance;
	private String notes;

	public EODDataPoint(ResultSet resultSet) {
	}

	public LocalDate getDate() {return date;}
	public void setDate(LocalDate date) {this.date = date;}
	public double getCashAmount() {return cashAmount;}
	public void setCashAmount(double cashAmount) {this.cashAmount = cashAmount;}
	public double getEftposAmount() {return eftposAmount;}
	public void setEftposAmount(double eftposAmount) {this.eftposAmount = eftposAmount;}
	public double getAmexAmount() {return amexAmount;}
	public void setAmexAmount(double amexAmount) {this.amexAmount = amexAmount;}
	public double getGoogleSquareAmount() {return googleSquareAmount;}
	public void setGoogleSquareAmount(double googleSquareAmount) {this.googleSquareAmount = googleSquareAmount;}
	public double getChequeAmount() {return chequeAmount;}
	public void setChequeAmount(double chequeAmount) {this.chequeAmount = chequeAmount;}
	public int getClinicalInterventions() {return clinicalInterventions;}
	public void setClinicalInterventions(int clinicalInterventions) {this.clinicalInterventions = clinicalInterventions;}
	public int getMedschecks() {return medschecks;}
	public void setMedschecks(int medschecks) {this.medschecks = medschecks;}
	public int getStockOnHandAmount() {return stockOnHandAmount;}
	public void setStockOnHandAmount(int stockOnHandAmount) {this.stockOnHandAmount = stockOnHandAmount;}
	public int getScriptsOnFile() {return scriptsOnFile;}
	public void setScriptsOnFile(int scriptsOnFile) {this.scriptsOnFile = scriptsOnFile;}
	public int getSmsPatients() {return smsPatients;}
	public void setSmsPatients(int smsPatients) {this.smsPatients = smsPatients;}
	public double getTillBalance() {return tillBalance;}
	public void setTillBalance(double tillBalance) {this.tillBalance = tillBalance;}
	public double getRunningTillBalance() {return runningTillBalance;}
	public void setRunningTillBalance(double runningTillBalance) {this.runningTillBalance = runningTillBalance;}
	public String getNotes() {return notes;}
	public void setNotes(String notes) {this.notes = notes;}

}
