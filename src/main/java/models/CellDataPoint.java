package models;

import java.time.LocalDate;

public class CellDataPoint {

    private LocalDate assignedDate;
    private String category;
    private String subCategory;
    private double quantity;
    private double amount;

    public CellDataPoint() {
    }

    public CellDataPoint(String category, String subCategory, double quantity, double amount) {
        this.category = category;
        this.subCategory = subCategory;
        this.quantity = quantity;
        this.amount = amount;
    }

    public CellDataPoint(LocalDate assignedDate,String category, String subCategory, double quantity, double amount) {
        this.assignedDate = assignedDate;
        this.category = category;
        this.subCategory = subCategory;
        this.quantity = quantity;
        this.amount = amount;
    }

    public LocalDate getAssignedDate() {return assignedDate;}

    public void setAssignedDate(LocalDate assignedDate) {this.assignedDate = assignedDate;}

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
