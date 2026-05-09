package com.snapfix.integration.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.common.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Authenticated citizen can fetch current user profile")
    void getCurrentUser_citizen_returnsCitizenProfile() throws Exception {
        String email = uniqueEmail("citizen-me");
        String accessToken = registerAndLogin(email, "Password123!", "CITIZEN", "Citizen Me");

        HttpResponse<String> response = get("/user/me", accessToken);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("email").asText()).isEqualTo(email);
        assertThat(body.get("role").asText()).isEqualTo("CITIZEN");
        assertThat(body.path("profile").path("name").asText()).isEqualTo("Citizen Me");
        assertThat(body.path("profile").path("reportsSubmitted").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("Authenticated citizen can update profile name and location")
    void updateProfile_citizen_updatesNameAndLocation() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("citizen-update"), "Password123!", "CITIZEN", "Before Name");

        HttpResponse<String> update = putJson(
                "/user/profile",
                """
                {
                  "name": "After Name",
                  "latitude": 19.076,
                  "longitude": 72.8777
                }
                """,
                accessToken);
        JsonNode body = objectMapper.readTree(update.body());

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(body.get("name").asText()).isEqualTo("After Name");
        assertThat(body.path("location").path("latitude").asDouble()).isEqualTo(19.076);
        assertThat(body.path("location").path("longitude").asDouble()).isEqualTo(72.8777);
    }

    @Test
    @DisplayName("Authenticated worker can fetch and update worker profile")
    void workerProfile_fetchAndUpdate_returnsWorkerFields() throws Exception {
        String email = uniqueEmail("worker");
        String accessToken = registerAndLogin(email, "Password123!", "WORKER", "Worker One");

        HttpResponse<String> before = get("/user/me", accessToken);
        JsonNode beforeBody = objectMapper.readTree(before.body());

        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(beforeBody.get("email").asText()).isEqualTo(email);
        assertThat(beforeBody.get("role").asText()).isEqualTo("WORKER");
        assertThat(beforeBody.path("profile").path("name").asText()).isEqualTo("Worker One");
        assertThat(beforeBody.path("profile").path("rating").asDouble()).isEqualTo(0.0);

        HttpResponse<String> update = putJson(
                "/user/profile",
                """
                {
                  "name": "Worker Updated",
                  "skills": ["plumbing", "electrical"]
                }
                """,
                accessToken);
        JsonNode updateBody = objectMapper.readTree(update.body());

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(updateBody.get("name").asText()).isEqualTo("Worker Updated");
        assertThat(updateBody.path("skills").get(0).asText()).isEqualTo("plumbing");
        assertThat(updateBody.path("skills").get(1).asText()).isEqualTo("electrical");
    }

    @Test
    @DisplayName("Profile update requires authentication")
    void updateProfile_withoutToken_returns401() throws Exception {
        HttpResponse<String> response = putJson(
                "/user/profile",
                "{\"name\":\"No Token\"}",
                null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private String registerAndLogin(String email, String password, String role, String name) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s",
                  "name": "%s"
                }
                """.formatted(email, password, role, name);
        HttpResponse<String> registerResponse = postJson("/auth/register", registerBody, null);
        assertThat(registerResponse.statusCode()).isEqualTo(200);

        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        HttpResponse<String> loginResponse = postJson("/auth/login", loginBody, null);
        assertThat(loginResponse.statusCode()).isEqualTo(200);

        return objectMapper.readTree(loginResponse.body()).get("accessToken").asText();
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> putJson(String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
