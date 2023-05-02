package models;

import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Credit {

    private int creditID;
    private String supplierName;

    private int supplierID;

    private int storeID;
    private String creditNo;
    private LocalDate creditDate;
    private String referenceInvoiceNo;
    private double creditAmount;
    private String notes;
    //TODO: make sure all notes fields dont extend past 255 character SQL limit

    public Credit(ResultSet resultSet){
        try {
            this.creditID= resultSet.getInt("idCredits");
            this.supplierID = resultSet.getInt("supplierID");
            this.storeID = resultSet.getInt("storeID");
            this.supplierName = resultSet.getString("supplierName");
            this.creditNo = resultSet.getString("creditNo");
            this.creditDate = resultSet.getDate("creditDate").toLocalDate();
            this.referenceInvoiceNo = resultSet.getString("referenceInvoiceNo");
            this.creditAmount = resultSet.getDouble("creditAmount");
            this.notes = resultSet.getString("notes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSupplierID() {
        return supplierID;
    }

    public void setSupplierID(int supplierID) {
        this.supplierID = supplierID;
    }

    public String getCreditNo() {
        return creditNo;
    }

    public void setCreditNo(String creditNo) {
        this.creditNo = creditNo;
    }

    public LocalDate getCreditDate() {
        return creditDate;
    }

    public void setCreditDate(LocalDate creditDate) {
        this.creditDate = creditDate;
    }

    public String getCreditDateString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatter.format(creditDate);
    }

    public String getReferenceInvoiceNo() {
        return referenceInvoiceNo;
    }

    public void setReferenceInvoiceNo(String referenceInvoiceNo) {
        this.referenceInvoiceNo = referenceInvoiceNo;
    }

    public double getCreditAmount() {
        return creditAmount;
    }

    public String getCreditAmountString(){return (NumberFormat.getCurrencyInstance().format(creditAmount));}

    public void setCreditAmount(double creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getCreditID() {
        return creditID;
    }

    public void setCreditID(int creditID) {
        this.creditID = creditID;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

}
