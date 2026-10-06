package utils;

import models.EODDataPoint;
import models.TillReportDataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XeroEodExporterTest {
    @TempDir Path directory;

    @Test
    void xeroCsvContainsDailyCountsAndRecoveryIncludingDatesAfterTheFifteenth() throws Exception {
        var month = YearMonth.of(2026, 9);
        var data = new ArrayList<TillReportDataPoint>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            // Leave Sundays absent, as a successful Z query does for no-script days.
            if (day % 7 == 6) continue;
            data.add(metric(month.atDay(day), "Script Count", day * 2, 0));
            data.add(metric(month.atDay(day), "Govt Recovery", 0, day * 10.25));
        }
        var file = directory.resolve("xero.csv");

        XeroEodExporter.write(file.toFile(), 1, month, List.of(), data, true);

        var lines = Files.readAllLines(file);
        var header = Arrays.asList(lines.getFirst().split(",", -1));
        int scriptsColumn = header.indexOf("No. of scripts");
        int recoveryColumn = header.indexOf("Z Dispense Govt Cont");
        int unitAmountColumn = header.indexOf("*UnitAmount");
        assertEquals(30, lines.stream().filter(line -> line.startsWith("Cash Income,")).count());
        assertEquals(30, lines.stream().filter(line -> line.startsWith("Medicare PBS (Ex GST),")).count());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            final int date = day;
            var cash = row(lines, "Cash Income," + date + ",");
            var medicare = row(lines, "Medicare PBS (Ex GST)," + date + ",");
            double expectedCount = day % 7 == 6 ? 0 : day * 2;
            double expectedRecovery = day % 7 == 6 ? 0 : day * 10.25;
            assertEquals(expectedCount, Double.parseDouble(cash[scriptsColumn]), "Script count for day " + day);
            assertEquals(expectedRecovery, Double.parseDouble(cash[recoveryColumn]), "Recovery for day " + day);
            assertEquals(expectedRecovery, Double.parseDouble(medicare[2]), "Medicare income for day " + day);
            assertEquals(expectedRecovery, Double.parseDouble(medicare[unitAmountColumn]));
        }
    }

    @Test
    void successfulEmptyScriptMonthExportsExplicitZerosForEveryDay() throws Exception {
        var file = directory.resolve("empty.csv");
        var month = YearMonth.of(2028, 2);

        XeroEodExporter.write(file.toFile(), 1, month, List.of(), List.of(), true);

        var lines = Files.readAllLines(file);
        var header = Arrays.asList(lines.getFirst().split(",", -1));
        assertEquals(29, lines.stream().filter(line -> line.startsWith("Cash Income,")).count());
        for (var line : lines.stream().filter(value -> value.startsWith("Cash Income,")).toList()) {
            var columns = line.split(",", -1);
            assertEquals("0.0", columns[header.indexOf("No. of scripts")]);
            assertEquals("0.0", columns[header.indexOf("Z Dispense Govt Cont")]);
        }
    }

    @Test
    void incompleteScriptDayFailsBeforeOverwritingExistingExport() throws Exception {
        var file = directory.resolve("existing.csv");
        Files.writeString(file, "previous export");
        var month = YearMonth.of(2026, 9);
        var data = List.of(metric(month.atDay(16), "Govt Recovery", 0, 1500));

        var error = assertThrows(IllegalStateException.class,
                () -> XeroEodExporter.write(file.toFile(), 1, month, List.of(), data, true));

        assertTrue(error.getMessage().contains("2026-09-16"));
        assertEquals("previous export", Files.readString(file));
    }

    @Test
    void enteredDailyTenderFiguresAreStillIncludedAlongsideScriptTotals() throws Exception {
        var file = directory.resolve("entered.csv");
        var month = YearMonth.of(2026, 9);
        var date = month.atDay(16);
        var eod = new EODDataPoint(true, date, 1, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
        var data = List.of(metric(date, "Script Count", 50, 0),
                metric(date, "Govt Recovery", 0, 1500), metric(date, "Total Takings", 0, 100));

        XeroEodExporter.write(file.toFile(), 1, month, List.of(eod), data, true);

        var lines = Files.readAllLines(file);
        var header = Arrays.asList(lines.getFirst().split(",", -1));
        var row = row(lines, "Cash Income,16,");
        assertEquals("100.0", row[2]);
        assertEquals("50.0", row[3]);
        assertEquals("1500.0", row[header.indexOf("Z Dispense Govt Cont")]);
    }

    private String[] row(List<String> lines, String prefix) {
        return lines.stream().filter(line -> line.startsWith(prefix)).findFirst().orElseThrow().split(",", -1);
    }

    private TillReportDataPoint metric(LocalDate date, String key, double quantity, double amount) {
        var point = new TillReportDataPoint();
        point.setStoreID(1);
        point.setAssignedDate(date);
        point.setKey(key);
        point.setQuantity(quantity);
        point.setAmount(amount);
        return point;
    }
}
