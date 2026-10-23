package models;

/** Editable scan frequency, stored by Alpha API as a whole number of minutes. */
public record ScanInterval(int value, Unit unit) {
    public static final int MIN_MINUTES = 5;
    public static final int MAX_MINUTES = 365 * 24 * 60;
    public static final String VALIDATION_MESSAGE =
            "Enter a whole number of minutes, hours, or days, between 5 minutes and 365 days.";

    public enum Unit {
        MINUTES("minutes", 1), HOURS("hours", 60), DAYS("days", 1440);

        private final String label;
        private final int minutes;

        Unit(String label, int minutes) {
            this.label = label;
            this.minutes = minutes;
        }

        @Override public String toString() { return label; }
    }

    public ScanInterval {
        long minutes = unit == null ? 0 : (long) value * unit.minutes;
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new IllegalArgumentException(VALIDATION_MESSAGE);
        }
    }

    public int minutes() { return value * unit.minutes; }

    public static ScanInterval parse(String value, Unit unit) {
        try {
            return new ScanInterval(Integer.parseInt(value == null ? "" : value.trim()), unit);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(VALIDATION_MESSAGE);
        }
    }

    public static ScanInterval fromMinutes(int minutes) {
        Unit unit = minutes % Unit.DAYS.minutes == 0 ? Unit.DAYS
                : minutes % Unit.HOURS.minutes == 0 ? Unit.HOURS : Unit.MINUTES;
        return new ScanInterval(minutes / unit.minutes, unit);
    }
}
