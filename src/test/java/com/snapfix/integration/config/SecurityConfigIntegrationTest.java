package com.snapfix.integration.config;

import com.snapfix.common.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityConfigIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("Protected endpoint returns 401 Unauthorized without JWT")
    void accessProtectedEndpoint_noToken_returns401() throws Exception {
        // Given / When
        HttpRequest request = HttpRequest.newBuilder(uri("/user/me")).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"status\":401");
        assertThat(response.body()).contains("\"timestamp\"");
    }

    @Test
    @DisplayName("Public endpoints like login are accessible without JWT")
    void accessPublicEndpoint_noToken_returns400Or200() throws Exception {
        // Given / When
        HttpRequest request = HttpRequest.newBuilder(uri("/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        // A 400 means it hit the controller and validation failed, not a 401/403 auth error.
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
