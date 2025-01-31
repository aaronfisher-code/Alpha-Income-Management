package models;

import java.time.LocalTime;

public class ShiftSegment {
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final boolean onLeave;

    public ShiftSegment(LocalTime startTime, LocalTime endTime, boolean onLeave) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.onLeave = onLeave;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime()   { return endTime; }
    public boolean isOnLeave()      { return onLeave; }
}
