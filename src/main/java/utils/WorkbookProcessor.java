package utils;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;
import java.util.Iterator;

public class WorkbookProcessor {

    private HSSFWorkbook wb;
    public WorkbookProcessor(HSSFWorkbook wb){
        this.wb = wb;
        //creating a Sheet object to retrieve the object
        HSSFSheet sheet=wb.getSheetAt(0);
        ArrayList<CellDataPoint> dataPoints = new ArrayList<>();
        //evaluating cell type
        String category = "";
        for(Row row: sheet)     //iteration over row using for each loop
        {
           CellDataPoint cdp = new CellDataPoint();
           boolean dataCheck = false;
            if(row.getCell(1)!=null){
                category = row.getCell(1).getStringCellValue();
            }else{
                category = "";
            }

            if(row.getCell(3)!=null){
                cdp.setSubCategory(row.getCell(3).getStringCellValue());
                cdp.setCategory(category);
            }

            if(row.getCell(9)!=null){
                cdp.setQuantity(row.getCell(9).getNumericCellValue());
                dataCheck = true;
            }

            if(row.getCell(14)!=null){
                cdp.setAmount(row.getCell(14).getNumericCellValue());
                dataCheck = true;
            }
            if((cdp.getCategory()==null && cdp.getSubCategory()==null) ||
                (cdp.getCategory().equals("") && cdp.getSubCategory().equals(""))){
                dataCheck= false;
            }
            if(dataCheck)
                dataPoints.add(cdp);
        }

        for(CellDataPoint c: dataPoints){
            if(c.getCategory()!=""){System.out.print(c.getCategory()+",");}else{System.out.print("\t");}
            if(c.getSubCategory()!="")System.out.print(c.getSubCategory()+",");
            System.out.print(c.getQuantity()+",");
            System.out.println(c.getAmount());
        }
    }

    private void printCell(Cell cell){
        FormulaEvaluator formulaEvaluator=wb.getCreationHelper().createFormulaEvaluator();
        switch(formulaEvaluator.evaluateInCell(cell).getCellType())
        {
            case NUMERIC:   //field that represents numeric cell type
                System.out.print(cell.getNumericCellValue()+ "\t\t");
                break;
            case STRING:
                System.out.print(cell.getStringCellValue()+ "\t\t");
                break;
        }
    }
}

class CellDataPoint{

    private String category;
    private String subCategory;
    private double quantity;
    private double amount;

    public CellDataPoint(){}

    public CellDataPoint(String category,String subCategory,double quantity, double amount){
        this.category = category;
        this.subCategory = subCategory;
        this.quantity = quantity;
        this.amount = amount;
    }

    public String getCategory() {return category;}
    public void setCategory(String category) {this.category = category;}
    public String getSubCategory() {return subCategory;}
    public void setSubCategory(String subCategory) {this.subCategory = subCategory;}
    public double getQuantity() {return quantity;}
    public void setQuantity(double quantity) {this.quantity = quantity;}
    public double getAmount() {return amount;}
    public void setAmount(double amount) {this.amount = amount;}

}
