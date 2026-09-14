package utils;

import models.EODDataPoint;
import models.TillReportDataPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Applies Z total takings to the entered EOD rows in date order.
 *
 * <p>A cached till row can outlive a corrected period assignment. It is not
 * valid to apply that row to a calendar date that has no EOD entry or is a
 * closed roster day, because the missing payment values would turn the whole
 * period into a false variance and contaminate every following running
 * balance.</p>
 */
public final class TillBalanceCalculator {
    private static final String TOTAL_TAKINGS = "Total Takings";

    private TillBalanceCalculator() {}

    public static void calculate(
            List<EODDataPoint> eodDataPoints,
            List<TillReportDataPoint> tillReportDataPoints) {
        calculate(eodDataPoints, tillReportDataPoints, ignored -> false);
    }

    public static void calculate(
            List<EODDataPoint> eodDataPoints,
            List<TillReportDataPoint> tillReportDataPoints,
            Predicate<LocalDate> nonWorkingDay) {
        Objects.requireNonNull(nonWorkingDay, "nonWorkingDay");
        double runningTillBalance = 0;
        boolean runningBalanceKnown = true;

        for (EODDataPoint eod : eodDataPoints) {
            if (!eod.isInDB() || nonWorkingDay.test(eod.getDate())) {
                // Do not use a cached Z row for a date with no entered EOD
                // values or a closed/non-working day. Carry the last known
                // cumulative value unchanged.
                eod.setTillTakingsAvailable(false);
                eod.setRunningTillBalanceAvailable(runningBalanceKnown);
                if (runningBalanceKnown) {
                    eod.setRunningTillBalance(runningTillBalance);
                }
                continue;
            }

            List<TillReportDataPoint> matchingTillReports = tillReportDataPoints.stream()
                    .filter(till -> till.getAssignedDate() != null
                            && till.getAssignedDate().equals(eod.getDate())
                            && TOTAL_TAKINGS.equals(till.getKey()))
                    .toList();
            if (matchingTillReports.isEmpty()) {
                eod.setTillTakingsAvailable(false);
                eod.setRunningTillBalanceAvailable(false);
                runningBalanceKnown = false;
                continue;
            }

            // Multiple segments can share a business date after a register
            // restart, so aggregate all matching rows.
            double totalTakings = matchingTillReports.stream()
                    .mapToDouble(TillReportDataPoint::getAmount)
                    .sum();
            eod.calculateTillBalances(totalTakings, runningTillBalance);
            if (runningBalanceKnown) {
                runningTillBalance = eod.getRunningTillBalance();
            } else {
                // The day-level variance is known, but its cumulative value
                // cannot be known until the missing period is resolved.
                eod.setRunningTillBalanceAvailable(false);
            }
        }
    }
}
