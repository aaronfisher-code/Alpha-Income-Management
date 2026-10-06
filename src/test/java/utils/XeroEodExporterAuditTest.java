package utils;

import models.EODDataPoint;
import models.TillReportDataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class XeroEodExporterAuditTest {
    @TempDir Path directory;
    private static final YearMonth MONTH = YearMonth.of(2026, 9);

    @Test
    void allFieldsMatchOriginalExporterExceptDocumentedMissingEntryBalanceCorrection() throws Exception {
        var entries = new ArrayList<EODDataPoint>();
        var metrics = new ArrayList<TillReportDataPoint>();
        String[] keys = {"Script Count", "Total Customers Served", "Total Sales", "Total Government Contribution",
                "Total Takings", "Gross Profit ($)", "Total GST Free Sales", "Total GST Sales",
                "Total GST Collected", "Total Sales-OTC Sales", "Avg. OTC Sales Per Customer", "Govt Recovery"};
        double[] quantities = {10, 11, 12, 13, 0, 0, 14, 15, 0, 16, 0, 0};
        double[] amounts = {0, 0, 120.25, 30.5, 21, 40.75, 50.5, 60.25, 7.25, 80.5, 8.5, 42.75};
        for (int day : new int[] {1, 16, 30}) {
            var date = MONTH.atDay(day);
            entries.add(new EODDataPoint(true, date, 1, 2, 3, 4, 5, 6, 7, 123.45, 8, 9, 0, 0, "Legacy note"));
            for (int key = 0; key < keys.length; key++) {
                metrics.add(metric(date, keys[key], quantities[key], amounts[key]));
            }
        }
        // Unsorted service responses must still produce ascending calendar days.
        Collections.reverse(entries);
        Collections.reverse(metrics);
        var actual = export(MONTH, entries, metrics, false);
        List<List<String>> legacy;
        try (var resource = getClass().getResourceAsStream("/exports/legacy-xero-september-2026.csv")) {
            assertNotNull(resource);
            legacy = parse(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(181, actual.size());
        assertEquals(legacy.size(), actual.size());
        assertEquals(legacy.getFirst(), actual.getFirst(), "All 31 headings and their order");
        for (int index = 1; index < actual.size(); index++) {
            var expected = new ArrayList<>(legacy.get(index));
            while (expected.size() < 31) expected.add(""); // Legacy non-cash rows stopped at column 22.
            int day = Integer.parseInt(expected.get(1));
            if ("Cash Income".equals(expected.getFirst()) && !Set.of(1, 16, 30).contains(day)) {
                assertEquals("$0.00", expected.get(28));
                expected.set(28, ""); // No entered EOD => no daily variance.
            }
            assertEquals(expected, actual.get(index), "All columns at row " + index);
        }
        BigDecimal invoiceTotal = actual.stream().skip(1).map(row -> new BigDecimal(row.get(17)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, invoiceTotal.compareTo(new BigDecimal("188.25")),
                "3 * (2 + 3 + 4 + 5 + 6 + 42.75), with Medicare included once in Xero unit amounts");
    }

    @Test
    void csvEscapesLargeBalancesAndMultilineQuotedUnicodeNotes() throws Exception {
        var date = MONTH.atDay(16);
        var entry = entry(date, 1234.56);
        entry.setNotes("Paid, \"confirmed\"\nCafé — staff");
        var rows = export(MONTH, List.of(entry), List.of(metric(date, "Total Takings", 0, 0)), false);
        assertTrue(rows.stream().allMatch(row -> row.size() == 31));
        var cash = row(rows, "Cash Income", 16);
        assertEquals("$1,234.56", cash.get(28));
        assertEquals("$1,234.56", cash.get(29));
        assertEquals(entry.getNotes(), cash.get(30));
        assertEquals("200", cash.get(20));
        assertEquals("GST Free Income", cash.get(21));
    }

    @Test
    void multiplePeriodsAreSummedAndOtcAverageIsWeightedByCustomers() throws Exception {
        var date = MONTH.atDay(16);
        var data = List.of(
                metric(date, "Total Takings", 0, 100), metric(date, "Total Takings", 0, 200),
                metric(date, "Script Count", 3, 0), metric(date, "Script Count", 4, 0),
                metric(date, "Govt Recovery", 0, 10.25), metric(date, "Govt Recovery", 0, 20.50),
                metric(date, "Total Customers Served", 10, 0), metric(date, "Total Customers Served", 30, 0),
                metric(date, "Total Sales", 12, 120), metric(date, "Total Sales", 35, 240),
                metric(date, "Total GST Free Sales", 9, 70), metric(date, "Total GST Free Sales", 12, 80),
                metric(date, "Total Sales-OTC Sales", 5, 20), metric(date, "Total Sales-OTC Sales", 9, 180),
                metric(date, "Avg. OTC Sales Per Customer", 0, 2),
                metric(date, "Avg. OTC Sales Per Customer", 0, 6));
        var rows = export(MONTH, List.of(entry(date, 310)), data, true);
        var cash = row(rows, "Cash Income", 16);
        assertEquals("7.0", cash.get(3));
        assertEquals("40.0", cash.get(4));
        assertEquals("47.0", cash.get(5));
        assertEquals("300.0", cash.get(7));
        assertEquals("21.0", cash.get(9), "Legacy GST-free field remains a count");
        assertEquals("14.0", cash.get(18));
        assertEquals("5.0", cash.get(19), "(20 + 180) / (10 + 30), rather than adding averages");
        assertEquals("30.75", cash.get(22));
        assertEquals("$10.00", cash.get(28));
        assertEquals("$10.00", cash.get(29));
        assertEquals("30.75", row(rows, "Medicare PBS (Ex GST)", 16).get(17));
    }

    @Test
    void absentEodAndClosedRosterDaysDoNotContaminateRunningBalance() throws Exception {
        var file = directory.resolve("closed.csv");
        var entries = List.of(entry(MONTH.atDay(1), 100), entry(MONTH.atDay(2), 0),
                entry(MONTH.atDay(4), 100));
        var data = List.of(metric(MONTH.atDay(1), "Total Takings", 0, 99),
                metric(MONTH.atDay(2), "Total Takings", 0, 500),
                metric(MONTH.atDay(3), "Total Takings", 0, 900),
                metric(MONTH.atDay(4), "Total Takings", 0, 98));
        XeroEodExporter.write(file.toFile(), 1, MONTH, entries, data, true, date -> date.getDayOfMonth() == 2);
        var rows = parse(Files.readString(file));
        for (int day : new int[] {2, 3}) {
            assertEquals("", row(rows, "Cash Income", day).get(28));
            assertEquals("$1.00", row(rows, "Cash Income", day).get(29));
        }
        assertEquals("$2.00", row(rows, "Cash Income", 4).get(28));
        assertEquals("$3.00", row(rows, "Cash Income", 4).get(29));
    }

    @Test
    void incompleteManualMonthLeavesUnknownBalancesBlank() throws Exception {
        var rows = export(MONTH, List.of(entry(MONTH.atDay(1), 100), entry(MONTH.atDay(2), 100)),
                List.of(metric(MONTH.atDay(2), "Total Takings", 0, 99)), false);
        assertEquals("", row(rows, "Cash Income", 1).get(28));
        assertEquals("", row(rows, "Cash Income", 1).get(29));
        assertEquals("$1.00", row(rows, "Cash Income", 2).get(28));
        assertEquals("", row(rows, "Cash Income", 2).get(29));
        assertEquals("", row(rows, "Cash Income", 30).get(29));
    }

    @Test
    void decimalTenderArithmeticDoesNotCreateNegativeZeroVariance() throws Exception {
        var entry = entry(MONTH.atDay(1), 0.1);
        entry.setEftposAmount(0.2);
        var rows = export(MONTH, List.of(entry),
                List.of(metric(MONTH.atDay(1), "Total Takings", 0, 0.3)), true);

        assertEquals("$0.00", row(rows, "Cash Income", 1).get(28));
        assertEquals("$0.00", row(rows, "Cash Income", 30).get(29));
    }

    @ParameterizedTest
    @CsvSource({"2026,2,28", "2028,2,29", "2026,12,31"})
    void everyNonzeroIncomeIncludingCreditsHasCorrectReferenceAndMonthEndDates(int year, int month, int lastDay)
            throws Exception {
        var selected = YearMonth.of(year, month);
        var date = selected.atEndOfMonth();
        var entry = new EODDataPoint(true, date, 1, -1, -2, -3, -4, -5, 0, 0, 0, 0, 0, 0, "");
        var rows = export(selected, List.of(entry), List.of(metric(date, "Total Takings", 0, -15),
                metric(date, "Script Count", 1, 0), metric(date, "Govt Recovery", 0, -6)), true);
        String[] contacts = {"Cash Income", "Eftpos Income", "Amex Income", "Google Square Income",
                "Cheques Income", "Medicare PBS (Ex GST)"};
        String[] suffixes = {"c", "e", "a", "gs", "ch", "me"};
        assertEquals(1 + lastDay * 6, rows.size());
        for (int i = 0; i < contacts.length; i++) {
            var income = row(rows, contacts[i], lastDay);
            assertEquals(lastDay + String.format("%02d", month) + year + suffixes[i], income.get(10));
            String formattedDate = String.format("%02d/%02d/%04d", lastDay, month, year);
            assertEquals(formattedDate, income.get(12));
            assertEquals(formattedDate, income.get(13));
            assertEquals(Double.toString(-i - 1.0), income.get(17));
            assertEquals(income.get(2), income.get(17));
            assertEquals("1", income.get(16));
            assertEquals("200", income.get(20));
            assertEquals("GST Free Income", income.get(21));
        }
    }

    @Test
    void invalidSnapshotsNeverOverwriteExistingCsv() throws Exception {
        var file = directory.resolve("existing.csv");
        Files.writeString(file, "previous export");
        var date = MONTH.atDay(16);
        var wrongStore = metric(date, "Total Takings", 0, 100);
        wrongStore.setStoreID(2);
        for (var metrics : List.of(List.of(wrongStore),
                List.of(metric(date.plusMonths(1), "Total Takings", 0, 100)),
                List.of(metric(date, "Total Takings", 0, Double.NaN)))) {
            assertThrows(IllegalStateException.class,
                    () -> XeroEodExporter.write(file.toFile(), 1, MONTH, List.of(), metrics, false));
            assertEquals("previous export", Files.readString(file));
        }
        assertThrows(IllegalStateException.class, () -> XeroEodExporter.write(file.toFile(), 1, MONTH,
                List.of(entry(date, 10), entry(date, 20)), List.of(), false));
        assertThrows(IllegalStateException.class, () -> XeroEodExporter.write(file.toFile(), 1, MONTH,
                List.of(entry(date, 10)), List.of(), true));
        var invalidTender = entry(date, Double.POSITIVE_INFINITY);
        assertThrows(IllegalStateException.class, () -> XeroEodExporter.write(file.toFile(), 1, MONTH,
                List.of(invalidTender), List.of(), false));
        assertEquals("previous export", Files.readString(file));
    }

    @Test
    void writeFailureIsReportedAndTemporaryCsvIsRemoved() throws Exception {
        var destinationDirectory = directory.resolve("not-a-file.csv");
        Files.createDirectory(destinationDirectory);
        Files.writeString(destinationDirectory.resolve("keep.txt"), "keep");
        assertThrows(java.io.IOException.class, () -> XeroEodExporter.write(destinationDirectory.toFile(),
                1, MONTH, List.of(), List.of(), true));
        assertEquals("keep", Files.readString(destinationDirectory.resolve("keep.txt")));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    private List<List<String>> export(YearMonth month, List<EODDataPoint> eod,
            List<TillReportDataPoint> data, boolean live) throws Exception {
        var file = directory.resolve("xero.csv");
        XeroEodExporter.write(file.toFile(), 1, month, eod, data, live);
        return parse(Files.readString(file));
    }

    private static List<String> row(List<List<String>> rows, String contact, int day) {
        return rows.stream().filter(row -> row.getFirst().equals(contact)
                && row.get(1).equals(Integer.toString(day))).findFirst().orElseThrow();
    }

    private static EODDataPoint entry(LocalDate date, double cash) {
        return new EODDataPoint(true, date, 1, cash, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
    }

    private static TillReportDataPoint metric(LocalDate date, String key, double quantity, double amount) {
        var point = new TillReportDataPoint();
        point.setStoreID(1);
        point.setAssignedDate(date);
        point.setKey(key);
        point.setQuantity(quantity);
        point.setAmount(amount);
        return point;
    }

    /** Independent CSV reader, including escaped quotes and embedded line breaks. */
    private static List<List<String>> parse(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < csv.length(); index++) {
            char ch = csv.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (!quoted && ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (!quoted && (ch == '\r' || ch == '\n')) {
                if (ch == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else cell.append(ch);
        }
        assertFalse(quoted, "Unclosed CSV quote");
        if (!row.isEmpty() || !cell.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }
}
