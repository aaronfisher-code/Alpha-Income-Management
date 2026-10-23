package models;

import org.junit.jupiter.api.Test;

import static models.ScanInterval.Unit.*;
import static org.junit.jupiter.api.Assertions.*;

class ScanIntervalTest {
    @Test
    void customHoursAndDaysConvertToTheApiMinuteInterval() {
        assertEquals(120, ScanInterval.parse("2", HOURS).minutes());
        assertEquals(4320, ScanInterval.parse("3", DAYS).minutes());
        assertEquals(43200, ScanInterval.parse(" 30 ", DAYS).minutes());
        assertEquals(525600, ScanInterval.parse("365", DAYS).minutes());
        assertEquals(5, ScanInterval.parse("5", MINUTES).minutes());
    }

    @Test
    void existingAndCustomSchedulesRoundTripWithoutChangingTheirFrequency() {
        for (int minutes : new int[] {5, 15, 30, 45, 60, 90, 120, 180, 360, 720, 1440, 1500, 4320, 10080, 43200, 525600}) {
            ScanInterval displayed = ScanInterval.fromMinutes(minutes);
            assertEquals(minutes, ScanInterval.parse(Integer.toString(displayed.value()), displayed.unit()).minutes());
        }
        assertEquals(new ScanInterval(2, HOURS), ScanInterval.fromMinutes(120));
        assertEquals(new ScanInterval(3, DAYS), ScanInterval.fromMinutes(4320));
        assertEquals(new ScanInterval(90, MINUTES), ScanInterval.fromMinutes(90));
    }

    @Test
    void invalidValuesAndOverflowAreRejectedBeforeSaving() {
        for (String value : new String[] {null, "", " ", "zero", "1.5", "0", "-1", "2147483648"}) {
            assertThrows(IllegalArgumentException.class, () -> ScanInterval.parse(value, HOURS));
        }
        assertThrows(IllegalArgumentException.class, () -> ScanInterval.parse("4", MINUTES));
        assertThrows(IllegalArgumentException.class, () -> ScanInterval.parse("366", DAYS));
        assertThrows(IllegalArgumentException.class, () -> ScanInterval.parse("2147483647", DAYS));
        assertThrows(IllegalArgumentException.class, () -> ScanInterval.parse("1", null));
    }
}
