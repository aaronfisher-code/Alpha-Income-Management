package utils;

import models.EODDataPoint;
import models.TillReportDataPoint;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/** Writes the EOD page's Xero CSV from the successfully loaded month snapshot. */
public final class XeroEodExporter {
    private XeroEodExporter() {}

    private enum Column {
        CONTACT("*ContactName"), DAY("Day Of Month"), AMOUNT("Amount"), SCRIPTS("No. of scripts"),
        CUSTOMERS("Total customers served"), SALES_COUNT("Total Sales (#)"),
        GOVT_CONTRIBUTION("Total Govt Contribution ($)"), TAKINGS("Total Takings"),
        GP("Gross Profit ($)"), GST_FREE_COUNT("Total GST Free Sales"), INVOICE_NUMBER("*InvoiceNumber"),
        GST_SALES("Total GST Sales"), INVOICE_DATE("*InvoiceDate"), DUE_DATE("*DueDate"),
        GST_COLLECTED("Total GST Collected"), DESCRIPTION("*Description"), QUANTITY("*Quantity"),
        UNIT_AMOUNT("*UnitAmount"), OTC_COUNT("Total OTC Sales (#)"),
        OTC_PER_CUSTOMER("Avg. OTC  Sales Per Customer ($)"), ACCOUNT("*AccountCode"),
        TAX("*TaxType"), RECOVERY("Z Dispense Govt Cont"), STOCK("Stock on hand"),
        SCRIPTS_ON_FILE("Scripts on file count"), SMS("SMS patients"), CLINICAL("Clinical Interventions"),
        MEDSCHECKS("Medschecks"), BALANCE("Till Balance"), RUNNING_BALANCE("Running till Balance"),
        NOTES("Notes");

        final String heading;
        Column(String heading) { this.heading = heading; }
    }

    public static void write(File file, int storeId, YearMonth month,
            List<EODDataPoint> eodData, List<TillReportDataPoint> tillData,
            boolean liveZEnabled) throws IOException {
        write(file, storeId, month, eodData, tillData, liveZEnabled, date -> false);
    }

