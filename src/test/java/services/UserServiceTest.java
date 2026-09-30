package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.PathContainer;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"admin", "lany.rainbow@gmail.com", "lany+rainbow@gmail.com", "lany%40gmail.com", "lany rainbow", "lany/rainbow"})
    void loginSendsTheOriginalUsernameAndPassword(String username) {
        var requests = new ArrayList<Request>();
        UserService service = service(requests);

        User result = service.verifyPassword(username, "test-password");

        Request request = onlyRequest(requests, HttpMethod.POST);
        assertEquals(List.of("users", username, "verify-password"), pathSegments(request.uri()));
        assertEquals("test-password", new String(request.body(), StandardCharsets.UTF_8));
        assertEquals("test-user", result.getUsername());
        assertEquals("test-session", UserService.getDocumentAiSession());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "lany.rainbow@gmail.com", "lany+rainbow@gmail.com", "lany%40gmail.com", "lany rainbow", "lany/rainbow"})
    void lookupSendsTheOriginalUsername(String username) {
        var requests = new ArrayList<Request>();

        User result = service(requests).getUserByUsername(username);

        assertEquals(List.of("users", "by-username", username),
                pathSegments(onlyRequest(requests, HttpMethod.GET).uri()));
        assertEquals("test-user", result.getUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "lany.rainbow@gmail.com", "lany+rainbow@gmail.com", "lany%40gmail.com", "lany rainbow", "lany/rainbow"})
    void updateSendsTheSameUsernameInThePathAndBody(String username) throws Exception {
        var requests = new ArrayList<Request>();
        User user = new User();
        user.setUsername(username);

        service(requests).updateUser(user);

        Request request = onlyRequest(requests, HttpMethod.PUT);
        assertEquals(List.of("users", username), pathSegments(request.uri()));
        assertEquals(username, new ObjectMapper().readTree(request.body()).get("username").asText());
    }

    private UserService service(List<Request> requests) {
        RestTemplate restTemplate = new RestTemplate();
        // Capture the real HTTP request after Spring expands and encodes its URL.
        restTemplate.getInterceptors().add((request, body, execution) -> {
            requests.add(new Request(request.getURI(), request.getMethod(), body));
            return new ClientHttpResponse() {
                public HttpStatus getStatusCode() { return HttpStatus.OK; }
                public String getStatusText() { return "OK"; }
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-Alpha-Session", "test-session");
                    return headers;
                }
                public InputStream getBody() {
                    return new ByteArrayInputStream("{\"userID\":1,\"username\":\"test-user\"}"
                            .getBytes(StandardCharsets.UTF_8));
                }
                public void close() {}
            };
        });
        Properties properties = new Properties();
        properties.setProperty("api.base.url", "https://example.invalid");
        properties.setProperty("api.token", "test-token");
        return new UserService(restTemplate, properties);
    }

    private Request onlyRequest(List<Request> requests, HttpMethod method) {
        assertEquals(1, requests.size());
        Request request = requests.getFirst();
        assertEquals(method, request.method());
        return request;
    }

    private List<String> pathSegments(URI uri) {
        // Match the API's decoding of @PathVariable values, including encoded slashes.
        return PathContainer.parsePath(uri.getRawPath()).elements().stream()
                .filter(element -> element instanceof PathContainer.PathSegment)
                .map(element -> ((PathContainer.PathSegment) element).valueToMatch())
                .toList();
    }

    private record Request(URI uri, HttpMethod method, byte[] body) {}
}
