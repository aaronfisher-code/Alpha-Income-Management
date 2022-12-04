package utils;

import models.CellDataPoint;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class WorkbookProcessor {

    public ArrayList<CellDataPoint> dataPoints = new ArrayList<>();
    private HSSFWorkbook wb;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    public WorkbookProcessor(HSSFWorkbook wb){
        this.wb = wb;
        //creating a Sheet object to retrieve the object
        HSSFSheet sheet=wb.getSheetAt(0);
        //evaluating cell type
        String category = "";
        System.out.println(sheet.getRow(1).getCell(2).getStringCellValue());
        String[] splitDate= sheet.getRow(3).getCell(4).getStringCellValue().split("\\s+");
        String format = "dd/MM/yyyy HH:mm";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        periodStart = LocalDateTime.parse(splitDate[0]+" "+splitDate[1],formatter);
        periodEnd = LocalDateTime.parse(splitDate[3]+" "+splitDate[4],formatter);
        System.out.println("Period Start: " + periodStart);
        System.out.println("Period End: " + periodEnd);
        for(Row row: sheet)     //iteration over row using for each loop
        {
            if(row.getRowNum()<7){
                continue;
            }
           CellDataPoint cdp = new CellDataPoint();
           boolean dataCheck = false;
            if(row.getCell(1)!=null){
                category = row.getCell(1).getStringCellValue();
                cdp.setCategory(category);
            }else{
                cdp.setCategory(category);
            }

            if(row.getCell(3)!=null){
                cdp.setSubCategory(row.getCell(3).getStringCellValue());
            }

            if(row.getCell(9)!=null){
                cdp.setQuantity(row.getCell(9).getNumericCellValue());
                dataCheck = true;
            }

            for(int i=13;i<17;i++){
                if(row.getCell(i)!=null){
                    cdp.setAmount(row.getCell(i).getNumericCellValue());
                    dataCheck = true;
                    break;
                }
            }

            if(dataCheck)
                dataPoints.add(cdp);
        }
    }
    public LocalDateTime getPeriodStart() {
        return periodStart;
    }
    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }
    public ArrayList<CellDataPoint> getDataPoints() {
        return dataPoints;
    }

}