    public static void write(File file, int storeId, YearMonth month,
            List<EODDataPoint> eodData, List<TillReportDataPoint> tillData,
            boolean liveZEnabled, Predicate<LocalDate> nonWorkingDay) throws IOException {
        Map<LocalDate, EODDataPoint> entries = new HashMap<>();
        for (var entry : eodData) {
            validateDateAndStore(entry.getDate(), entry.getStoreID(), storeId, month);
            if (entries.put(entry.getDate(), entry) != null) {
                throw new IllegalStateException("Duplicate EOD entry for " + entry.getDate());
            }
            for (double amount : new double[] {entry.getCashAmount(), entry.getEftposAmount(),
                    entry.getAmexAmount(), entry.getGoogleSquareAmount(), entry.getChequeAmount(),
                    entry.getStockOnHandAmount()}) {
                requireFinite(amount, "EOD amount for " + entry.getDate());
            }
        }
        Map<LocalDate, List<TillReportDataPoint>> metrics = new HashMap<>();
        for (var point : tillData) {
            validateDateAndStore(point.getAssignedDate(), point.getStoreID(), storeId, month);
            requireFinite(point.getQuantity(), point.getKey() + " quantity for " + point.getAssignedDate());
            requireFinite(point.getAmount(), point.getKey() + " amount for " + point.getAssignedDate());
            metrics.computeIfAbsent(point.getAssignedDate(), date -> new ArrayList<>()).add(point);
        }

        // Build and validate every row before touching the destination.
        List<String[]> rows = new ArrayList<>();
        rows.add(Arrays.stream(Column.values()).map(column -> column.heading).toArray(String[]::new));
        BigDecimal runningBalance = BigDecimal.ZERO;
        boolean runningBalanceKnown = true;
        var currency = NumberFormat.getCurrencyInstance(Locale.US);
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            var eod = entries.getOrDefault(date,
                    new EODDataPoint(false, date, storeId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""));
            var daily = new DailyMetrics(metrics.getOrDefault(date, List.of()));
            if (liveZEnabled && daily.has("Script Count") != daily.has("Govt Recovery")) {
                throw new IllegalStateException("Cannot export incomplete daily script totals for " + date
                        + ". Refresh pharmacy data and try again.");
            }
            boolean balanceApplies = eod.isInDB() && !nonWorkingDay.test(date);
            if (liveZEnabled && balanceApplies && !daily.has("Total Takings")) {
                throw new IllegalStateException("Cannot export EOD data because no completed till report was found for "
                        + date + ". Zero the till for that date.");
            }

            String[] cash = incomeRow("Cash Income", "c", date, month, decimal(eod.getCashAmount()));
            put(cash, Column.SCRIPTS, daily.value("Script Count", true, liveZEnabled));
            put(cash, Column.CUSTOMERS, daily.value("Total Customers Served", true, false));
            put(cash, Column.SALES_COUNT, daily.value("Total Sales", true, false));
            put(cash, Column.GOVT_CONTRIBUTION, daily.value("Total Government Contribution", false, false));
            put(cash, Column.TAKINGS, daily.value("Total Takings", false, false));
            put(cash, Column.GP, daily.value("Gross Profit ($)", false, false));
            // This legacy column contains the sales COUNT, despite its ambiguous heading.
            put(cash, Column.GST_FREE_COUNT, daily.value("Total GST Free Sales", true, false));
            put(cash, Column.GST_SALES, daily.value("Total GST Sales", false, false));
            put(cash, Column.GST_COLLECTED, daily.value("Total GST Collected", false, false));
            put(cash, Column.OTC_COUNT, daily.value("Total Sales-OTC Sales", true, false));
            put(cash, Column.OTC_PER_CUSTOMER, daily.otcAverage(date));
            put(cash, Column.RECOVERY, daily.value("Govt Recovery", false, liveZEnabled));
            put(cash, Column.STOCK, Double.toString(eod.getStockOnHandAmount()));
            put(cash, Column.SCRIPTS_ON_FILE, Integer.toString(eod.getScriptsOnFile()));
            put(cash, Column.SMS, Integer.toString(eod.getSmsPatients()));
            put(cash, Column.MEDSCHECKS, Integer.toString(eod.getMedschecks()));
            put(cash, Column.NOTES, eod.getNotes());
            if (balanceApplies) {
                if (daily.has("Total Takings")) {
                    BigDecimal balance = decimal(eod.getCashAmount()).add(decimal(eod.getEftposAmount()))
                            .add(decimal(eod.getAmexAmount())).add(decimal(eod.getGoogleSquareAmount()))
                            .add(decimal(eod.getChequeAmount())).subtract(daily.sum("Total Takings", false));
                    put(cash, Column.BALANCE, currency.format(balance));
                    runningBalance = runningBalance.add(balance);
                } else {
                    // A missing manual till report is unknown, rather than zero.
                    runningBalanceKnown = false;
                }
            }
            if (runningBalanceKnown) put(cash, Column.RUNNING_BALANCE, currency.format(runningBalance));
            rows.add(cash);
            rows.add(incomeRow("Eftpos Income", "e", date, month, decimal(eod.getEftposAmount())));
            rows.add(incomeRow("Amex Income", "a", date, month, decimal(eod.getAmexAmount())));
            rows.add(incomeRow("Google Square Income", "gs", date, month, decimal(eod.getGoogleSquareAmount())));
            rows.add(incomeRow("Cheques Income", "ch", date, month, decimal(eod.getChequeAmount())));
            rows.add(incomeRow("Medicare PBS (Ex GST)", "me", date, month, daily.sum("Govt Recovery", false)));
        }
        writeAtomically(file.toPath(), rows);
    }

    private static String[] incomeRow(String contact, String suffix, LocalDate date,
            YearMonth month, BigDecimal amount) {
        String[] row = new String[Column.values().length];
        Arrays.fill(row, "");
        put(row, Column.CONTACT, contact);
        put(row, Column.DAY, Integer.toString(date.getDayOfMonth()));
        put(row, Column.AMOUNT, number(amount));
        // A negative amount also needs the reference and dates for a credit note.
        if (amount.signum() != 0) {
            put(row, Column.INVOICE_NUMBER, date.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + suffix);
            put(row, Column.INVOICE_DATE, date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            put(row, Column.DUE_DATE, month.atEndOfMonth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        put(row, Column.DESCRIPTION, contact);
        put(row, Column.QUANTITY, "1");
        put(row, Column.UNIT_AMOUNT, number(amount));
        put(row, Column.ACCOUNT, "200");
        put(row, Column.TAX, "GST Free Income");
        return row;
    }

    private static void writeAtomically(Path destination, List<String[]> rows) throws IOException {
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".alpha-xero-", ".csv");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (var row : rows) {
                    for (int column = 0; column < row.length; column++) {
                        if (column > 0) writer.write(',');
                        writer.write(escape(row[column]));
                    }
                    writer.write("\r\n");
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String escape(String value) {
        return value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0 ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private static void put(String[] row, Column column, String value) {
        row[column.ordinal()] = value == null ? "" : value;
    }

    private static BigDecimal decimal(double value) { return BigDecimal.valueOf(value); }
    private static String number(BigDecimal value) { return Double.toString(value.doubleValue()); }

    private static void requireFinite(double value, String description) {
        if (!Double.isFinite(value)) throw new IllegalStateException("Invalid " + description);
    }

    private static void validateDateAndStore(LocalDate date, int actualStoreId, int storeId, YearMonth month) {
        if (date == null || !YearMonth.from(date).equals(month) || actualStoreId != storeId) {
            throw new IllegalStateException("Export data does not match store " + storeId + " and month " + month);
        }
    }

    private record DailyMetrics(List<TillReportDataPoint> points) {
        boolean has(String key) { return points.stream().anyMatch(point -> key.equals(point.getKey())); }

        BigDecimal sum(String key, boolean quantity) {
            return points.stream().filter(point -> key.equals(point.getKey()))
                    .map(point -> decimal(quantity ? point.getQuantity() : point.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        String value(String key, boolean quantity, boolean zeroWhenAbsent) {
            return has(key) || zeroWhenAbsent ? number(sum(key, quantity)) : "";
        }

        String otcAverage(LocalDate date) {
            long averages = points.stream().filter(point -> "Avg. OTC Sales Per Customer".equals(point.getKey())).count();
            if (averages == 0) return "";
            if (averages == 1) return number(sum("Avg. OTC Sales Per Customer", false));
            if (!has("Total Customers Served") || !has("Total Sales-OTC Sales")) {
                throw new IllegalStateException("Cannot calculate daily OTC average for " + date
                        + ": sales or customer totals are missing.");
            }
            BigDecimal customers = sum("Total Customers Served", true);
            return customers.signum() == 0 ? "0.0"
                    : number(sum("Total Sales-OTC Sales", false).divide(customers, 2, RoundingMode.HALF_UP));
        }
    }
}
