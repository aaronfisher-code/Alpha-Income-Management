package services;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Bounded requests, including saves whose response may be lost after the API commits them. */
final class ApiHttpClient {
    private ApiHttpClient() {}

    static RestTemplate create(Properties properties) throws IOException {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(timeout(properties, "api.connect.timeout.seconds", 10));
        requests.setReadTimeout(timeout(properties, "api.read.timeout.seconds", 60));
        return new RestTemplate(requests);
    }

    private static Duration timeout(Properties properties, String key, int defaultSeconds) throws IOException {
        String configured = properties.getProperty(key, Integer.toString(defaultSeconds)).trim();
        try {
            int seconds = Integer.parseInt(configured);
            if (seconds < 1) throw new NumberFormatException("must be positive");
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException exception) {
            throw new IOException(key + " must be a positive whole number", exception);
        }
    }
}
