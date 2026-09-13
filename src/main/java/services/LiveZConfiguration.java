package services;

import java.io.IOException;
import java.util.Properties;

/** Shared client-side feature configuration for screens backed by live Z data. */
public record LiveZConfiguration(boolean enabled) {
    public static LiveZConfiguration load() throws IOException {
        return from(loadProperties());
    }

    public static LiveZConfiguration from(Properties properties) {
        return new LiveZConfiguration(Boolean.parseBoolean(
                properties.getProperty("z.live.enabled", "false")));
    }

    static Properties loadProperties() throws IOException {
        var properties = new Properties();
        try (var input = LiveZConfiguration.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) throw new IOException("application.properties was not found");
            properties.load(input);
        }
        return properties;
    }
}
