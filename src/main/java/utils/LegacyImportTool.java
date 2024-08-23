package utils;

import application.Main;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.sql.Date;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

public class LegacyImportTool {

    private Connection con = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private Main main;

    private String sql = null;


    public LegacyImportTool(Main main){
        this.main = main;
    }

    public void ImportStaffCopy(XSSFWorkbook wb) {

        //creating a Sheet object to retrieve the object
        XSSFSheet sheet = wb.getSheet("EOD");
        int month = (int) sheet.getRow(0).getCell(0).getNumericCellValue();
        int year = (int) sheet.getRow(1).getCell(0).getNumericCellValue();
        System.out.println("Currently importing: " + Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
        YearMonth yearMonthObject = YearMonth.of(year, month);
        int daysInMonth = yearMonthObject.lengthOfMonth();

        try{
            sql = "INSERT INTO eodDataPoints (cash, eftpos, amex, googleSquare, cheque, medschecks, scriptsOnFile,stockOnHand, smsPatients, notes, date,storeID) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE cash=?, eftpos=?, amex=?, googleSquare=?, cheque=?, medschecks=?, scriptsOnFile=?,stockOnHand=?, smsPatients=?, notes=?";
            preparedStatement = con.prepareStatement(sql);
            con.setAutoCommit(false);
            //Pull EOD values
            for(int i=0;i<daysInMonth;i++){
                double cashAmount = findNum(sheet,i+1,2);
                double eftposAmount = findNum(sheet,i+1,3);
                double amexAmount = findNum(sheet,i+1,4);
                double googleSquareAmount = findNum(sheet,i+1,5);
                double chequeAmount =findNum(sheet,i+1,6);
//                double clinicalInterventions = sheet.getRow(i+1).getCell(7).getNumericCellValue();
                int medschecks = (int) findNum(sheet,i+1,8);
                double stockOnHand = findNum(sheet,i+1,9);
                int scriptsOnFile = (int) findNum(sheet,i+1,10);
                int smsPatients = (int) findNum(sheet,i+1,11);
                String notes = sheet.getRow(i+1).getCell(14).getStringCellValue();
                preparedStatement.setDouble(1, cashAmount);
                preparedStatement.setDouble(2, eftposAmount);
                preparedStatement.setDouble(3, amexAmount);
                preparedStatement.setDouble(4, googleSquareAmount);
                preparedStatement.setDouble(5, chequeAmount);
                preparedStatement.setInt(6, medschecks);
                preparedStatement.setInt(7, scriptsOnFile);
                preparedStatement.setDouble(8, stockOnHand);
                preparedStatement.setInt(9, smsPatients);
                preparedStatement.setString(10, notes);
                preparedStatement.setDate(11, Date.valueOf(LocalDate.of(year, month, i+1)));
                preparedStatement.setInt(12, main.getCurrentStore().getStoreID());
                preparedStatement.setDouble(13, cashAmount);
                preparedStatement.setDouble(14, eftposAmount);
                preparedStatement.setDouble(15, amexAmount);
                preparedStatement.setDouble(16, googleSquareAmount);
                preparedStatement.setDouble(17, chequeAmount);
                preparedStatement.setInt(18, medschecks);
                preparedStatement.setInt(19, scriptsOnFile);
                preparedStatement.setDouble(20, stockOnHand);
                preparedStatement.setInt(21, smsPatients);
                preparedStatement.setString(22, notes);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        }catch(Exception e){
            e.printStackTrace();
        }

        sheet = wb.getSheet("Till Reports");
        try{
            sql = "INSERT INTO tillreportdatapoints (storeID, assignedDate, periodStartDate, periodEndDate, `key`, quantity, amount) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE periodStartDate=?,periodEndDate=?,quantity=?, amount=?";
            preparedStatement = con.prepareStatement(sql);
            con.setAutoCommit(false);
            //Pull Tillreport values
            for(int i=0;i<daysInMonth;i++) {
                if(sheet.getRow(i)!=null && sheet.getRow(i).getCell(0)!=null && sheet.getRow(i).getCell(0).getCellType()!= CellType.BLANK){
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

                    String period = sheet.getRow(i).getCell(8).getStringCellValue();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    // Check if the string contains parentheses
                    if (period.contains("(") && period.contains(")")) {
                        period = period.substring(period.indexOf('(') + 1, period.indexOf(')'));
                    }
                    String[] parts = period.split(" to ");
                    LocalDateTime startDate = LocalDateTime.parse(parts[0], formatter);
                    LocalDateTime endDate = LocalDateTime.parse(parts[1], formatter);

                    for (Map.Entry<String, List<Double>> entry : tillValues.entrySet()) {
                        preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
                        preparedStatement.setDate(2, Date.valueOf(LocalDate.of(year,month,i+1)));
                        preparedStatement.setTimestamp(3, Timestamp.valueOf(startDate));
                        preparedStatement.setTimestamp(4, Timestamp.valueOf(endDate));
                        preparedStatement.setString(5, entry.getKey());
                        preparedStatement.setDouble(6, entry.getValue().get(0));
                        preparedStatement.setDouble(7, entry.getValue().get(1));
                        preparedStatement.setTimestamp(8, Timestamp.valueOf(startDate));
                        preparedStatement.setTimestamp(9, Timestamp.valueOf(endDate));
                        preparedStatement.setDouble(10, entry.getValue().get(0));
                        preparedStatement.setDouble(11, entry.getValue().get(1));
                        preparedStatement.addBatch();
                    }
                }
            }
            preparedStatement.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        }catch(Exception e){
            e.printStackTrace();
        }


        //pull daily script total values
        sheet = wb.getSheet("Daily script totals");
        try{
            sql = "INSERT INTO tillreportdatapoints (storeID, assignedDate, `key`, quantity, amount) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE quantity=?, amount=?";
            preparedStatement = con.prepareStatement(sql);
            con.setAutoCommit(false);
            //Pull Daily script values
            for(int i=17;i<71;i++) {
                if (sheet.getRow(i).getCell(1) != null && sheet.getRow(i).getCell(1).getCellType()!= CellType.BLANK) {
                    LocalDate date;
                    if(sheet.getRow(i).getCell(1).getCellType()==CellType.STRING&&sheet.getRow(i).getCell(1).getStringCellValue().equals("TOTAL")) {
                        break;
                    }
                    if(sheet.getRow(i).getCell(1).getCellType()==CellType.STRING){
                        date = LocalDate.parse(sheet.getRow(i).getCell(1).getStringCellValue(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    }else{
                        date = sheet.getRow(i).getCell(1).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                    double scriptCount = findNum(sheet,i,5);
                    double govtRecovery = findNum(sheet,i,14);
                    preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
                    preparedStatement.setDate(2, Date.valueOf(date));
                    preparedStatement.setString(3, "Script Count");
                    preparedStatement.setDouble(4, scriptCount);
                    preparedStatement.setDouble(5, 0.00);
                    preparedStatement.setDouble(6, scriptCount);
                    preparedStatement.setDouble(7, 0.00);
                    preparedStatement.addBatch();

                    preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
                    preparedStatement.setDate(2, Date.valueOf(date));
                    preparedStatement.setString(3, "Govt Recovery");
                    preparedStatement.setDouble(4, 0.00);
                    preparedStatement.setDouble(5, govtRecovery);
                    preparedStatement.setDouble(6, 0.00);
                    preparedStatement.setDouble(7, govtRecovery);
                    preparedStatement.addBatch();
                }
            }
            preparedStatement.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        }catch(Exception e){
            e.printStackTrace();
        }

        //pull account payment values
        sheet = wb.getSheet("Account Payments");
        for(int i=1;i<1000;i++) {//arbitrarily large row count to hopefully catch all account payments
            if (sheet.getRow(i).getCell(0) != null && sheet.getRow(i).getCell(0).getCellType() != CellType.BLANK){
                String contactName = sheet.getRow(i).getCell(0).getStringCellValue();

                try {
                    //Check if contact name exists in db
                    sql = "SELECT * FROM accountpaymentcontacts WHERE contactName=? AND storeID=?";
                    preparedStatement = con.prepareStatement(sql);
                    preparedStatement.setString(1, contactName);
                    preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                    resultSet = preparedStatement.executeQuery();
                    int contactID;
                    if (!resultSet.next()) {
                        //insert contact name, store id, account code into db
                        sql = "INSERT INTO accountpaymentcontacts (contactName, storeID, accountCode) VALUES (?,?,?)";
                        preparedStatement = con.prepareStatement(sql);
                        preparedStatement.setString(1, contactName);
                        preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                        preparedStatement.setString(3, "200");
                        preparedStatement.executeUpdate();

                        //get contact id
                        sql = "SELECT * FROM accountpaymentcontacts WHERE contactName=? AND storeID=?";
                        preparedStatement = con.prepareStatement(sql);
                        preparedStatement.setString(1, contactName);
                        preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                        resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        contactID = resultSet.getInt("idaccountPaymentContacts");
                    } else {
                        //get contact ID
                        contactID = resultSet.getInt("idaccountPaymentContacts");
                    }
                    sql = "INSERT INTO accountpayments (contactID, storeID, invoiceNo, invoiceDate, dueDate, description, unitAmount, accountAdjusted, taxRate) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE contactID=?, storeID=?, invoiceNo=?, invoiceDate=?, dueDate=?, description=?, unitAmount=?, accountAdjusted=?, taxRate=?";
                    preparedStatement = con.prepareStatement(sql);
                    preparedStatement.setInt(1, contactID);
                    preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                    preparedStatement.setString(3, getCellAsString(sheet,i,10));
                    preparedStatement.setDate(4, Date.valueOf(findDate(sheet, i, 11)));
                    preparedStatement.setDate(5, Date.valueOf(findDate(sheet, i, 12)));
                    preparedStatement.setString(6, getCellAsString(sheet,i,14));
                    preparedStatement.setDouble(7, findNum(sheet, i, 16));
                    preparedStatement.setBoolean(8, sheet.getRow(i).getCell(19).getStringCellValue().equals("Y"));
                    preparedStatement.setString(9, sheet.getRow(i).getCell(18).getStringCellValue());
                    preparedStatement.setInt(10, contactID);
                    preparedStatement.setInt(11, main.getCurrentStore().getStoreID());
                    preparedStatement.setString(12, getCellAsString(sheet,i,10));
                    preparedStatement.setDate(13, Date.valueOf(findDate(sheet, i, 11)));
                    preparedStatement.setDate(14, Date.valueOf(findDate(sheet, i, 12)));
                    preparedStatement.setString(15, getCellAsString(sheet,i,14));
                    preparedStatement.setDouble(16, findNum(sheet, i, 16));
                    preparedStatement.setBoolean(17, sheet.getRow(i).getCell(19).getStringCellValue().equals("Y"));
                    preparedStatement.setString(18, sheet.getRow(i).getCell(18).getStringCellValue());
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void importOwnersCopy(XSSFWorkbook wb){
        //creating a Sheet object to retrieve the invoices
        XSSFSheet sheet = wb.getSheet("COGS");
        for(int i=1;i<999;i++) {//arbitrarily large row count to hopefully catch all invoices
            if (sheet.getRow(i).getCell(16) != null && sheet.getRow(i).getCell(16).getCellType() != CellType.BLANK){
                String contactName = sheet.getRow(i).getCell(0).getStringCellValue();

                try {
                    //Check if contact name exists in db
                    sql = "SELECT * FROM invoicesuppliers WHERE supplierName=? AND storeID=?";
                    preparedStatement = con.prepareStatement(sql);
                    preparedStatement.setString(1, contactName);
                    preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                    resultSet = preparedStatement.executeQuery();
                    int contactID;
                    if (!resultSet.next()) {
                        //insert contact name, store id, account code into db
                        sql = "INSERT INTO invoicesuppliers (supplierName, storeID) VALUES (?,?)";
                        preparedStatement = con.prepareStatement(sql);
                        preparedStatement.setString(1, contactName);
                        preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                        preparedStatement.executeUpdate();

                        //get contact id
                        sql = "SELECT * FROM invoicesuppliers WHERE supplierName=? AND storeID=?";
                        preparedStatement = con.prepareStatement(sql);
                        preparedStatement.setString(1, contactName);
                        preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
                        resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        contactID = resultSet.getInt("idinvoiceSuppliers");
                    } else {
                        //get contact ID
                        contactID = resultSet.getInt("idinvoiceSuppliers");
                    }
                    sql = "INSERT INTO invoices (supplierID, invoiceNo, invoiceDate, dueDate, description, unitAmount, storeID, notes) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE supplierID=?, invoiceNo=?, invoiceDate=?, dueDate=?, description=?, unitAmount=?, storeID=?, notes=?";
                    preparedStatement = con.prepareStatement(sql);
                    preparedStatement.setInt(1, contactID);
                    preparedStatement.setString(2, getCellAsString(sheet,i,10));
                    preparedStatement.setDate(3, Date.valueOf(findDate(sheet,i,11)));
                    preparedStatement.setDate(4, Date.valueOf(findDate(sheet,i,12)));
                    preparedStatement.setString(5, getCellAsString(sheet,i,14));
                    preparedStatement.setDouble(6, findNum(sheet,i,16));
                    preparedStatement.setInt(7, main.getCurrentStore().getStoreID());
                    preparedStatement.setString(8, "");
                    preparedStatement.setInt(9, contactID);
                    preparedStatement.setString(10, getCellAsString(sheet,i,10));
                    preparedStatement.setDate(11, Date.valueOf(findDate(sheet,i,11)));
                    preparedStatement.setDate(12, Date.valueOf(findDate(sheet,i,12)));
                    preparedStatement.setString(13, getCellAsString(sheet,i,14));
                    preparedStatement.setDouble(14, findNum(sheet,i,16));
                    preparedStatement.setInt(15, main.getCurrentStore().getStoreID());
                    preparedStatement.setString(16, "");
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //Import BAS Checker values
        LocalDate date = LocalDate.of((int) findNum(wb.getSheet("EOD"),1,0), (int) findNum(wb.getSheet("EOD"),0,0), 1);
        sheet = wb.getSheet("BAS Checker");
        try{
            sql = "INSERT INTO baschecker (date, storeID, cashAdjustment, eftposAdjustment, amexAdjustment, googleSquareAdjustment, chequesAdjustment, medicareAdjustment, totalIncomeAdjustment, cashCorrect, eftposCorrect, amexCorrect, googleSquareCorrect, chequesCorrect, medicareCorrect, totalIncomeCorrect, gstCorrect, basDailyScript) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE cashAdjustment=?, eftposAdjustment=?, amexAdjustment=?, googleSquareAdjustment=?, chequesAdjustment=?, medicareAdjustment=?, totalIncomeAdjustment=?, cashCorrect=?, eftposCorrect=?, amexCorrect=?, googleSquareCorrect=?, chequesCorrect=?, medicareCorrect=?, totalIncomeCorrect=?, gstCorrect=?, basDailyScript=?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, Date.valueOf(date));
            preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
            preparedStatement.setDouble(3, findNum(sheet, 2, 2));
            preparedStatement.setDouble(4, findNum(sheet, 3, 2));
            preparedStatement.setDouble(5, findNum(sheet, 4, 2));
            preparedStatement.setDouble(6, findNum(sheet, 5, 2));
            preparedStatement.setDouble(7, findNum(sheet, 6, 2));
            preparedStatement.setDouble(8, findNum(sheet, 7, 2));
            preparedStatement.setDouble(9, findNum(sheet, 8, 2));
            preparedStatement.setBoolean(10, sheet.getRow(2).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(11, sheet.getRow(3).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(12, sheet.getRow(4).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(13, sheet.getRow(5).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(14, sheet.getRow(6).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(15, sheet.getRow(7).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(16, sheet.getRow(8).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(17, sheet.getRow(9).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setDouble(18, findNum(sheet, 14, 2));
            preparedStatement.setDouble(19, findNum(sheet, 2, 2));
            preparedStatement.setDouble(20, findNum(sheet, 3, 2));
            preparedStatement.setDouble(21, findNum(sheet, 4, 2));
            preparedStatement.setDouble(22, findNum(sheet, 5, 2));
            preparedStatement.setDouble(23, findNum(sheet, 6, 2));
            preparedStatement.setDouble(24, findNum(sheet, 7, 2));
            preparedStatement.setDouble(25, findNum(sheet, 8, 2));
            preparedStatement.setBoolean(26, sheet.getRow(2).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(27, sheet.getRow(3).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(28, sheet.getRow(4).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(29, sheet.getRow(5).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(30, sheet.getRow(6).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(31, sheet.getRow(7).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(32, sheet.getRow(8).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setBoolean(33, sheet.getRow(9).getCell(4).getStringCellValue().toLowerCase().equals("y"));
            preparedStatement.setDouble(34, findNum(sheet, 14, 2));
            preparedStatement.executeUpdate();
        }catch(SQLException e){
            throw new RuntimeException(e);
        }

        //Import budget and expenses values
        sheet = wb.getSheet("Budget and Expenses");
        try{
            sql = "INSERT INTO budgetandexpenses (date, storeID, monthlyRent, dailyOutgoings, monthlyLoan, `6CPAIncome`, LanternPayIncome, OtherIncome, ATO_GST_BAS_refund) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE monthlyRent=?, dailyOutgoings=?, monthlyLoan=?, `6CPAIncome`=?, LanternPayIncome=?, OtherIncome=?, ATO_GST_BAS_refund=?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, Date.valueOf(date));
            preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
            preparedStatement.setDouble(3, findNum(sheet, 4, 2));
            preparedStatement.setDouble(4, findNum(sheet, 9, 2));
            preparedStatement.setDouble(5, findNum(sheet, 15, 2));
            preparedStatement.setDouble(6, findNum(sheet, 18, 1));
            preparedStatement.setDouble(7, findNum(sheet, 19, 1));
            preparedStatement.setDouble(8, findNum(sheet, 20, 1));
            preparedStatement.setDouble(9, findNum(sheet, 21, 1));
            preparedStatement.setDouble(10, findNum(sheet, 4, 2));
            preparedStatement.setDouble(11, findNum(sheet, 9, 2));
            preparedStatement.setDouble(12, findNum(sheet, 15, 2));
            preparedStatement.setDouble(13, findNum(sheet, 18, 1));
            preparedStatement.setDouble(14, findNum(sheet, 19, 1));
            preparedStatement.setDouble(15, findNum(sheet, 20, 1));
            preparedStatement.setDouble(16, findNum(sheet, 21, 1));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellAsString(XSSFSheet sheet, int row, int col) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(sheet.getRow(row).getCell(col));
    }

    public double findNum(XSSFSheet sheet,int row,int col) {
        double value = 0.0;
        Cell cell = sheet.getRow(row).getCell(col);
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    try {
                        value = Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        System.out.println("Could not parse the string into a double at " + row + ", " + col);
                        e.printStackTrace();
                    }
                    break;

                case NUMERIC:
                    value = cell.getNumericCellValue();
                    break;

                case FORMULA: //If the cell contains formula, evaluate it
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        value = evaluator.evaluate(cell).getNumberValue();
                    } catch (Exception e) {
                        System.out.println("Could not evaluate the formula");
                        e.printStackTrace();
                    }
                    break;

                case BLANK:
                    value = 0.0;
                    break;

                default:
                    System.out.println("The cell contains an unsupported type");
            }
        }
        return value;
    }

//    private double findNum(XSSFSheet sheet,int row,int col){
//        double num = 0.00;
//        try{
//            num = sheet.getRow(row).getCell(col).getNumericCellValue();
//        }catch(Exception e){
//            num = 0.00;
//        }
//        return num;
//    }

    private LocalDate findDate(XSSFSheet sheet, int row, int col) {
        LocalDate date = null;
        try {
            Cell cell = sheet.getRow(row).getCell(col);
            if (cell.getCellType() == CellType.STRING) {
                date = parseDate(cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.NUMERIC) {
                java.util.Date dateCellValue = cell.getDateCellValue();
                date = dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(date==null){
            System.err.println("Error parsing date at row "+row+" column "+col);
        }
        return date;
    }

    private LocalDate parseDate(String dateString) {
        List<String> formatStrings = Arrays.asList("dd/MM/yyyy", "d/M/yy", "d/M/yyyy","d/MMyyyy");
        for (String formatString : formatStrings) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(formatString);
                return LocalDate.parse(dateString, dtf);
            } catch (DateTimeParseException e) {
                //ignore exception, try next format
            }
        }
        System.out.println("Unable to parse date: " + dateString);
        return null; // Return null if none of the formats work
    }

}
