package utils;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

public class LegacyImportTool {

    public LegacyImportTool(XSSFWorkbook wb) {
        //creating a Sheet object to retrieve the object
        XSSFSheet sheet = wb.getSheet("EOD");
        int month = (int) sheet.getRow(0).getCell(0).getNumericCellValue();
        int year = (int) sheet.getRow(1).getCell(0).getNumericCellValue();
        System.out.println("Currently importing: " + Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
        YearMonth yearMonthObject = YearMonth.of(year, month);
        int daysInMonth = yearMonthObject.lengthOfMonth();

        //Pull EOD values
        for(int i=0;i<daysInMonth;i++){
            double cashAmount = sheet.getRow(i+1).getCell(2).getNumericCellValue();
            double eftposAmount = sheet.getRow(i+1).getCell(3).getNumericCellValue();
            double amexAmount = sheet.getRow(i+1).getCell(4).getNumericCellValue();
            double googleSquareAmount = sheet.getRow(i+1).getCell(5).getNumericCellValue();
            double chequeAmount = sheet.getRow(i+1).getCell(6).getNumericCellValue();
            double clinicalInterventions = sheet.getRow(i+1).getCell(7).getNumericCellValue();
            double medschecks = sheet.getRow(i+1).getCell(8).getNumericCellValue();
            double stockOnHand = sheet.getRow(i+1).getCell(9).getNumericCellValue();
            double scriptsOnFile = sheet.getRow(i+1).getCell(10).getNumericCellValue();
            double smsPatients = sheet.getRow(i+1).getCell(11).getNumericCellValue();
            String notes = sheet.getRow(i+1).getCell(14).getStringCellValue();
            System.out.println("Day: " + (i+1));
            System.out.println("Cash: " + cashAmount + " Eftpos: " + eftposAmount + " Amex: " + amexAmount + " Google Square: " + googleSquareAmount + " Cheque: " + chequeAmount + " Clinical Interventions: " + clinicalInterventions + " Medschecks: " + medschecks + " Stock on hand: " + stockOnHand + " Scripts on file: " + scriptsOnFile + " SMS Patients: " + smsPatients + " Notes: " + notes);
        }

        sheet = wb.getSheet("Till Reports");
        //Pull Tillreport values
        for(int i=0;i<daysInMonth;i++) {
            if(sheet.getRow(i).getCell(0)!=null){
                Map<String, List<Double>> tillValues = new HashMap<>(); //Key, Quantity+Amount
                tillValues.put("Agency Sales", Arrays.asList(findNum(sheet,i,187),findNum(sheet,i,188)));
                tillValues.put("Agency Sales Commission", Arrays.asList(findNum(sheet,i,190),findNum(sheet,i,191)));
                tillValues.put("Avg. Discounts Per Customer", Arrays.asList(findNum(sheet,i,166),findNum(sheet,i,167)));
                tillValues.put("Avg. OTC Sales Per Customer", Arrays.asList(findNum(sheet,i,157),findNum(sheet,i,158)));
                tillValues.put("Avg. Payment Surcharges Per Customer", Arrays.asList(findNum(sheet,i,178),findNum(sheet,i,179)));
                tillValues.put("Avg. Prescription Sales Per Customer", Arrays.asList(findNum(sheet,i,160),findNum(sheet,i,161)));
                tillValues.put("Avg. Surcharges Per Customer", Arrays.asList(findNum(sheet,i,172),findNum(sheet,i,173)));
                tillValues.put("Equals Total Takings", Arrays.asList(0.00,findNum(sheet,i,154)));
                tillValues.put("Equals Total Till Turnover", Arrays.asList(0.00,findNum(sheet,i,122)));
                tillValues.put("Gross OTC Sales", Arrays.asList(findNum(sheet,i,68),findNum(sheet,i,69)));
                tillValues.put("Gross OTC Sales-Less Discounts", Arrays.asList(findNum(sheet,i,71),findNum(sheet,i,72)));
                tillValues.put("Gross OTC Sales-Plus Payment Surcharges", Arrays.asList(findNum(sheet,i,77),findNum(sheet,i,78)));
                tillValues.put("Gross OTC Sales-Plus Surcharges", Arrays.asList(findNum(sheet,i,74),findNum(sheet,i,75)));
                tillValues.put("Gross Profit ($)", Arrays.asList(0.00,findNum(sheet,i,33)));
                tillValues.put("Gross Profit (%)", Arrays.asList(0.00,findNum(sheet,i,35)));
                tillValues.put("Less Debtor Charges", Arrays.asList(findNum(sheet,i,133),findNum(sheet,i,134)));
                tillValues.put("Less Layby Purchases", Arrays.asList(findNum(sheet,i,136),findNum(sheet,i,137)));
                tillValues.put("Less Petty Cash", Arrays.asList(findNum(sheet,i,145),findNum(sheet,i,146)));
                tillValues.put("Less Redeemed Club Dollars", Arrays.asList(findNum(sheet,i,142),findNum(sheet,i,143)));
                tillValues.put("Less Redeemed Vouchers", Arrays.asList(findNum(sheet,i,139),findNum(sheet,i,140)));
                tillValues.put("Less Total Government Contributions", Arrays.asList(findNum(sheet,i,148),findNum(sheet,i,149)));
                tillValues.put("Open Drawer", Arrays.asList(findNum(sheet,i,196),0.00));
                tillValues.put("Plus Debtor Account Payments", Arrays.asList(findNum(sheet,i,124),findNum(sheet,i,125)));
                tillValues.put("Plus Gross OTC Debtor Charges", Arrays.asList(findNum(sheet,i,80),findNum(sheet,i,81)));
                tillValues.put("Plus Gross OTC Debtor Charges-Less Discounts", Arrays.asList(findNum(sheet,i,83),findNum(sheet,i,84)));
                tillValues.put("Plus Gross OTC Debtor Charges-Plus Payment Surcharges", Arrays.asList(findNum(sheet,i,89),findNum(sheet,i,90)));
                tillValues.put("Plus Gross OTC Debtor Charges-Plus Surcharges", Arrays.asList(findNum(sheet,i,86),findNum(sheet,i,87)));
                tillValues.put("Plus Gross Prescription Debtor Charges", Arrays.asList(findNum(sheet,i,107),findNum(sheet,i,108)));
                tillValues.put("Plus Gross Prescription Debtor Charges-Less Discounts", Arrays.asList(findNum(sheet,i,110),findNum(sheet,i,111)));
                tillValues.put("Plus Gross Prescription Debtor Charges-Plus Government Contribution", Arrays.asList(findNum(sheet,i,119),findNum(sheet,i,120)));
                tillValues.put("Plus Gross Prescription Debtor Charges-Plus Payment Surcharges", Arrays.asList(findNum(sheet,i,116),findNum(sheet,i,117)));
                tillValues.put("Plus Gross Prescription Debtor Charges-Plus Surcharges", Arrays.asList(findNum(sheet,i,113),findNum(sheet,i,114)));
                tillValues.put("Plus Gross Prescription Sales", Arrays.asList(findNum(sheet,i,92),findNum(sheet,i,93)));
                tillValues.put("Plus Gross Prescription Sales-Less Discounts", Arrays.asList(findNum(sheet,i,95),findNum(sheet,i,96)));
                tillValues.put("Plus Gross Prescription Sales-Plus Government Contribution", Arrays.asList(findNum(sheet,i,104),findNum(sheet,i,105)));
                tillValues.put("Plus Gross Prescription Sales-Plus Payment Surcharges", Arrays.asList(findNum(sheet,i,101),findNum(sheet,i,102)));
                tillValues.put("Plus Gross Prescription Sales-Plus Surcharges", Arrays.asList(findNum(sheet,i,98),findNum(sheet,i,99)));
                tillValues.put("Plus Layby Account Payments", Arrays.asList(findNum(sheet,i,127),findNum(sheet,i,128)));
                tillValues.put("Plus Purchased Vouchers", Arrays.asList(findNum(sheet,i,130),findNum(sheet,i,131)));
                tillValues.put("Plus Total Rounding", Arrays.asList(findNum(sheet,i,151),findNum(sheet,i,152)));
                tillValues.put("Refunds", Arrays.asList(findNum(sheet,i,181),findNum(sheet,i,182)));
                tillValues.put("Saved Sales", Arrays.asList(findNum(sheet,i,193),findNum(sheet,i,194)));
                tillValues.put("Total Customers Served", Arrays.asList(findNum(sheet,i,13),0.00));
                tillValues.put("Total Debtor Sales", Arrays.asList(findNum(sheet,i,47),findNum(sheet,i,48)));
                tillValues.put("Total Debtor Sales-GST Free Sales", Arrays.asList(findNum(sheet,i,50),findNum(sheet,i,51)));
                tillValues.put("Total Debtor Sales-GST Sales", Arrays.asList(findNum(sheet,i,53),findNum(sheet,i,54)));
                tillValues.put("Total Discounts", Arrays.asList(findNum(sheet,i,163),findNum(sheet,i,164)));
                tillValues.put("Total Government Contribution", Arrays.asList(findNum(sheet,i,26),findNum(sheet,i,27)));
                tillValues.put("Total GST Collected", Arrays.asList(0.00,findNum(sheet,i,62)));
                tillValues.put("Total GST Free Sales", Arrays.asList(findNum(sheet,i,56),findNum(sheet,i,57)));
                tillValues.put("Total GST Paid in Petty Cash", Arrays.asList(findNum(sheet,i,64),findNum(sheet,i,65)));
                tillValues.put("Total GST Sales", Arrays.asList(findNum(sheet,i,59),findNum(sheet,i,60)));
                tillValues.put("Total Non-Debtor Sales", Arrays.asList(findNum(sheet,i,38),findNum(sheet,i,39)));
                tillValues.put("Total Non-Debtor Sales-GST Free Sales", Arrays.asList(findNum(sheet,i,41),findNum(sheet,i,42)));
                tillValues.put("Total Non-Debtor Sales-GST Sales", Arrays.asList(findNum(sheet,i,44),findNum(sheet,i,45)));
                tillValues.put("Total Payment Surcharges", Arrays.asList(findNum(sheet,i,175),findNum(sheet,i,176)));
                tillValues.put("Total Sales", Arrays.asList(findNum(sheet,i,15),findNum(sheet,i,16)));
                tillValues.put("Total Sales-OTC Sales", Arrays.asList(findNum(sheet,i,18),findNum(sheet,i,19)));
                tillValues.put("Total Sales-Prescription Sales", Arrays.asList(findNum(sheet,i,21),findNum(sheet,i,22)));
                tillValues.put("Total Sales-Prescription Sales (inc Government Contribution)", Arrays.asList(0.00,findNum(sheet,i,24)));
                tillValues.put("Total Surcharges", Arrays.asList(findNum(sheet,i,169),findNum(sheet,i,170)));
                tillValues.put("Total Till Turnover", Arrays.asList(0.00,findNum(sheet,i,29)));
                tillValues.put("Voided Receipts", Arrays.asList(findNum(sheet,i,184),findNum(sheet,i,185)));
                tillValues.put("Total Takings", Arrays.asList(0.00,findNum(sheet,i,31)));
            }
        }

        //pull daily script total values
        sheet = wb.getSheet("Daily script totals");
        for(int i=17;i<daysInMonth*2;i++) {
            if (sheet.getRow(i).getCell(1) != null) {
                LocalDate date = sheet.getRow(i).getCell(1).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                double scriptCount = findNum(sheet,i,5);
                double govtRecovery = findNum(sheet,i,14);
            }
        }

        //pull account payment values
        sheet = wb.getSheet("Account Payments");
        for(int i=0;i<1000;i++){//arbitrarily large row count to hopefully catch all account payments
            String contactName = sheet.getRow(i).getCell(0).getStringCellValue();
            String invoiceNo = sheet.getRow(i).getCell(10).getStringCellValue();
            LocalDate invoiceDate = sheet.getRow(i).getCell(11).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate dueDate = sheet.getRow(i).getCell(12).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String description = sheet.getRow(i).getCell(14).getStringCellValue();
            double quantity = findNum(sheet,i,15);
            double unitAmount = findNum(sheet,i,16);
            double accountCode = findNum(sheet,i,17);
            String gstType = sheet.getRow(i).getCell(18).getStringCellValue();
            boolean accountAdjusted = sheet.getRow(i).getCell(19).getStringCellValue().equals("Y");
        }
    }

    private double findNum(XSSFSheet sheet,int row,int col){
        double num = 0.00;
        try{
            num = sheet.getRow(row).getCell(col).getNumericCellValue();
        }catch(Exception e){
            num = 0.00;
        }
        return num;
    }

}
