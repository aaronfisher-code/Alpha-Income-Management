package utils;
import models.Shift;
import models.LeaveRequest;
import services.LeaveService;
import services.RosterService;
import application.Main;

import java.io.IOException;
import java.time.*;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class RosterUtils {

    private Main main;
    private RosterService rosterService;
    private LeaveService leaveService;
    private YearMonth yearMonth;

    private List<Shift> allShifts = new ArrayList<>();
    private List<Shift> allModifications = new ArrayList<>();
    private List<LeaveRequest> allLeaveRequests = new ArrayList<>();

    private Map<LocalDate, Double> dayDurationMap = new HashMap<>();

    public RosterUtils(Main main, LocalDate startDate, LocalDate endDate) throws IOException {
        this.main = main;
        this.rosterService = new RosterService();
        this.leaveService = new LeaveService();

        fillDayDurations(startDate, endDate);
    }

    public RosterUtils(Main main, YearMonth yearMonth) throws IOException {
        this.main = main;
        this.rosterService = new RosterService();
        this.leaveService = new LeaveService();
        this.yearMonth = yearMonth;

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        fillDayDurations(monthStart, monthEnd);
    }

    private void fillDayDurations(LocalDate startDate, LocalDate endDate){
        allShifts = rosterService.getShifts(main.getCurrentStore().getStoreID(), startDate, endDate);
        allModifications = rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), startDate, endDate);
        allLeaveRequests = leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), startDate, endDate);

        for (LocalDate day = startDate; day.isBefore(endDate.plusDays(1)); day = day.plusDays(1)) {
            dayDurationMap.put(day, loadDayDuration(day));
        }
    }

    public double getDayDuration(LocalDate day) {
        return dayDurationMap.getOrDefault(day,0.0);
    }

    private double loadDayDuration(LocalDate day) {
        LocalTime earliestStart = LocalTime.MAX;
        LocalTime latestEnd     = LocalTime.MIN;

        // 1) Gather all “active” shifts or modifications on this day
        List<Shift> toConsider = new ArrayList<>();

        for (Shift s : allShifts) {
            // **NULL GUARD**: skip any shift missing its key data
            if (s.getShiftStartDate() == null
                    || s.getShiftStartTime() == null
                    || s.getShiftEndTime()   == null) {
                continue;
            }

            boolean repeatDay = s.isRepeating()
                    && DAYS.between(s.getShiftStartDate(), day) >= 0
                    && DAYS.between(s.getShiftStartDate(), day) % s.getDaysPerRepeat() == 0;
            boolean sameDay   = s.getShiftStartDate().equals(day);
            boolean pastEnd   = s.getShiftEndDate() != null
                    && s.getShiftEndDate().isBefore(day);

            if ((sameDay || repeatDay) && !pastEnd) {
                // look for a modification on exactly this original shift & day
                Shift mod = allModifications.stream()
                        .filter(m -> m.getShiftID() == s.getShiftID()
                                && day.equals(m.getOriginalDate()))
                        .findFirst()
                        .orElse(null);

                if (mod != null
                        && mod.getShiftStartDate() != null
                        && mod.getShiftStartDate().equals(day)
                        && mod.getShiftStartTime() != null
                        && mod.getShiftEndTime()   != null) {
                    toConsider.add(mod);
                } else {
                    toConsider.add(s);
                }
            }
        }

        // also include any “pure” modifications that moved a shift onto this day
        for (Shift m : allModifications) {
            if (m.getShiftStartDate() == null
                    || m.getShiftStartTime() == null
                    || m.getShiftEndTime()   == null) {
                continue;
            }
            if (day.equals(m.getShiftStartDate())
                    && !day.equals(m.getOriginalDate())) {
                toConsider.add(m);
            }
        }

        // 2) For each candidate, carve out working segments around any leave
        for (Shift shift : toConsider) {
            LocalTime sTime = shift.getShiftStartTime();
            LocalTime eTime = shift.getShiftEndTime();

            // sanity check
            if (!sTime.isBefore(eTime)) continue;

            LocalDateTime shiftStartDT = LocalDateTime.of(day, sTime);
            LocalDateTime shiftEndDT   = LocalDateTime.of(day, eTime);

            // collect overlapping leaves
            List<LeaveRequest> relevantLeaves = new ArrayList<>();
            for (LeaveRequest lr : allLeaveRequests) {
                if (lr.getUserID() == shift.getUserID()
                        && lr.getFromDate().isBefore(shiftEndDT)
                        && lr.getToDate().isAfter(shiftStartDT)) {
                    relevantLeaves.add(lr);
                }
            }

            // build boundary list
            List<LocalTime> boundaries = new ArrayList<>();
            boundaries.add(sTime);
            boundaries.add(eTime);
            for (LeaveRequest lr : relevantLeaves) {
                LocalDateTime ls = lr.getFromDate().isBefore(shiftStartDT) ? shiftStartDT : lr.getFromDate();
                LocalDateTime le = lr.getToDate().isAfter( shiftEndDT  ) ? shiftEndDT   : lr.getToDate();
                boundaries.add(ls.toLocalTime());
                boundaries.add(le.toLocalTime());
            }
            boundaries = boundaries.stream()
                    .distinct()
                    .sorted()
                    .toList();

            // build & evaluate each tiny sub‑segment
            for (int i = 0; i < boundaries.size() - 1; i++) {
                LocalTime segStart = boundaries.get(i);
                LocalTime segEnd   = boundaries.get(i + 1);
                if (!segStart.isBefore(segEnd)) continue;

                LocalDateTime subStart = LocalDateTime.of(day, segStart);
                LocalDateTime subEnd   = LocalDateTime.of(day, segEnd);

                // check if fully covered by any leave
                boolean onLeave = false;
                for (LeaveRequest lr : relevantLeaves) {
                    if (!lr.getFromDate().isAfter(subStart)
                            && !lr.getToDate().isBefore(subEnd)) {
                        onLeave = true;
                        break;
                    }
                }

                if (!onLeave) {
                    if (segStart.isBefore(earliestStart)) earliestStart = segStart;
                    if (segEnd.isAfter( latestEnd   )) latestEnd     = segEnd;
                }
            }
        }

        // 3) compute duration
        if (earliestStart.equals(LocalTime.MAX) || latestEnd.equals(LocalTime.MIN)) {
            return 0.0;
        }
        long openHours = Duration.between(earliestStart, latestEnd).toHours();
        return (double) openHours / main.getCurrentStore().getStoreHours();
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
