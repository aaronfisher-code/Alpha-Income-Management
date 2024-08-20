package utils;
import models.Shift;
import models.LeaveRequest;
import services.LeaveService;
import services.RosterService;
import application.Main;

import java.sql.SQLException;
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

    public RosterUtils(Main main, LocalDate startDate, LocalDate endDate) throws SQLException {
        this.main = main;
        this.rosterService = new RosterService();
        this.leaveService = new LeaveService();

        fillDayDurations(startDate, endDate);
    }

    public RosterUtils(Main main, YearMonth yearMonth) throws SQLException {
        this.main = main;
        this.rosterService = new RosterService();
        this.leaveService = new LeaveService();
        this.yearMonth = yearMonth;

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        fillDayDurations(monthStart, monthEnd);
    }

    private void fillDayDurations(LocalDate startDate, LocalDate endDate) throws SQLException {
        allShifts = rosterService.getShifts(main.getCurrentStore().getStoreID(), startDate, endDate);
        allModifications = rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), startDate, endDate);
        allLeaveRequests = leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), startDate, endDate);

        for (LocalDate day = startDate; day.isBefore(endDate.plusDays(1)); day = day.plusDays(1)) {
            dayDurationMap.put(day, loadDayDuration(day));
        }
    }

    public double getDayDuration(LocalDate day) {
        return dayDurationMap.get(day);
    }

    private double loadDayDuration(LocalDate day){
        LocalTime earliestStart = LocalTime.MAX;
        LocalTime latestEnd = LocalTime.MIN;

        for (Shift s : allShifts) {
            boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), day) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), day) >= 0);
            boolean equalDay = s.getShiftStartDate().equals(day);
            boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(day);
            if ((equalDay || repeatShiftDay) && !pastEnd) {
                Shift updatedShift = s;
                boolean shiftIsModified = false;
                for(Shift m: allModifications){
                    if(m.getShiftID()==s.getShiftID() && m.getOriginalDate().equals(day)){
                        updatedShift = m;
                        shiftIsModified=true;
                    }
                }
                if(!shiftIsModified || (shiftIsModified&&updatedShift.getShiftStartDate()!=null&&updatedShift.getShiftStartDate().equals(day))){
                    boolean shiftOnLeave = false;
                    for(LeaveRequest lr: allLeaveRequests){
                        LocalDateTime shiftStart = LocalDateTime.of(day,updatedShift.getShiftStartTime());
                        LocalDateTime shiftEnd = LocalDateTime.of(day,updatedShift.getShiftEndTime());
                        if(lr.getEmployeeID().equals(updatedShift.getUsername())&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                            shiftOnLeave = true;
                        }
                    }
                    if(!shiftOnLeave) {
                        if (updatedShift.getShiftStartTime().isBefore(earliestStart)) {
                            earliestStart = updatedShift.getShiftStartTime();
                        }
                        if (updatedShift.getShiftEndTime().isAfter(latestEnd)) {
                            latestEnd = updatedShift.getShiftEndTime();
                        }
                    }
                }
            }
        }

        for(Shift m: allModifications){
            if(m.getShiftStartDate()!=null&&m.getShiftStartDate().equals(day)&&(!(m.getShiftStartDate().equals(m.getOriginalDate())))){
                boolean shiftOnLeave = false;
                for(LeaveRequest lr: allLeaveRequests){
                    LocalDateTime shiftStart = LocalDateTime.of(day,m.getShiftStartTime());
                    LocalDateTime shiftEnd = LocalDateTime.of(day,m.getShiftEndTime());
                    if(lr.getEmployeeID().equals(m.getUsername())&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                        shiftOnLeave = true;
                    }
                }
                if(!shiftOnLeave) {
                    if (m.getShiftStartTime().isBefore(earliestStart)) {
                        earliestStart = m.getShiftStartTime();
                    }
                    if (m.getShiftEndTime().isAfter(latestEnd)) {
                        latestEnd = m.getShiftEndTime();
                    }
                }
            }
        }
        if(earliestStart.equals(LocalTime.MAX)||latestEnd.equals(LocalTime.MIN)){
            return 0;
        }else {
            //todo: replace magic number 10 with store specific hours per day
            return (double) Duration.between(earliestStart, latestEnd).toHours() / 10;
        }
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
