package models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonthlySummaryDataPointTest {
    @Test
    void medicareIncomeCountsBeforeStaffEnterEndOfDayFigures() {
        var date = LocalDate.of(2026, 9, 16);
        var scripts = FXCollections.observableArrayList(metric(date, "Govt Recovery", 1500));

        var day = day(date, scripts, FXCollections.observableArrayList(), FXCollections.observableArrayList());

        assertEquals(1500, day.getTotalIncome());
        assertEquals(1500, day.getGovtRecovery());
        assertEquals(1500, day.getGpDollars());
    }

    @Test
    void addsMedicareOnceToTenderIncomeAndUsesItInProfit() {
        var date = LocalDate.of(2026, 9, 30);
        var scripts = FXCollections.observableArrayList(
                metric(date, "Govt Recovery", 2500), metric(date, "Gross Profit ($)", 800),
                metric(date, "Total Government Contribution", 500));
        var eods = FXCollections.observableArrayList(new EODDataPoint(
                true, date, 1, 100, 200, 300, 400, 500, 0, 0, 0, 0, 0, 0, ""));

        var day = day(date, scripts, eods, FXCollections.observableArrayList());

        assertEquals(4000, day.getTotalIncome());
        assertEquals(2800, day.getGpDollars());
    }

    @Test
    void monthTotalsIncludeRecoveryAfterTheCsvHandoverAndOnTheLastDay() {
        var start = LocalDate.of(2026, 9, 1);
        var scripts = FXCollections.observableArrayList(
                metric(start.plusDays(14), "Govt Recovery", 1000),
                metric(start.plusDays(15), "Govt Recovery", 1500),
                metric(start.plusDays(29), "Govt Recovery", 2500));
        ObservableList<MonthlySummaryDataPoint> days = FXCollections.observableArrayList();
        for (int i = 0; i < 30; i++) {
            days.add(day(start.plusDays(i), scripts, FXCollections.observableArrayList(), days));
        }

        var total = new MonthlySummaryDataPoint(days, true, 30);

        assertEquals(5000, total.getTotalIncome());
        assertEquals("$5,000.00", total.getTotalIncomeValue());
        assertEquals("$5,000.00", total.getGpDollarsValue());
    }

    private MonthlySummaryDataPoint day(LocalDate date, ObservableList<TillReportDataPoint> scripts,
            ObservableList<EODDataPoint> eods, ObservableList<MonthlySummaryDataPoint> previous) {
        return new MonthlySummaryDataPoint(date, scripts, eods, previous, 1, 30, 0, 0, 30, 0, 0);
    }

    private TillReportDataPoint metric(LocalDate date, String key, double amount) {
        var point = new TillReportDataPoint();
        point.setAssignedDate(date);
        point.setKey(key);
        point.setAmount(amount);
        return point;
    }
}
