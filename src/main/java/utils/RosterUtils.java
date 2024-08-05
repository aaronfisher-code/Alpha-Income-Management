package utils;
import models.Shift;
import models.LeaveRequest;
import services.RosterService;
import application.Main;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class RosterUtils {

    private Main main;
    private RosterService rosterService;
    private YearMonth yearMonth;

    private List<Shift> allShifts = new ArrayList<>();
    private List<Shift> allModifications = new ArrayList<>();
    private List<LeaveRequest> allLeaveRequests = new ArrayList<>();

    private Map<LocalDate, Double> dayDurationMap = new HashMap<>();

    public RosterUtils(Main main, LocalDate startDate, LocalDate endDate) {
        this.main = main;
        this.rosterService = new RosterService();

        fillDayDurations(startDate, endDate);
    }

    public RosterUtils(Main main, YearMonth yearMonth) {
        this.main = main;
        this.rosterService = new RosterService();
        this.yearMonth = yearMonth;

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        fillDayDurations(monthStart, monthEnd);
    }

    private void fillDayDurations(LocalDate startDate, LocalDate endDate) {
        try {
            allShifts = rosterService.getShifts(main.getCurrentStore().getStoreID(), startDate, endDate);
            allModifications = rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), startDate, endDate);
            allLeaveRequests = rosterService.getLeaveRequests(main.getCurrentStore().getStoreID(), startDate, endDate);
        } catch (SQLException ex) {
            System.err.println("Error fetching roster data: " + ex.getMessage());
            // Handle the exception appropriately (e.g., show an error message to the user)
        }

        for (LocalDate day = startDate; day.isBefore(endDate.plusDays(1)); day = day.plusDays(1)) {
            dayDurationMap.put(day, loadDayDuration(day));
        }
    }

    public double getDayDuration(LocalDate day) {
        return dayDurationMap.get(day);
    }

    private double loadDayDuration(LocalDate day) {
        LocalTime earliestStart = LocalTime.MAX;
        LocalTime latestEnd = LocalTime.MIN;

        for (Shift s : allShifts) {
            if (isShiftActive(s, day)) {
                Shift updatedShift = getUpdatedShift(s, day);
                if (isShiftValid(updatedShift, day) && !isShiftOnLeave(updatedShift, day)) {
                    earliestStart = earliestStart.isBefore(updatedShift.getShiftStartTime()) ? earliestStart : updatedShift.getShiftStartTime();
                    latestEnd = latestEnd.isAfter(updatedShift.getShiftEndTime()) ? latestEnd : updatedShift.getShiftEndTime();
                }
            }
        }

        for (Shift m : allModifications) {
            if (m.getShiftStartDate() != null && m.getShiftStartDate().equals(day) && !m.getShiftStartDate().equals(m.getOriginalDate())) {
                if (!isShiftOnLeave(m, day)) {
                    earliestStart = earliestStart.isBefore(m.getShiftStartTime()) ? earliestStart : m.getShiftStartTime();
                    latestEnd = latestEnd.isAfter(m.getShiftEndTime()) ? latestEnd : m.getShiftEndTime();
                }
            }
        }

        if (earliestStart.equals(LocalTime.MAX) || latestEnd.equals(LocalTime.MIN)) {
            return 0;
        } else {
            //todo: replace magic number 10 with store specific hours per day
            return (double) Duration.between(earliestStart, latestEnd).toHours() / 10;
        }
    }

    private boolean isShiftActive(Shift s, LocalDate day) {
        boolean repeatShiftDay = s.isRepeating() && DAYS.between(s.getShiftStartDate(), day) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), day) >= 0;
        boolean equalDay = s.getShiftStartDate().equals(day);
        boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(day);
        return (equalDay || repeatShiftDay) && !pastEnd;
    }

    private Shift getUpdatedShift(Shift s, LocalDate day) {
        for (Shift m : allModifications) {
            if (m.getShiftID() == s.getShiftID() && m.getOriginalDate().equals(day)) {
                return m;
            }
        }
        return s;
    }

    private boolean isShiftValid(Shift s, LocalDate day) {
        return s.getShiftStartDate() == null || s.getShiftStartDate().equals(day);
    }

    private boolean isShiftOnLeave(Shift s, LocalDate day) {
        LocalDateTime shiftStart = LocalDateTime.of(day, s.getShiftStartTime());
        LocalDateTime shiftEnd = LocalDateTime.of(day, s.getShiftEndTime());
        for (LeaveRequest lr : allLeaveRequests) {
            if (lr.getEmployeeID().equals(s.getUsername()) && lr.getFromDate().isBefore(shiftEnd) && lr.getToDate().isAfter(shiftStart)) {
                return true;
            }
        }
        return false;
    }

    public int getOpenDays() {
        return (int) dayDurationMap.values().stream().filter(duration -> duration > 0).count();
    }

    public double getOpenDuration() {
        return dayDurationMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public int getPartialDays() {
        return (int) dayDurationMap.values().stream().filter(duration -> duration > 0 && duration < 0.5).count();
    }

    public int getTotalDays() {
        return yearMonth.lengthOfMonth();
    }

    public double getDuration(LocalDate startDate, LocalDate endDate) {
        return dayDurationMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(startDate) && !entry.getKey().isAfter(endDate))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }
}
