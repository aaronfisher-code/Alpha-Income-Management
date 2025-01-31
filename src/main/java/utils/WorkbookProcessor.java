package utils;

import models.CellDataPoint;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;

public class WorkbookProcessor {

    public ArrayList<CellDataPoint> dataPoints = new ArrayList<>();
    private HSSFWorkbook wb;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    public WorkbookProcessor(HSSFWorkbook wb) {
        this.wb = wb;
        HSSFSheet sheet = wb.getSheetAt(0);

        // Check first if at least one of the rows exists
        if (sheet.getRow(0) == null && sheet.getRow(1) == null && sheet.getRow(7) == null) {
            System.out.println("Error: No valid rows found in the sheet.");
            return;
        }
        // Identify report type
        if (cellContainsText(sheet, 1, 1, "Daily Script Totals")) {
            System.out.println("Identified as Daily script totals report");
            processDailyScriptTotals(sheet);
        } else if (cellContainsText(sheet, 0, 0, "Order Invoices List")) {
            System.out.println("Identified as Invoice export report");
            processInvoiceExport(sheet);
        } else if (cellContainsText(sheet, 7, 1, "Till Summary")) {
            System.out.println("Identified as Till report");
            processTillReport(sheet);
        }else{
            throw new IllegalArgumentException("Invalid report type");
        }
    }

    // Helper method to check if a cell contains a specific text
    private boolean cellContainsText(HSSFSheet sheet, int rowIdx, int colIdx, String expectedText) {
        Row row = sheet.getRow(rowIdx);
        if (row != null) {
            Cell cell = row.getCell(colIdx);
            if (cell != null) {
                try {
                    return cell.getStringCellValue().trim().equalsIgnoreCase(expectedText);
                } catch (IllegalStateException e) {
                    return false; // Cell might be numeric or date type
                }
            }
        }
        return false;
    }

    private void processTillReport(HSSFSheet sheet){
        String category = "";
        System.out.println(sheet.getRow(1).getCell(2).getStringCellValue());
        String[] splitDate= sheet.getRow(3).getCell(4).getStringCellValue().split("\\s+");
        String format = "dd/MM/yyyy HH:mm";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        periodStart = LocalDateTime.parse(splitDate[0]+" "+splitDate[1],formatter);
        periodEnd = LocalDateTime.parse(splitDate[3]+" "+splitDate[4],formatter);
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

    private void processDailyScriptTotals(HSSFSheet sheet){
        for(Row row: sheet)     //iteration over row using for each loop
        {
            if(row.getRowNum()<16){ //skip to row 16
                continue;
            }
            if(row.getCell(1)!=null) {
                try{
                    Date rawDate = row.getCell(1).getDateCellValue();
                    LocalDate date = rawDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                    CellDataPoint scriptTotalsPoint = new CellDataPoint();
                    scriptTotalsPoint.setAssignedDate(date);
                    scriptTotalsPoint.setCategory("Script Count");
                    scriptTotalsPoint.setSubCategory("");
                    scriptTotalsPoint.setQuantity(row.getCell(5).getNumericCellValue());
                    dataPoints.add(scriptTotalsPoint);

                    CellDataPoint govtRecoveryPoint = new CellDataPoint();
                    govtRecoveryPoint.setAssignedDate(date);
                    govtRecoveryPoint.setCategory("Govt Recovery");
                    govtRecoveryPoint.setSubCategory("");
                    govtRecoveryPoint.setAmount(row.getCell(14).getNumericCellValue());
                    dataPoints.add(govtRecoveryPoint);
                }catch(IllegalStateException e){}
            }
        }
    }

    private void processInvoiceExport(HSSFSheet sheet){
        for(Row row: sheet)     //iteration over row using for each loop
        {
            if(row.getRowNum()<2){//Skip to invoices
                continue;
            }
            if(row.getCell(1)!=null) {
                try{
                    String invoiceNumber = row.getCell(0).getStringCellValue();
                    String invoiceAmountString = row.getCell(8).getStringCellValue();
                    String formattedAmount = invoiceAmountString.replaceAll("[$,]", "");
                    Double actualAmount = Double.parseDouble(formattedAmount);
                    CellDataPoint invoiceDataPoint = new CellDataPoint();
                    invoiceDataPoint.setCategory(invoiceNumber);
                    invoiceDataPoint.setSubCategory("");
                    invoiceDataPoint.setAmount(actualAmount);
                    dataPoints.add(invoiceDataPoint);
                }catch(IllegalStateException e){
                    e.printStackTrace();
                }
            }
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

