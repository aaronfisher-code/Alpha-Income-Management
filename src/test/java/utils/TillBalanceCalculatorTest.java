package utils;

import models.EODDataPoint;
import models.TillReportDataPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TillBalanceCalculatorTest {
    @Test
    void calculatesFourteenSeptemberBalanceFromTheReportedTotalTakings() {
        var day = new EODDataPoint(
                true, LocalDate.of(2026, 9, 14), 1,
                975.00, 3_233.76, 223.58, 0, 0,
                0, 0, 0, 0, 0, 0, "");

        TillBalanceCalculator.calculate(
                List.of(day),
                List.of(till(LocalDate.of(2026, 9, 14), 4_434.74)));

        assertEquals(-2.40, day.getTillBalance(), 0.001);
        assertEquals(-2.40, day.getRunningTillBalance(), 0.001);
    }

    @Test
    void cachedTakingsOnAnUnenteredWeekendCannotChangeTheNextRunningBalance() {
        var saturday = eod(true, LocalDate.of(2026, 9, 5), 100);
        var weekend = eod(false, LocalDate.of(2026, 9, 6), 0);
        var monday = eod(true, LocalDate.of(2026, 9, 7), 100);

        TillBalanceCalculator.calculate(
                List.of(saturday, weekend, monday),
                List.of(
                        till(LocalDate.of(2026, 9, 5), 95),
                        till(LocalDate.of(2026, 9, 6), 582.41),
                        till(LocalDate.of(2026, 9, 7), 99.81)),
                date -> date.equals(LocalDate.of(2026, 9, 6)));

        assertEquals(5, saturday.getTillBalance(), 0.001);
        assertEquals(5, saturday.getRunningTillBalance(), 0.001);
        assertFalse(weekend.isTillTakingsAvailable());
        assertTrue(weekend.isRunningTillBalanceAvailable());
        assertEquals(5, weekend.getRunningTillBalance(), 0.001);
        assertEquals(0.19, monday.getTillBalance(), 0.001);
        assertEquals(5.19, monday.getRunningTillBalance(), 0.001);
    }

    @Test
    void zeroFilledEodRowOnAClosedWeekendCannotChangeTheNextRunningBalance() {
        var saturday = eod(true, LocalDate.of(2026, 9, 5), 100);
        var closedDay = eod(true, LocalDate.of(2026, 9, 6), 0);
        var monday = eod(true, LocalDate.of(2026, 9, 7), 100);

        TillBalanceCalculator.calculate(
                List.of(saturday, closedDay, monday),
                List.of(
                        till(LocalDate.of(2026, 9, 5), 95),
                        till(LocalDate.of(2026, 9, 6), 582.41),
                        till(LocalDate.of(2026, 9, 7), 99.81)),
                date -> date.equals(LocalDate.of(2026, 9, 6)));

        assertFalse(closedDay.isTillTakingsAvailable());
        assertEquals(5, closedDay.getRunningTillBalance(), 0.001);
        assertEquals(5.19, monday.getRunningTillBalance(), 0.001);
    }

    private static EODDataPoint eod(boolean inDatabase, LocalDate date, double cash) {
        return new EODDataPoint(
                inDatabase, date, 1, cash, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, "");
    }

    private static TillReportDataPoint till(LocalDate date, double amount) {
        var point = new TillReportDataPoint();
        point.setAssignedDate(date);
        point.setKey("Total Takings");
        point.setAmount(amount);
        return point;
    }
}
