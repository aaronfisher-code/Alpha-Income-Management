package controllers;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import models.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import services.LeaveService;
import services.RosterService;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class ExportToolController extends PageController {
    @FXML
    private MFXComboBox<String> monthPicker;
    @FXML
    private MFXTextField yearPicker;
    @FXML
    private MFXProgressBar progressBarExcel;
    @FXML
    private MFXProgressBar progressBarPDF;

    private RosterPageController parent;
    private RosterService rosterService;
    private LeaveService leaveService;
    private UserService userService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HOUR_ONLY_FMT = DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR_MIN_FMT = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

    private static final float MARGIN = 40;
    private static final float Y_START_PAGE_OFFSET = 25;
    private static final float BOTTOM_PAGE_MARGIN_THRESHOLD = MARGIN + 30;
    private static final float EMPLOYEE_ROW_HEIGHT = 20;
    private static final float DAY_HEADER_ROW_HEIGHT = 18;
    private static final float ROLE_HEADER_HEIGHT = 10;
    private static final float WEEK_TITLE_HEIGHT = 14;
    private static final float INTER_TABLE_SPACING = 15;
    private static final float CELL_PADDING = 3;


    private static final float MAIN_TITLE_FONT_SIZE = 13;
    private static final float WEEK_TITLE_FONT_SIZE = 11;
    private static final float ROLE_DIVIDER_FONT_SIZE = 7;
    private static final float DAY_HEADER_FONT_SIZE = 7.5f;
    private static final float EMPLOYEE_NAME_FONT_SIZE = 7.5f;
    private static final float SHIFT_TIME_FONT_SIZE = 7f;

    private PDType1Font fontBold;
    private PDType1Font fontRegular;
    private PDType1Font fontItalic;

    private LocalDate overallFortnightStartDate;
    private LocalDate overallFortnightEndDate;

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

        progressBarExcel.setVisible(true);
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
                        // Look for a modification for this shift on the current day.
                        Shift modificationForShift = modifications.stream()
                                .filter(mod -> mod.getShiftID() == shift.getShiftID() &&
                                        mod.getOriginalDate().equals(currentDate))
                                .findFirst().orElse(null);

                        if (modificationForShift != null) {
                            // 1) Export the original shift ONLY if there is a leave record (i.e. at least one segment is on leave)
                            User originalUser = userMap.get(shift.getUserID());
                            if (originalUser != null) {
                                List<ShiftSegment> originalSegments = buildShiftSegments(shift, date, leaveRequests);
                                for (ShiftSegment seg : originalSegments) {
                                    // Only export segments that are on leave
                                    if (seg.getStartTime().equals(seg.getEndTime()) || !seg.isOnLeave()) {
                                        continue;
                                    }
                                    String leaveType = "";
                                    LocalDateTime subStart = LocalDateTime.of(date, seg.getStartTime());
                                    LocalDateTime subEnd   = LocalDateTime.of(date, seg.getEndTime());
                                    leaveType = leaveRequests.stream()
                                            .filter(lr -> lr.getUserID() == shift.getUserID())
                                            .filter(lr -> !lr.getFromDate().isAfter(subStart)
                                                    && !lr.getToDate().isBefore(subEnd))
                                            .map(LeaveRequest::getLeaveType)
                                            .findFirst()
                                            .orElse("On Leave");
                                    String totalHours = calculateShiftDuration(
                                            seg.getStartTime(),
                                            seg.getEndTime(),
                                            shift.getThirtyMinBreaks(),
                                            shift.getTenMinBreaks()
                                    );
                                    export.append(String.format(
                                            "%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                            date.format(DATE_FORMATTER),
                                            originalUser.getUserID(),
                                            originalUser.getFirst_name(),
                                            originalUser.getLast_name(),
                                            seg.getStartTime().format(TIME_FORMATTER),
                                            seg.getEndTime().format(TIME_FORMATTER),
                                            totalHours,
                                            shift.getThirtyMinBreaks(),
                                            shift.getTenMinBreaks(),
                                            isPublicHoliday ? "Yes" : "No",
                                            leaveType
                                    ));
                                }
                            }

                            // 2) Export the covering (modified) shift if it still falls on the current day
                            if (modificationForShift.getShiftStartDate() == null ||
                                    modificationForShift.getShiftStartDate().equals(currentDate)) {
                                User coveringUser = userMap.get(modificationForShift.getUserID());
                                if (coveringUser != null) {
                                    List<ShiftSegment> coveringSegments = buildShiftSegments(modificationForShift, date, leaveRequests);
                                    for (ShiftSegment seg : coveringSegments) {
                                        if (seg.getStartTime().equals(seg.getEndTime())) continue;
                                        String leaveType = "";
                                        if (seg.isOnLeave()) {
                                            LocalDateTime subStart = LocalDateTime.of(date, seg.getStartTime());
                                            LocalDateTime subEnd   = LocalDateTime.of(date, seg.getEndTime());
                                            leaveType = leaveRequests.stream()
                                                    .filter(lr -> lr.getUserID() == modificationForShift.getUserID())
                                                    .filter(lr -> !lr.getFromDate().isAfter(subStart)
                                                            && !lr.getToDate().isBefore(subEnd))
                                                    .map(LeaveRequest::getLeaveType)
                                                    .findFirst()
                                                    .orElse("On Leave");
                                        }
                                        String totalHours = calculateShiftDuration(
                                                seg.getStartTime(),
                                                seg.getEndTime(),
                                                modificationForShift.getThirtyMinBreaks(),
                                                modificationForShift.getTenMinBreaks()
                                        );
                                        export.append(String.format(
                                                "%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                                date.format(DATE_FORMATTER),
                                                coveringUser.getUserID(),
                                                coveringUser.getFirst_name(),
                                                coveringUser.getLast_name(),
                                                seg.getStartTime().format(TIME_FORMATTER),
                                                seg.getEndTime().format(TIME_FORMATTER),
                                                totalHours,
                                                modificationForShift.getThirtyMinBreaks(),
                                                modificationForShift.getTenMinBreaks(),
                                                isPublicHoliday ? "Yes" : "No",
                                                leaveType
                                        ));
                                    }
                                }
                            }
                        } else {
                            // Normal processing if no modification exists.
                            User user = userMap.get(shift.getUserID());
                            if (user == null) continue;
                            List<ShiftSegment> segments = buildShiftSegments(shift, date, leaveRequests);
                            for (ShiftSegment seg : segments) {
                                if (seg.getStartTime().equals(seg.getEndTime())) continue;
                                String leaveType = "";
                                if (seg.isOnLeave()) {
                                    LocalDateTime subStart = LocalDateTime.of(date, seg.getStartTime());
                                    LocalDateTime subEnd   = LocalDateTime.of(date, seg.getEndTime());
                                    leaveType = leaveRequests.stream()
                                            .filter(lr -> lr.getUserID() == shift.getUserID())
                                            .filter(lr -> !lr.getFromDate().isAfter(subStart)
                                                    && !lr.getToDate().isBefore(subEnd))
                                            .map(LeaveRequest::getLeaveType)
                                            .findFirst()
                                            .orElse("On Leave");
                                }
                                String totalHours = calculateShiftDuration(
                                        seg.getStartTime(),
                                        seg.getEndTime(),
                                        shift.getThirtyMinBreaks(),
                                        shift.getTenMinBreaks()
                                );
                                export.append(String.format(
                                        "%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                        date.format(DATE_FORMATTER),
                                        user.getUserID(),
                                        user.getFirst_name(),
                                        user.getLast_name(),
                                        seg.getStartTime().format(TIME_FORMATTER),
                                        seg.getEndTime().format(TIME_FORMATTER),
                                        totalHours,
                                        shift.getThirtyMinBreaks(),
                                        shift.getTenMinBreaks(),
                                        isPublicHoliday ? "Yes" : "No",
                                        leaveType
                                ));
                            }
                        }
                    }

                    // Add modifications that create new shifts on this date
                    for (Shift mod : modifications) {
                        if (mod.getShiftStartDate() != null &&
                                mod.getShiftStartDate().equals(currentDate) &&
                                !mod.getShiftStartDate().equals(mod.getOriginalDate())) {

                            User user = userMap.get(mod.getUserID());
                            if (user == null) continue;

                            List<ShiftSegment> segments = buildShiftSegments(mod, date, leaveRequests);

                            for (ShiftSegment seg : segments) {
                                if (seg.getStartTime().equals(seg.getEndTime())) {
                                    continue; // skip zero-length
                                }
                                // If on leave => pick a leaveType or "On Leave"
                                String leaveType = "";
                                if (seg.isOnLeave()) {
                                    LocalDateTime subStart = LocalDateTime.of(date, seg.getStartTime());
                                    LocalDateTime subEnd   = LocalDateTime.of(date, seg.getEndTime());
                                    leaveType = leaveRequests.stream()
                                            .filter(lr -> lr.getUserID() == mod.getUserID())
                                            .filter(lr -> !lr.getFromDate().isAfter(subStart)
                                                    && !lr.getToDate().isBefore(subEnd))
                                            .map(LeaveRequest::getLeaveType)
                                            .findFirst()
                                            .orElse("On Leave");
                                }

                                String totalHours = calculateShiftDuration(
                                        seg.getStartTime(),
                                        seg.getEndTime(),
                                        mod.getThirtyMinBreaks(),
                                        mod.getTenMinBreaks()
                                );

                                export.append(String.format(
                                        "%s\t%d\t%s %s\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                                        date.format(DATE_FORMATTER),
                                        user.getUserID(),
                                        user.getFirst_name(),
                                        user.getLast_name(),
                                        seg.getStartTime().format(TIME_FORMATTER),
                                        seg.getEndTime().format(TIME_FORMATTER),
                                        totalHours,
                                        mod.getThirtyMinBreaks(),
                                        mod.getTenMinBreaks(),
                                        isPublicHoliday ? "Yes" : "No",
                                        leaveType
                                ));
                            }
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
            progressBarExcel.setVisible(false);
        });

        exportTask.setOnFailed(e -> {
            Throwable exception = exportTask.getException();
            String errorMessage = exception.getMessage();
            if (errorMessage.equals("No shifts found for the selected month.")) {
                parent.getDialogPane().showInformation("No Data", errorMessage);
            } else {
                parent.getDialogPane().showError("Export Failed", "Failed to export data", exception);
            }
            progressBarExcel.setVisible(false);
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

    private List<ShiftSegment> buildShiftSegments(Shift shift,
                                                  LocalDate shiftDay,
                                                  List<LeaveRequest> allLeaveRequests) {

        LocalTime shiftStart = shift.getShiftStartTime();
        LocalTime shiftEnd   = shift.getShiftEndTime();

        if (shiftStart == null || shiftEnd == null) {
            return new ArrayList<>();
        }
        if (!shiftStart.isBefore(shiftEnd)) {
            // If start >= end, treat as zero-length shift
            return new ArrayList<>();
        }

        // 1) Find relevant leaves for this user that intersect [shiftDay@shiftStart, shiftDay@shiftEnd]
        LocalDateTime dayStart = LocalDateTime.of(shiftDay, shiftStart);
        LocalDateTime dayEnd   = LocalDateTime.of(shiftDay, shiftEnd);
        List<LeaveRequest> relevantLeaves = new ArrayList<>();

        for (LeaveRequest lr : allLeaveRequests) {
            if (lr.getUserID() == shift.getUserID()) {
                // Overlap if [lr.fromDate, lr.toDate] intersects [dayStart, dayEnd]
                if (lr.getToDate().isAfter(dayStart) && lr.getFromDate().isBefore(dayEnd)) {
                    relevantLeaves.add(lr);
                }
            }
        }

        // If no relevant leave, it's one continuous sub-shift
        if (relevantLeaves.isEmpty()) {
            return List.of(new ShiftSegment(shiftStart, shiftEnd, false));
        }

        // 2) Build boundary times for sub-shift splits
        List<LocalTime> boundaries = new ArrayList<>();
        boundaries.add(shiftStart);
        boundaries.add(shiftEnd);

        for (LeaveRequest lr : relevantLeaves) {
            LocalDateTime from = lr.getFromDate();
            LocalDateTime to   = lr.getToDate();

            // Clamp the leave interval to [dayStart, dayEnd]
            LocalDateTime clampStart = from.isBefore(dayStart) ? dayStart : from;
            LocalDateTime clampEnd   = to.isAfter(dayEnd) ? dayEnd : to;

            LocalTime leaveStart = clampStart.toLocalTime();
            LocalTime leaveEnd   = clampEnd.toLocalTime();

            // Add to boundaries if they are within [shiftStart, shiftEnd]
            if (!leaveStart.isBefore(shiftStart) && !leaveStart.isAfter(shiftEnd)) {
                boundaries.add(leaveStart);
            }
            if (!leaveEnd.isBefore(shiftStart) && !leaveEnd.isAfter(shiftEnd)) {
                boundaries.add(leaveEnd);
            }
        }

        // Sort & remove duplicates
        boundaries = boundaries.stream().distinct().sorted().toList();

        // 3) Build sub-segments from consecutive boundary pairs
        List<ShiftSegment> segments = new ArrayList<>();
        for (int i = 0; i < boundaries.size() - 1; i++) {
            LocalTime segStart = boundaries.get(i);
            LocalTime segEnd   = boundaries.get(i + 1);

            if (!segStart.isBefore(segEnd)) {
                continue; // skip zero-length or reversed intervals
            }

            // Check if sub-interval is covered by a leave
            boolean onLeave = false;
            LocalDateTime subStart = LocalDateTime.of(shiftDay, segStart);
            LocalDateTime subEnd   = LocalDateTime.of(shiftDay, segEnd);

            for (LeaveRequest lr : relevantLeaves) {
                // If this sub-block is fully inside lr => onLeave
                if (!lr.getFromDate().isAfter(subStart) && !lr.getToDate().isBefore(subEnd)) {
                    onLeave = true;
                    break;
                }
            }

            segments.add(new ShiftSegment(segStart, segEnd, onLeave));
        }

        return segments;
    }

    @FXML
    public void generatePDF() {
        LocalDate dateWithinDisplayedWeek = main.getCurrentDate();
        fontBold    = PDType1Font.HELVETICA_BOLD;
        fontRegular = PDType1Font.HELVETICA;
        fontItalic  = PDType1Font.HELVETICA_OBLIQUE;

        // compute fortnight
        LocalDate weekStart = dateWithinDisplayedWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate epoch    = LocalDate.of(2000,1,3);
        long idx = ChronoUnit.DAYS.between(epoch,weekStart) / 14;
        overallFortnightStartDate = epoch.plusDays(idx*14);
        overallFortnightEndDate   = overallFortnightStartDate.plusDays(13);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Fortnight Roster PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files","*.pdf"));
        chooser.setInitialFileName(
                "Roster_"+
                        overallFortnightStartDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))+"_"+
                        overallFortnightEndDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))+".pdf"
        );
        File file = chooser.showSaveDialog(main.getStg());
        if (file==null) return;
        progressBarPDF.setVisible(true);

        Task<Void> pdfTask = new Task<>(){
            @Override protected Void call() throws Exception {
                // fetch data
                List<User> allEmps = userService.getAllUserEmployments(main.getCurrentStore().getStoreID());
                Map<String,List<User>> empsByRole = allEmps.stream()
                        .filter(u->u.getRole()!=null && !u.getRole().isEmpty())
                        .collect(Collectors.groupingBy(u->u.getRole().toUpperCase()));
                List<String> order = List.of("PHARMACIST","PHARMACY ASSISTANTS","OCCASIONAL STAFF");
                List<String> roles = new ArrayList<>();
                for(String r:order) if(empsByRole.containsKey(r)) roles.add(r);
                empsByRole.keySet().stream().filter(r->!order.contains(r)).sorted().forEach(roles::add);

                List<Shift> shifts = rosterService.getShifts(main.getCurrentStore().getStoreID(), overallFortnightStartDate, overallFortnightEndDate);
                List<Shift> mods   = rosterService.getShiftModifications(main.getCurrentStore().getStoreID(), overallFortnightStartDate, overallFortnightEndDate);
                List<LeaveRequest> leaves = leaveService.getLeaveRequests(main.getCurrentStore().getStoreID(), overallFortnightStartDate, overallFortnightEndDate);
                Map<String,Shift> modMap = new HashMap<>();
                for(Shift m:mods) if(m.getOriginalDate()!=null)
                    modMap.put(m.getShiftID()+"_"+m.getOriginalDate(),m);

                try(PDDocument doc=new PDDocument()){
                    PDRectangle ps = PDRectangle.A4;
                    AtomicReference<PDPage> pageRef = new AtomicReference<>(new PDPage(ps));
                    doc.addPage(pageRef.get());
                    AtomicReference<PDPageContentStream> csRef =
                            new AtomicReference<>(new PDPageContentStream(doc,pageRef.get()));
                    AtomicReference<Float> yRef = new AtomicReference<>(ps.getHeight()-MARGIN-Y_START_PAGE_OFFSET);
                    int[] pageNum = {1,0};

                    // week1
                    LocalDate w1 = overallFortnightStartDate;
                    String week1Title = String.format(
                            "ROSTER – WEEK 1 – %s to %s",
                            w1.format(DateTimeFormatter.ofPattern("dd/MM")),
                            w1.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM"))
                    );

                    // draw the week title:
                    csRef.get().setFont(fontBold, WEEK_TITLE_FONT_SIZE);
                    float titleWidth = fontBold.getStringWidth(week1Title) / 1000 * WEEK_TITLE_FONT_SIZE;
                    float pageWidth  = ps.getWidth();
                    csRef.get().beginText();
                    csRef.get().newLineAtOffset((pageWidth - titleWidth)/2, yRef.get());
                    csRef.get().showText(week1Title);
                    csRef.get().endText();
                    yRef.set(yRef.get() - WEEK_TITLE_HEIGHT);
                    yRef.set(yRef.get() - INTER_TABLE_SPACING/2);
                    yRef.set(drawWeekTable(doc,pageRef,csRef,ps,roles,empsByRole,shifts,mods,modMap,leaves,w1,yRef,pageNum));

                    yRef.set(yRef.get() - INTER_TABLE_SPACING*8);

                    // week2
                    LocalDate w2 = overallFortnightStartDate.plusDays(7);
                    String week2Title = String.format(
                            "ROSTER – WEEK 2 – %s to %s",
                            w2.format(DateTimeFormatter.ofPattern("dd/MM")),
                            w2.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM"))
                    );

                    // draw the week title:
                    csRef.get().setFont(fontBold, WEEK_TITLE_FONT_SIZE);
                    titleWidth = fontBold.getStringWidth(week2Title) / 1000 * WEEK_TITLE_FONT_SIZE;
                    pageWidth  = ps.getWidth();
                    csRef.get().beginText();
                    csRef.get().newLineAtOffset((pageWidth - titleWidth)/2, yRef.get());
                    csRef.get().showText(week2Title);
                    csRef.get().endText();
                    yRef.set(yRef.get() - WEEK_TITLE_HEIGHT);

                    yRef.set(yRef.get() - INTER_TABLE_SPACING/4);
                    yRef.set(drawWeekTable(doc,pageRef,csRef,ps,roles,empsByRole,shifts,mods,modMap,leaves,w2,yRef,pageNum));

                    csRef.get().close();
                    doc.save(file);
                }
                return null;
            }
        };
        pdfTask.setOnSucceeded(e->{ progressBarPDF.setVisible(false); parent.getDialogPane().showInformation("Success","Exported to " + file.getAbsolutePath()); });
        pdfTask.setOnFailed(e->{ progressBarPDF.setVisible(false); parent.getDialogPane().showError("Error","PDF export failed",pdfTask.getException()); });
        executor.submit(pdfTask);
    }

    private float drawWeekTable(
            PDDocument doc,
            AtomicReference<PDPage> pageRef,
            AtomicReference<PDPageContentStream> csRef,
            PDRectangle ps,
            List<String> roles,
            Map<String,List<User>> empsByRole,
            List<Shift> allShifts,
            List<Shift> allMods,
            Map<String,Shift> modMap,
            List<LeaveRequest> allLeaves,
            LocalDate weekStart,
            AtomicReference<Float> yRef,
            int[] pageNum
    ) throws IOException {
        float y = yRef.get();
        float colNameW = 120;
        float colDateW = (ps.getWidth() - 2 * MARGIN - colNameW) / 7;
        List<LocalDate> dates = weekStart.datesUntil(weekStart.plusDays(7)).collect(Collectors.toList());

        // draw day-headers once
        if (y < BOTTOM_PAGE_MARGIN_THRESHOLD + DAY_HEADER_ROW_HEIGHT + roles.size() * EMPLOYEE_ROW_HEIGHT) {
            // new page (no headers)
            y = startNewPageAndContextHeaders(
                    doc,
                    pageRef,
                    csRef,
                    ps,
                    pageNum,
                    yRef,       // <-- pass AtomicReference<Float> instead of primitive y/null
                    null,       // no main title
                    null,       // no role title
                    dates,
                    false,      // don't draw main title
                    false       // don't draw week title only
            );
            y = yRef.get();  // sync local y with ref
        }
        drawDayHeaders(csRef.get(), dates, colNameW, colDateW, y);
        y -= DAY_HEADER_ROW_HEIGHT;
        float tableTop = y;

        for (String role : roles) {
            List<User> list = empsByRole.getOrDefault(role, Collections.emptyList());
            // filter those with any shift
            List<User> withShifts = list.stream().filter(u ->
                    dates.stream().anyMatch(d -> findEffectiveShiftForDate(u, d, allShifts, allMods, modMap) != null)
            ).toList();
            if (withShifts.isEmpty()) continue;

            // divider row
            if (y < BOTTOM_PAGE_MARGIN_THRESHOLD + ROLE_HEADER_HEIGHT + withShifts.size() * EMPLOYEE_ROW_HEIGHT) {
                // new page (redraw week title)
                y = startNewPageAndContextHeaders(
                        doc,
                        pageRef,
                        csRef,
                        ps,
                        pageNum,
                        yRef,          // <-- pass AtomicReference<Float>
                        null,          // no main title (already drawn)
                        null,          // no role title
                        dates,
                        false,         // don't draw main title
                        true           // draw week title only
                );
                y = yRef.get();  // sync local y with ref
                drawDayHeaders(csRef.get(), dates, colNameW, colDateW, y);
                y -= DAY_HEADER_ROW_HEIGHT;
            }
            // — divider row: show role as one big merged cell, centered
            csRef.get().setFont(fontBold, ROLE_DIVIDER_FONT_SIZE);
            float titleWidth = fontBold.getStringWidth(role) / 1000 * ROLE_DIVIDER_FONT_SIZE;
            float tableWidth = ps.getWidth() - 2 * MARGIN;
            float centeredX = MARGIN + (tableWidth - titleWidth) / 2;
            csRef.get().beginText();
            csRef.get().newLineAtOffset(centeredX, y - ROLE_DIVIDER_FONT_SIZE);
            csRef.get().showText(role);
            csRef.get().endText();
            y -= ROLE_HEADER_HEIGHT;   // leave the full height for the “merged” cell

            // underline divider
            csRef.get().setLineWidth(0.5f);
            csRef.get().moveTo(MARGIN, y);
            csRef.get().lineTo(ps.getWidth() - MARGIN, y);
            csRef.get().stroke();
            y -= CELL_PADDING;

            float groupTop = y;
            float dividerTopY    = groupTop + ROLE_HEADER_HEIGHT + CELL_PADDING;
            float dividerBottomY = groupTop;

            // draw the horizontal underline (you already have this)
            csRef.get().setLineWidth(0.5f);
            csRef.get().moveTo(MARGIN, groupTop + CELL_PADDING + ROLE_HEADER_HEIGHT);
            csRef.get().lineTo(ps.getWidth() - MARGIN, groupTop + CELL_PADDING + ROLE_HEADER_HEIGHT);
            csRef.get().stroke();

            // now the left/right caps:
            csRef.get().setLineWidth(0.25f);
            csRef.get().moveTo(MARGIN, dividerTopY);
            csRef.get().lineTo(MARGIN, dividerBottomY);
            csRef.get().moveTo(ps.getWidth() - MARGIN, dividerTopY);
            csRef.get().lineTo(ps.getWidth() - MARGIN, dividerBottomY);
            csRef.get().stroke();

            // employee rows
            for (User u : withShifts) {
                if (y < BOTTOM_PAGE_MARGIN_THRESHOLD + EMPLOYEE_ROW_HEIGHT) {
                    // finish verticals + horizontal bottom
                    drawVerticalTableLines(csRef.get(), tableTop, y + EMPLOYEE_ROW_HEIGHT, colNameW, colDateW, dates.size());
                    y = startNewPageAndContextHeaders(doc, pageRef, csRef, ps, pageNum, yRef, null, null, dates, false, false);
                    drawDayHeaders(csRef.get(), dates, colNameW, colDateW, y);
                    y -= DAY_HEADER_ROW_HEIGHT;
                    tableTop = y;
                }
                float rowTop = y;
                float yPosName = rowTop - (EMPLOYEE_ROW_HEIGHT + EMPLOYEE_NAME_FONT_SIZE) / 2;
                // name & role
                csRef.get().beginText();
                csRef.get().setFont(fontRegular, EMPLOYEE_NAME_FONT_SIZE);
                csRef.get().newLineAtOffset(MARGIN + CELL_PADDING, yPosName);
                csRef.get().showText(u.getNickname());
                csRef.get().endText();
                float x = MARGIN + colNameW;
                for (LocalDate d : dates) {
                    String text = "-";
                    boolean leave = allLeaves.stream().anyMatch(l -> l.getUserID() == u.getUserID()
                            && !d.isBefore(l.getFromDate().toLocalDate()) && !d.isAfter(l.getToDate().toLocalDate()));
                    if (leave) text = "LEAVE";
                    else {
                        List<Shift> shiftsForDay = findAllShiftsForDate(u, d, allShifts, allMods, modMap);

                        // build simple TimeRange list
                        List<TimeRange> ranges = shiftsForDay.stream()
                                .map(sft -> new TimeRange(sft.getShiftStartTime(), sft.getShiftEndTime()))
                                .collect(Collectors.toList());

                        // merge any that butt‐up
                        List<TimeRange> merged = mergeContiguous(ranges);

                        // format each into a line
                        List<String> lines = merged.stream()
                                .map(r -> formatTime(r.start) + " - " + formatTime(r.end))
                                .collect(Collectors.toList());

                        // join with newline (or comma, if you prefer)
                        text = String.join("\n", lines);
                    }
                    drawTextInCell(csRef.get(), text, leave ? fontItalic : fontRegular, SHIFT_TIME_FONT_SIZE,
                            x, rowTop - EMPLOYEE_ROW_HEIGHT, colDateW, EMPLOYEE_ROW_HEIGHT, true);
                    x += colDateW;
                }
                // bottom border
                csRef.get().setLineWidth(0.25f);
                csRef.get().moveTo(MARGIN, rowTop - EMPLOYEE_ROW_HEIGHT);
                csRef.get().lineTo(MARGIN + colNameW + colDateW * dates.size(), rowTop - EMPLOYEE_ROW_HEIGHT);
                csRef.get().stroke();
                y -= EMPLOYEE_ROW_HEIGHT;

                float groupBottom = y;
                drawVerticalTableLines(csRef.get(), groupTop, groupBottom, colNameW, colDateW, dates.size());
            }
        }
        return y;
    }

    private List<Shift> findAllShiftsForDate(User emp, LocalDate date,
                                             List<Shift> allShifts,
                                             List<Shift> allMods,
                                             Map<String,Shift> modMap) {
        List<Shift> result = new ArrayList<>();

        // 1) explicit modifications that start on this date
        for (Shift mod : allMods) {
            if (mod.getUserID()==emp.getUserID()
                    && mod.getShiftStartDate()!=null
                    && mod.getShiftStartDate().equals(date)) {
                result.add(mod);
            }
        }

        // 2) original or repeating instances that weren’t moved off this date
        for (Shift s : allShifts) {
            if (s.getUserID()!=emp.getUserID()) continue;
            boolean onDate = (!s.isRepeating() && s.getShiftStartDate().equals(date))
                    || (s.isRepeating()
                    && !s.getShiftStartDate().isAfter(date)
                    && (s.getShiftEndDate()==null || !s.getShiftEndDate().isBefore(date))
                    && DAYS.between(s.getShiftStartDate(), date) % s.getDaysPerRepeat() == 0
            );
            if (!onDate) continue;

            // if there’s a “move‐away” mod for this instance, skip it:
            String key = s.getShiftID() + "_" + date.toString();
            Shift move = modMap.get(key);
            if (move!=null && (move.getShiftStartDate()==null || !move.getShiftStartDate().equals(date))) {
                continue;
            }
            result.add(s);
        }

        // sort by start‐time:
        result.sort(Comparator.comparing(Shift::getShiftStartTime));
        return result;
    }

    // Corrected Shift Logic
    private Shift findEffectiveShiftForDate(User employee, LocalDate date,
                                            List<Shift> allShifts, List<Shift> allModifications,
                                            Map<String, Shift> modificationMap) {
        // Priority 1: Check modifications that ARE on this 'date'
        for (Shift mod : allModifications) {
            if (mod.getUserID() == employee.getUserID() && mod.getShiftStartDate() != null && mod.getShiftStartDate().equals(date)) {
                return mod; // This modification is the definitive shift for this date
            }
        }

        // Priority 2: Check original/repeating shifts, ensure they weren't modified to be OFF this 'date'
        for (Shift s : allShifts) {
            if (s.getUserID() == employee.getUserID()) {
                boolean isRepeatingInstanceOnDate = s.isRepeating() &&
                        !s.getShiftStartDate().isAfter(date) &&
                        (s.getShiftEndDate() == null || !s.getShiftEndDate().isBefore(date)) &&
                        ChronoUnit.DAYS.between(s.getShiftStartDate(), date) >= 0 &&
                        ChronoUnit.DAYS.between(s.getShiftStartDate(), date) % s.getDaysPerRepeat() == 0;
                boolean isNonRepeatingOnDate = !s.isRepeating() && s.getShiftStartDate().equals(date);

                if (isRepeatingInstanceOnDate || isNonRepeatingOnDate) {
                    // This shift 's' has a theoretical instance on 'date'.
                    // Check if this specific instance was modified (key: shiftID + this specific 'date').
                    Shift potentialModification = modificationMap.get(s.getShiftID() + "_" + date.toString());

                    if (potentialModification != null) {
                        // A modification exists for this instance.
                        // If potentialModification.getShiftStartDate() is also this 'date',
                        // it means the shift was altered but stayed on this day. The first loop (above) would have already returned it.
                        // If potentialModification.getShiftStartDate() is *not* this 'date' (or null),
                        // it means this instance was moved away or deleted. So, 's' is not the shift.
                        // Therefore, if a modification exists, the first loop is authoritative. We can skip 's'.
                        continue;
                    } else {
                        // No modification for this instance of 's' on 'date'. So, 's' is the one.
                        return s;
                    }
                }
            }
        }
        return null;
    }


    private void drawDayHeaders(PDPageContentStream cs, List<LocalDate> datesInWeek,
                                float empColW, float dateColW, float headerTopY) throws IOException {
        float currentX = MARGIN;
        // Employee column header text
        drawTextInCell(cs, "Employee", fontBold, DAY_HEADER_FONT_SIZE, currentX, headerTopY - DAY_HEADER_ROW_HEIGHT, empColW, DAY_HEADER_ROW_HEIGHT, false);
        currentX += empColW;

        DateTimeFormatter dateFmtHeader = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.ENGLISH);
        for (LocalDate date : datesInWeek) {
            drawTextInCell(cs, date.format(dateFmtHeader), fontBold, DAY_HEADER_FONT_SIZE,
                    currentX, headerTopY - DAY_HEADER_ROW_HEIGHT, dateColW, DAY_HEADER_ROW_HEIGHT, true);
            currentX += dateColW;
        }
        cs.setLineWidth(0.5f); // Line below day headers
        cs.moveTo(MARGIN, headerTopY - DAY_HEADER_ROW_HEIGHT);
        cs.lineTo(MARGIN + empColW + (dateColW * datesInWeek.size()), headerTopY - DAY_HEADER_ROW_HEIGHT);
        cs.stroke();
    }

    private float startNewPageAndContextHeaders(PDDocument doc, AtomicReference<PDPage> crP, AtomicReference<PDPageContentStream> csR,
                                                PDRectangle pS, int[] pNI, AtomicReference<Float> cY,
                                                String weekTitleStr, String roleNameStr, List<LocalDate> datesInWeek, // Nullable if not needed
                                                boolean drawMainTitle, boolean drawWeekTitleOnly) throws IOException {

        csR.get().close();
        pNI[0]++; // Increment page number

        PDPage newPage = new PDPage(pS);
        doc.addPage(newPage);
        crP.set(newPage);
        csR.set(new PDPageContentStream(doc, newPage));
        float currentY = pS.getHeight() - MARGIN - Y_START_PAGE_OFFSET;

        if (drawMainTitle) {
            currentY = new AtomicReference<>(currentY).get() - (MAIN_TITLE_FONT_SIZE + (MAIN_TITLE_FONT_SIZE-2) + 15); // Adjust based on drawMainPdfTitle's consumption
        }

        if (drawWeekTitleOnly && weekTitleStr != null){
            currentY = new AtomicReference<>(currentY).get() - (WEEK_TITLE_HEIGHT + 5);
            cY.set(currentY);
            return currentY; // Only week title was needed
        }


        // Redraw context for a continued table (Week Title, Role Header, Day Headers)
        if (weekTitleStr != null) {
            currentY = new AtomicReference<>(currentY).get() - (WEEK_TITLE_HEIGHT + 5);
        }
        if (roleNameStr != null) {
            currentY = new AtomicReference<>(currentY).get() - (ROLE_HEADER_HEIGHT + 3);
        }
        if (datesInWeek != null) {
            float employeeColWidth = 120; // Must match usage in drawTableForRoleGroup
            float dateColWidth = (pS.getWidth() - (2 * MARGIN) - employeeColWidth) / 7;
            drawDayHeaders(csR.get(), datesInWeek, employeeColWidth, dateColWidth, currentY);
            currentY -= DAY_HEADER_ROW_HEIGHT;
        }
        cY.set(currentY);
        return currentY;
    }

    private void drawTextInCell(PDPageContentStream stream,
                                String text,
                                PDType1Font font,
                                float fontSize,
                                float cellX,
                                float cellBottomY,
                                float cellWidth,
                                float cellHeight,
                                boolean centered) throws IOException {
        if (text == null) text = "";

        // split into lines immediately
        String[] parts = text.split("\n");
        float leading = fontSize + 2f;
        float blockH  = leading * parts.length;
        float startY  = cellBottomY + (cellHeight - blockH)/2 + (parts.length-1)*leading;

        stream.setFont(font, fontSize);

        for (int i = 0; i < parts.length; i++) {
            // truncate *this* single line only
            String line = truncateText(parts[i], cellWidth - 2 * CELL_PADDING, font, fontSize);
            float w     = font.getStringWidth(line) / 1000 * fontSize;

            float xPos = cellX + (centered
                    ? (cellWidth - w) / 2
                    : CELL_PADDING);

            // don’t let it overflow
            if (xPos < cellX + CELL_PADDING) xPos = cellX + CELL_PADDING;
            if (xPos + w > cellX + cellWidth - CELL_PADDING) {
                xPos = cellX + cellWidth - CELL_PADDING - w;
            }

            float yPos = startY - i * leading;

            stream.beginText();
            stream.newLineAtOffset(xPos, yPos);
            stream.showText(line);
            stream.endText();
        }
    }

    private String truncateText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
        if (text == null) return "";
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        if (textWidth <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        float ellipsisWidth = font.getStringWidth(ellipsis) / 1000 * fontSize;
        if (maxWidth < ellipsisWidth) return ""; // Not enough space even for ellipsis

        StringBuilder sb = new StringBuilder();
        float currentWidth = 0;
        for (char c : text.toCharArray()) {
            float charWidth = font.getStringWidth(String.valueOf(c)) / 1000 * fontSize;
            if (currentWidth + charWidth + ellipsisWidth <= maxWidth) {
                sb.append(c);
                currentWidth += charWidth;
            } else {
                break;
            }
        }
        return sb.toString() + ellipsis;
    }

    private void drawVerticalTableLines(PDPageContentStream cs, float tableContentTopY, float tableContentBottomY,
                                        float empColW, float dateColW, int numDateCols) throws IOException {
        cs.setLineWidth(0.25f); // Thinner for internal grid lines
        float currentX = MARGIN;

        float headerSectionHeight = DAY_HEADER_ROW_HEIGHT; // Height of the day headers section
        float tableTopActualContent = tableContentTopY;

        // Leftmost line
        cs.moveTo(currentX, tableTopActualContent);
        cs.lineTo(currentX, tableContentBottomY);
        cs.stroke();

        // After Employee Column
        currentX += empColW;
        cs.moveTo(currentX, tableTopActualContent);
        cs.lineTo(currentX, tableContentBottomY);
        cs.stroke();

        // After each Date Column
        for (int i = 0; i < numDateCols; i++) {
            currentX += dateColW;
            cs.moveTo(currentX, tableTopActualContent);
            cs.lineTo(currentX, tableContentBottomY);
            cs.stroke();
        }
    }

    private String formatTime(LocalTime t) {
        String raw = (t.getMinute() == 0)
                ? t.format(HOUR_ONLY_FMT)
                : t.format(HOUR_MIN_FMT);
        return raw.toLowerCase();  // e.g. "9am" or "3:30pm"
    }

    private List<TimeRange> mergeContiguous(List<TimeRange> in) {
        List<TimeRange> out = new ArrayList<>();
        if (in.isEmpty()) return out;
        // assume `in` is sorted by start
        TimeRange curr = in.get(0);
        for (int i = 1; i < in.size(); i++) {
            TimeRange next = in.get(i);
            if (curr.end.equals(next.start)) {
                // extend
                curr = new TimeRange(curr.start, next.end);
            } else {
                out.add(curr);
                curr = next;
            }
        }
        out.add(curr);
        return out;
    }

    private static class TimeRange {
        LocalTime start, end;
        TimeRange(LocalTime s, LocalTime e) { start = s; end = e; }
    }
}