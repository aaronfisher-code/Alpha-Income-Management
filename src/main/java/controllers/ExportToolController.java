package controllers;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import models.LeaveRequest;
import models.Shift;
import models.SpecialDateObj;
import models.User;
import services.LeaveService;
import services.RosterService;
import services.UserService;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class ExportToolController extends PageController {
    @FXML
    private MFXComboBox<String> monthPicker;
    @FXML
    private MFXTextField yearPicker;
    @FXML
    private MFXProgressBar progressBar;

    private RosterPageController parent;
    private RosterService rosterService;
    private LeaveService leaveService;
    private UserService userService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DURATION_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    @FXML
    public void initialize() {
        try {
            rosterService = new RosterService();
            leaveService = new LeaveService();
            userService = new UserService();
            executor = Executors.newCachedThreadPool();
        } catch (IOException ex) {
            parent.getDialogPane().showError("Failed to initialize services", ex);
        }
    }

    public void setParent(RosterPageController parent) {
        this.parent = parent;
    }

    @Override
    public void fill() {
        // Add months to the month picker
        for (int i = 0; i < 12; i++) {
            monthPicker.getItems().add(new DateFormatSymbols().getMonths()[i]);
        }

        // Set default value to current month and year
        monthPicker.selectItem(new DateFormatSymbols().getMonths()[LocalDate.now().getMonthValue() - 1]);
        yearPicker.setText(String.valueOf(LocalDate.now().getYear()));
    }

    private String calculateShiftDuration(LocalTime startTime, LocalTime endTime, int thirtyMinBreaks, int tenMinBreaks) {
        Duration duration;

        // Handle shifts that cross midnight
        if (endTime.isBefore(startTime)) {
            duration = Duration.between(startTime, LocalTime.MAX)
                    .plus(Duration.between(LocalTime.MIN, endTime))
                    .plusMinutes(1); // Add 1 minute to account for midnight
        } else {
            duration = Duration.between(startTime, endTime);
        }

        // Convert duration to decimal hours
        double hours = duration.toMinutes() / 60.0;

        // Format to 2 decimal places
        return String.format("%.2f", hours);
    }

    public void copyToClipboard() {
        if (!validateInputs()) {
            return;
        }

        progressBar.setVisible(true);
        Task<String> exportTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                LocalDate startDate = LocalDate.of(
                        Integer.parseInt(yearPicker.getText()),
                        monthPicker.getItems().indexOf(monthPicker.getValue()) + 1,
                        1
                );
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                // Fetch all required data concurrently
                CompletableFuture<List<SpecialDateObj>> specialDatesFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getSpecialDates(startDate, endDate);
                    } catch (Exception e) {
                        // Return an empty list if there are no special dates
                        return new ArrayList<>();
                    }
                }, executor);

                CompletableFuture<List<User>> usersFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return userService.getAllUsers();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);

                CompletableFuture<List<Shift>> shiftsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getShifts(main.getCurrentStore().getStoreID(), startDate, endDate);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);

                CompletableFuture<List<Shift>> modificationsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), startDate, endDate);
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }
                }, executor);

                CompletableFuture<List<LeaveRequest>> leaveRequestsFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), startDate, endDate);
                    } catch (Exception e) {
                        // Return an empty list if there are no leave requests
                        return new ArrayList<>();
                    }
                }, executor);

                // Wait for all data to be fetched
                List<SpecialDateObj> specialDates = specialDatesFuture.get();
                List<User> users = usersFuture.get();
                List<Shift> shifts = shiftsFuture.get();
                List<Shift> modifications = modificationsFuture.get();
                List<LeaveRequest> leaveRequests = leaveRequestsFuture.get();

                // If no shifts found, show message and return empty string
                if (shifts.isEmpty()) {
                    throw new Exception("No shifts found for the selected month.");
                }

                // Create lookup map for users
                Map<Integer, User> userMap = users.stream()
                        .collect(Collectors.toMap(User::getUserID, user -> user));

                // Build the export string
                StringBuilder export = new StringBuilder();

                // Add header row
                export.append("Date\tUserID\tName\tStartTime\tFinishTime\tTotal Hours\t30min Breaks\t10min Breaks\tPublicHoliday\tLeaveType\n");

                // Process each day in the selected month
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    LocalDate currentDate = date;

                    boolean isPublicHoliday = specialDates.stream()
                            .anyMatch(sd -> sd.getEventDate().equals(currentDate) &&
                                    sd.getStoreStatus().equals("Public Holiday"));

                    // Get all shifts for the current day (including repeating shifts)
                    List<Shift> dayShifts = new ArrayList<>();
                    dayShifts.addAll(shifts.stream()
                            .filter(s -> s.getShiftStartDate().equals(currentDate))
                            .toList());

                    dayShifts.addAll(shifts.stream()
                            .filter(s -> s.isRepeating() &&
                                    DAYS.between(s.getShiftStartDate(), currentDate) % s.getDaysPerRepeat() == 0 &&
                                    !currentDate.isBefore(s.getShiftStartDate()) &&
                                    (s.getShiftEndDate() == null || !currentDate.isAfter(s.getShiftEndDate())))
                            .toList());

                    // Process each shift
                    for (Shift shift : dayShifts) {
                        // Check for modifications
                        Shift effectiveShift = shift;
                        boolean shiftIsModified = false;

                        for (Shift mod : modifications) {
                            if (mod.getShiftID() == shift.getShiftID() &&
                                    mod.getOriginalDate().equals(currentDate)) {
                                effectiveShift = mod;
                                shiftIsModified = true;
                                break;
                            }
                        }

                        // Skip if shift is modified and moved to different date
                        if (shiftIsModified && effectiveShift.getShiftStartDate() != null &&
                                !effectiveShift.getShiftStartDate().equals(currentDate)) {
                            continue;
                        }

                        User user = userMap.get(effectiveShift.getUserID());
                        if (user == null) continue;

                        Shift finalEffectiveShift = effectiveShift;
                        String leaveType = leaveRequests.stream()
                                .filter(lr -> lr.getUserID() == finalEffectiveShift.getUserID())
                                .filter(lr -> {
                                    LocalDateTime shiftStart = LocalDateTime.of(currentDate, finalEffectiveShift.getShiftStartTime());
                                    LocalDateTime shiftEnd = LocalDateTime.of(currentDate, finalEffectiveShift.getShiftEndTime());
                                    return lr.getFromDate().isBefore(shiftEnd) && lr.getToDate().isAfter(shiftStart);
                                })
                                .map(LeaveRequest::getLeaveType)
                                .findFirst()
                                .orElse("");

                        // Calculate total hours
                        String totalHours = calculateShiftDuration(
                                effectiveShift.getShiftStartTime(),
                                effectiveShift.getShiftEndTime(),
                                effectiveShift.getThirtyMinBreaks(),
                                effectiveShift.getTenMinBreaks()
                        );

                        // Add row to export
                        export.append(String.format("%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                currentDate.format(DATE_FORMATTER),
                                user.getUserID(),
                                user.getFirst_name(),
                                user.getLast_name(),
                                effectiveShift.getShiftStartTime().format(TIME_FORMATTER),
                                effectiveShift.getShiftEndTime().format(TIME_FORMATTER),
                                totalHours,
                                effectiveShift.getThirtyMinBreaks(),
                                effectiveShift.getTenMinBreaks(),
                                isPublicHoliday ? "Yes" : "No",
                                leaveType
                        ));
                    }

                    // Add modifications that create new shifts on this date
                    for (Shift mod : modifications) {
                        if (mod.getShiftStartDate() != null &&
                                mod.getShiftStartDate().equals(currentDate) &&
                                !mod.getShiftStartDate().equals(mod.getOriginalDate())) {

                            User user = userMap.get(mod.getUserID());
                            if (user == null) continue;

                            String leaveType = leaveRequests.stream()
                                    .filter(lr -> lr.getUserID() == mod.getUserID())
                                    .filter(lr -> {
                                        LocalDateTime shiftStart = LocalDateTime.of(currentDate, mod.getShiftStartTime());
                                        LocalDateTime shiftEnd = LocalDateTime.of(currentDate, mod.getShiftEndTime());
                                        return lr.getFromDate().isBefore(shiftEnd) && lr.getToDate().isAfter(shiftStart);
                                    })
                                    .map(LeaveRequest::getLeaveType)
                                    .findFirst()
                                    .orElse("");

                            String totalHours = calculateShiftDuration(
                                    mod.getShiftStartTime(),
                                    mod.getShiftEndTime(),
                                    mod.getThirtyMinBreaks(),
                                    mod.getTenMinBreaks()
                            );

                            export.append(String.format("%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                    currentDate.format(DATE_FORMATTER),
                                    user.getUserID(),
                                    user.getFirst_name(),
                                    user.getLast_name(),
                                    mod.getShiftStartTime().format(TIME_FORMATTER),
                                    mod.getShiftEndTime().format(TIME_FORMATTER),
                                    totalHours,
                                    mod.getThirtyMinBreaks(),
                                    mod.getTenMinBreaks(),
                                    isPublicHoliday ? "Yes" : "No",
                                    leaveType
                            ));
                        }
                    }
                }

                return export.toString();
            }
        };

        exportTask.setOnSucceeded(e -> {
            String exportData = exportTask.getValue();
            ClipboardContent content = new ClipboardContent();
            content.putString(exportData);
            Clipboard.getSystemClipboard().setContent(content);
            parent.getDialogPane().showInformation("Success", "Data has been copied to clipboard");
            progressBar.setVisible(false);
        });

        exportTask.setOnFailed(e -> {
            Throwable exception = exportTask.getException();
            String errorMessage = exception.getMessage();
            if (errorMessage.equals("No shifts found for the selected month.")) {
                parent.getDialogPane().showInformation("No Data", errorMessage);
            } else {
                parent.getDialogPane().showError("Export Failed", "Failed to export data", exception);
            }
            progressBar.setVisible(false);
        });

        executor.submit(exportTask);
    }

    private boolean validateInputs() {
        if (monthPicker.getValue() == null || monthPicker.getValue().isEmpty()) {
            parent.getDialogPane().showError("Validation Error", "Please select a month");
            return false;
        }

        try {
            int year = Integer.parseInt(yearPicker.getText());
            if (year < 1900 || year > 2100) {
                parent.getDialogPane().showError("Validation Error", "Please enter a valid year between 1900 and 2100");
                return false;
            }
        } catch (NumberFormatException e) {
            parent.getDialogPane().showError("Validation Error", "Please enter a valid year");
            return false;
        }

        return true;
    }
}