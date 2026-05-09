package com.snapfix.integration.auth;

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

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Citizen can register and login")
    void registerAndLogin_citizen_returnsTokens() throws Exception {
        String email = uniqueEmail("citizen");
        String password = "Password123!";

        HttpResponse<String> registerResponse = register(email, password, "CITIZEN", "Test Citizen");

        assertThat(registerResponse.statusCode()).isEqualTo(200);
        assertThat(registerResponse.body()).contains("User registered successfully");

        JsonNode login = login(email, password);

        assertThat(login.get("accessToken").asText()).isNotBlank();
        assertThat(login.get("refreshToken").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Duplicate registration returns conflict")
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail("duplicate");

        register(email, "Password123!", "CITIZEN", "Original User");
        HttpResponse<String> duplicate = register(email, "Password123!", "CITIZEN", "Duplicate User");

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("Email already exists");
    }

    @Test
    @DisplayName("Refresh token rotates and revokes old refresh token")
    void refresh_validToken_rotatesRefreshToken() throws Exception {
        String email = uniqueEmail("refresh");
        String password = "Password123!";
        register(email, password, "CITIZEN", "Refresh User");
        JsonNode login = login(email, password);
        String originalRefreshToken = login.get("refreshToken").asText();

        HttpResponse<String> refreshResponse = postText("/auth/refresh", originalRefreshToken);
        JsonNode refresh = objectMapper.readTree(refreshResponse.body());

        assertThat(refreshResponse.statusCode()).isEqualTo(200);
        assertThat(refresh.get("accessToken").asText()).isNotBlank();
        assertThat(refresh.get("refreshToken").asText()).isNotBlank();
        assertThat(refresh.get("refreshToken").asText()).isNotEqualTo(originalRefreshToken);

        HttpResponse<String> reusedOldToken = postText("/auth/refresh", originalRefreshToken);

        assertThat(reusedOldToken.statusCode()).isEqualTo(409);
        assertThat(reusedOldToken.body()).contains("Token revoked");
    }

    @Test
    @DisplayName("Logout revokes refresh token and blacklists access token")
    void logout_validTokens_revokesRefreshAndAccess() throws Exception {
        String email = uniqueEmail("logout");
        String password = "Password123!";
        register(email, password, "CITIZEN", "Logout User");
        JsonNode login = login(email, password);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        HttpResponse<String> logout = postJson(
                "/auth/logout",
                "{\"refreshToken\":\"" + refreshToken + "\"}",
                accessToken);

        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(logout.body()).contains("Logged out successfully");

        HttpResponse<String> protectedEndpoint = get("/user/me", accessToken);
        assertThat(protectedEndpoint.statusCode()).isEqualTo(401);
        assertThat(protectedEndpoint.body()).contains("Token has been revoked");

        HttpResponse<String> refreshAfterLogout = postText("/auth/refresh", refreshToken);
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(409);
        assertThat(refreshAfterLogout.body()).contains("Token revoked");
    }

    private HttpResponse<String> register(String email, String password, String role, String name) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s",
                  "name": "%s"
                }
                """.formatted(email, password, role, name);
        return postJson("/auth/register", body);
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        HttpResponse<String> response = postJson("/auth/login", body);

        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        return postJson(path, body, null);
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

    private HttpResponse<String> postText(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
