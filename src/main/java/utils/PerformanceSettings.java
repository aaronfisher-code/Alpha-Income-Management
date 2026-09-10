package utils;

import java.util.Locale;

/** Runtime rendering policy. Override with -Dalpha.performance.mode=quality|balanced|low-power|off. */
public final class PerformanceSettings {
    public enum Mode {
        QUALITY,
        BALANCED,
        LOW_POWER,
        OFF
    }

    private static final Mode MODE = readMode();

    private PerformanceSettings() {
    }

    public static Mode mode() {
        return MODE;
    }

    public static boolean animationsEnabled() {
        return MODE != Mode.OFF;
    }

    public static boolean layoutAnimationsEnabled() {
        return MODE == Mode.QUALITY;
    }

    public static double animationDuration(double requestedMillis) {
        return switch (MODE) {
            case QUALITY -> requestedMillis;
            case BALANCED -> Math.min(requestedMillis, 120.0);
            case LOW_POWER -> Math.min(requestedMillis, 80.0);
            case OFF -> 0.0;
        };
    }

    private static Mode readMode() {
        String value = System.getProperty("alpha.performance.mode", "balanced")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (value) {
            case "quality" -> Mode.QUALITY;
            case "low-power", "low_power", "lowpower" -> Mode.LOW_POWER;
            case "off", "none", "disabled" -> Mode.OFF;
            case "balanced" -> Mode.BALANCED;
            default -> {
                System.err.println("Unknown alpha.performance.mode '" + value + "'; using balanced.");
                yield Mode.BALANCED;
            }
        };
    }
}
