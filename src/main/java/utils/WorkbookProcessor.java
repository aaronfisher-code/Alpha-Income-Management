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
import java.util.*;
import java.util.stream.Collectors;



public class WorkbookProcessor {
    private enum WorkbookType {
        DAILY_SCRIPT_TOTALS,
        ORDER_INVOICES_LIST,
        TILL_SUMMARY
    }
    public ArrayList<CellDataPoint> dataPoints = new ArrayList<>();
    private HSSFWorkbook wb;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private WorkbookType workbookType;
    public WorkbookProcessor(HSSFWorkbook wb) {
        this.wb = wb;
        HSSFSheet sheet = wb.getSheetAt(0);

        // Check first if at least one of the rows exists
        if (sheet.getRow(0) == null && sheet.getRow(1) == null && sheet.getRow(7) == null) {
            return;
        }
        // Identify report type
        if (cellContainsText(sheet, 1, 1, "Daily Script Totals")) {
            this.workbookType = WorkbookType.DAILY_SCRIPT_TOTALS;
        } else if (cellContainsText(sheet, 0, 0, "Order Invoices List")) {
            this.workbookType = WorkbookType.ORDER_INVOICES_LIST;
        } else if (cellContainsText(sheet, 7, 1, "Till Summary")) {
            this.workbookType = WorkbookType.TILL_SUMMARY;
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

    private void processTillReport(HSSFSheet sheet) {
        String category = "";

        String tillPeriodCell = sheet.getRow(3).getCell(4).getStringCellValue();
        String dateRange;

        if (tillPeriodCell.contains("(") && tillPeriodCell.contains(")")) {
            int openParen = tillPeriodCell.indexOf('(');
            int closeParen = tillPeriodCell.indexOf(')');
            dateRange = tillPeriodCell.substring(openParen + 1, closeParen);
        } else {
            dateRange = tillPeriodCell;
        }

        String[] dates = dateRange.split(" to ");
        if (dates.length != 2) {
            System.err.println("Error: Unable to parse date range from: " + dateRange);
            return;
        }

        String format = "dd/MM/yyyy HH:mm";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        try {
            periodStart = LocalDateTime.parse(dates[0].trim(), formatter);
            periodEnd = LocalDateTime.parse(dates[1].trim(), formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing dates: " + e.getMessage());
            return;
        }

        //Build column index map to identify the columns of interest
        Map<Integer, Integer> columnCounts = new HashMap<>();

        // Iterate over all rows in the sheet to get counts of each column.
        for (Row row : sheet) {
            if (row.getRowNum() < 7) {
                continue;
            }

            // Determine the bounds for the current row.
            int firstCellNum = row.getFirstCellNum();
            int lastCellNum = row.getLastCellNum();
            String lastCountedValue = null; // used to track the value from the last non-blank cell we counted

            for (int colIndex = firstCellNum; colIndex < lastCellNum; colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    String currentValue = getCellValue(cell).trim();
                    if (lastCountedValue != null && lastCountedValue.equals(currentValue)) {
                        continue;
                    }
                    columnCounts.put(colIndex, columnCounts.getOrDefault(colIndex, 0) + 1);
                    lastCountedValue = currentValue;
                } else {
                    lastCountedValue = null;
                }
            }
        }
        // If there's not enough to make indexes, return
        if (columnCounts.size() < 4) {
            System.err.println("Not enough columns with data to assign all 4 fields.");
            return;  // or handle this situation as needed
        }

        // Get the top 4 columns by non-null count (sorted descending by count)
        List<Integer> topColumns = columnCounts.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(4)
                .map(Map.Entry::getKey).sorted().toList();

        // Assign the columns of interest as follows:
        int categoryIndex   = topColumns.get(0);
        int subcategoryIndex = topColumns.get(1);
        int quantityIndex    = topColumns.get(2);
        int amountIndex      = topColumns.get(3);

        // Process the remaining rows for data points
        for (Row row : sheet) {
            if (row.getRowNum() < 7) {
                continue;
            }
            CellDataPoint cdp = new CellDataPoint();
            cdp.setCategory(category);
            boolean dataCheck = false;

            for(int i = categoryIndex; i < subcategoryIndex; i++){
                if(row.getCell(i) != null){
                    category = row.getCell(i).getStringCellValue();
                    cdp.setCategory(category);
                    break;
                }
            }

            for(int i = subcategoryIndex; i < quantityIndex; i++){
                if(row.getCell(i) != null){
                    cdp.setSubCategory(row.getCell(i).getStringCellValue());
                    break;
                }
            }

            for(int i = quantityIndex; i < amountIndex; i++){
                if(row.getCell(i) != null){
                    cdp.setQuantity(row.getCell(i).getNumericCellValue());
                    dataCheck = true;
                    break;
                }
            }

            for(int i = amountIndex; i < row.getLastCellNum(); i++){
                if(row.getCell(i) != null){
                    cdp.setAmount(row.getCell(i).getNumericCellValue());
                    dataCheck = true;
                    break;
                }
            }


            if (dataCheck) {
                dataPoints.add(cdp);
            }
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
        if(this.workbookType == WorkbookType.TILL_SUMMARY){
            processTillReport(this.wb.getSheetAt(0));
        }else if(this.workbookType == WorkbookType.DAILY_SCRIPT_TOTALS){
            processDailyScriptTotals(this.wb.getSheetAt(0));
        } else if (this.workbookType == WorkbookType.ORDER_INVOICES_LIST) {
            processInvoiceExport(this.wb.getSheetAt(0));
        }
        return dataPoints;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA ->
                // Depending on your needs, you might want to evaluate the formula here.
                    cell.getCellFormula();
            default -> "";
        };
    }

}

