package models;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    public Credit() {
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

    public String getCreditAmountString(){return (NumberFormat.getCurrencyInstance(Locale.US).format(creditAmount));}

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

    public int getStoreID() {
        return storeID;
    }

    public void setStoreID(int storeID) {
        this.storeID = storeID;
    }
}
