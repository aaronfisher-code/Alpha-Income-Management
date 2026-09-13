package services;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveZConfigurationTest {
    @Test
    void defaultsToImportedMode() {
        assertFalse(LiveZConfiguration.from(new Properties()).enabled());
    }

    @Test
    void enablesAllLiveZScreensFromOneProperty() {
        var properties = new Properties();
        properties.setProperty("z.live.enabled", "true");
        assertTrue(LiveZConfiguration.from(properties).enabled());
    }
}
