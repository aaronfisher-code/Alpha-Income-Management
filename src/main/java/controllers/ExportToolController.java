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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                export.append("Date\tUserID\tName\tStartTime\tFinishTime\tPublicHoliday\tLeaveType\n");

                // Process each day in the selected month
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    LocalDate currentDate = date; // Effectively final copy for lambda

                    // Check if it's a public holiday
                    boolean isPublicHoliday = specialDates.stream()
                            .anyMatch(sd -> sd.getEventDate().equals(currentDate) &&
                                    sd.getStoreStatus().equals("Public Holiday"));

                    // Process all shifts for this day
                    List<Shift> dayShifts = new ArrayList<>();

                    // Add regular shifts
                    dayShifts.addAll(shifts.stream()
                            .filter(s -> s.getShiftStartDate().equals(currentDate))
                            .toList());

                    // Add repeating shifts
                    dayShifts.addAll(shifts.stream()
                            .filter(s -> s.isRepeating() &&
                                    DAYS.between(s.getShiftStartDate(), currentDate) % s.getDaysPerRepeat() == 0 &&
                                    !currentDate.isBefore(s.getShiftStartDate()) &&
                                    (s.getShiftEndDate() == null || !currentDate.isAfter(s.getShiftEndDate())))
                            .toList());

                    // Process each shift
                    for (Shift shift : dayShifts) {
                        User user = userMap.get(shift.getUserID());
                        if (user == null) continue;

                        // Check for leave
                        String leaveType = leaveRequests.stream()
                                .filter(lr -> lr.getUserID() == shift.getUserID())
                                .filter(lr -> {
                                    LocalDateTime shiftStart = LocalDateTime.of(currentDate, shift.getShiftStartTime());
                                    LocalDateTime shiftEnd = LocalDateTime.of(currentDate, shift.getShiftEndTime());
                                    return lr.getFromDate().isBefore(shiftEnd) && lr.getToDate().isAfter(shiftStart);
                                })
                                .map(LeaveRequest::getLeaveType)
                                .findFirst()
                                .orElse("");

                        // Add row to export
                        export.append(String.format("%s\t%d\t%s %s\t%s\t%s\t%s\t%s\n",
                                currentDate.format(DATE_FORMATTER),
                                user.getUserID(),
                                user.getFirst_name(),
                                user.getLast_name(),
                                shift.getShiftStartTime().format(TIME_FORMATTER),
                                shift.getShiftEndTime().format(TIME_FORMATTER),
                                isPublicHoliday ? "Yes" : "No",
                                leaveType
                        ));
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