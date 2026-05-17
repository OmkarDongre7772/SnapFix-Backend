package com.snapfix.integration.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportSupportRepository reportSupportRepository;

    @Autowired
    private ReportRepository reportRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reportSupportRepository.deleteAll();
        reportRepository.deleteAll();
        reset(storageService);
        when(storageService.uploadImage(any(MultipartFile.class)))
                .thenReturn("https://cdn.snapfix.test/notification-report.jpg");
    }

    @Test
    @DisplayName("Creating a report creates an unread notification for the citizen")
    void createReport_createsUnreadNotificationForCreator() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("notification-create"), "CITIZEN");

        createReport(accessToken, "Notification pothole", "POTHOLE", 19.076, 72.8777);

        HttpResponse<String> notifications = get("/notifications", accessToken);
        JsonNode body = objectMapper.readTree(notifications.body());

        assertThat(notifications.statusCode()).isEqualTo(200);
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("type").asText()).isEqualTo("REPORT_CREATED");
        assertThat(body.get(0).get("message").asText()).contains("POTHOLE");
        assertThat(body.get(0).get("read").asBoolean()).isFalse();
        assertThat(body.get(0).get("createdAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Unread filter and mark-as-read work for current user")
    void markRead_updatesUnreadFilterForCurrentUser() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("notification-read"), "CITIZEN");
        createReport(accessToken, "Unread report", "GARBAGE", 18.52, 73.85);

        JsonNode unreadBefore = objectMapper.readTree(get("/notifications?unread=true", accessToken).body());
        String notificationId = unreadBefore.get(0).get("notificationId").asText();

        HttpResponse<String> markRead = patch("/notifications/" + notificationId + "/read", accessToken);
        JsonNode unreadAfter = objectMapper.readTree(get("/notifications?unread=true", accessToken).body());
        JsonNode readAfter = objectMapper.readTree(get("/notifications?unread=false", accessToken).body());

        assertThat(markRead.statusCode()).isEqualTo(204);
        assertThat(unreadAfter).isEmpty();
        assertThat(readAfter).hasSize(1);
        assertThat(readAfter.get(0).get("notificationId").asText()).isEqualTo(notificationId);
        assertThat(readAfter.get(0).get("read").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("User cannot mark another user's notification as read")
    void markRead_otherUsersNotification_returns404() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("notification-owner"), "CITIZEN");
        String otherToken = registerAndLogin(uniqueEmail("notification-other"), "CITIZEN");
        createReport(ownerToken, "Private notification", "ROAD_DAMAGE", 17.385, 78.4867);
        JsonNode ownerNotifications = objectMapper.readTree(get("/notifications", ownerToken).body());
        String notificationId = ownerNotifications.get(0).get("notificationId").asText();

        HttpResponse<String> forbiddenRead = patch("/notifications/" + notificationId + "/read", otherToken);
        JsonNode stillUnread = objectMapper.readTree(get("/notifications?unread=true", ownerToken).body());

        assertThat(forbiddenRead.statusCode()).isEqualTo(404);
        assertThat(stillUnread).hasSize(1);
        assertThat(stillUnread.get(0).get("notificationId").asText()).isEqualTo(notificationId);
    }

    @Test
    @DisplayName("Duplicate report support creates notification for original report owner")
    void duplicateReport_createsSupportedNotificationForOriginalOwner() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("notification-duplicate-owner"), "CITIZEN");
        String supporterToken = registerAndLogin(uniqueEmail("notification-duplicate-supporter"), "CITIZEN");
        createReport(ownerToken, "Water leak owner report", "WATER_LEAK", 28.6139, 77.2090);

        HttpResponse<String> duplicate = createReport(
                supporterToken,
                "Same water leak support",
                "WATER_LEAK",
                28.6140,
                77.2091);
        JsonNode ownerNotifications = objectMapper.readTree(get("/notifications", ownerToken).body());

        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(ownerNotifications).hasSize(2);
        assertThat(ownerNotifications.findValuesAsText("type")).contains("REPORT_CREATED", "REPORT_SUPPORTED");
    }

    @Test
    @DisplayName("Notifications require authentication")
    void notifications_withoutToken_returns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/notifications")).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Worker discovery endpoint returns nearby reports for workers only")
    void workerNearbyReports_requiresWorkerRole() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("worker-discovery-citizen"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("worker-discovery-worker"), "WORKER");
        postJson(
                "/workers/profile",
                """
                        {
                          "skills": ["electrical"],
                          "lat": 12.9716,
                          "lng": 77.5946
                        }
                        """,
                workerToken);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Worker-visible report",
                "STREETLIGHT",
                12.9716,
                77.5946).body());

        HttpResponse<String> workerResponse = get("/workers/reports/nearby", workerToken);
        HttpResponse<String> citizenResponse = get("/workers/reports/nearby", citizenToken);
        JsonNode workerBody = objectMapper.readTree(workerResponse.body());

        assertThat(workerResponse.statusCode()).isEqualTo(200);
        assertThat(workerBody).hasSize(1);
        assertThat(workerBody.get(0).get("id").asText()).isEqualTo(report.get("id").asText());
        assertThat(citizenResponse.statusCode()).isEqualTo(403);
    }

    private String registerAndLogin(String email, String role) throws Exception {
        String password = "Password123!";
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s",
                  "name": "Notification Test User"
                }
                """.formatted(email, password, role);
        HttpResponse<String> register = postJson("/auth/register", registerBody, null);
        assertThat(register.statusCode())
                .as("register response body: %s", register.body())
                .isEqualTo(200);

        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        HttpResponse<String> login = postJson("/auth/login", loginBody, null);
        assertThat(login.statusCode())
                .as("login response body: %s", login.body())
                .isEqualTo(200);

        return objectMapper.readTree(login.body()).get("accessToken").asText();
    }

    private HttpResponse<String> createReport(
            String accessToken,
            String description,
            String category,
            double lat,
            double lng) throws Exception {

        String boundary = "snapfix-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, description, category, lat, lng);

        HttpRequest request = HttpRequest.newBuilder(uri("/reports"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private byte[] multipartBody(String boundary, String description, String category, double lat, double lng) {
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            writeTextPart(body, boundary, "description", description);
            writeTextPart(body, boundary, "category", category);
            writeTextPart(body, boundary, "lat", String.valueOf(lat));
            writeTextPart(body, boundary, "lng", String.valueOf(lng));
            writeFilePart(body, boundary);
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return body.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build multipart request", e);
        }
    }

    private void writeTextPart(ByteArrayOutputStream body, String boundary, String name, String value) throws Exception {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream body, String boundary) throws Exception {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write("Content-Disposition: form-data; name=\"image\"; filename=\"issue.jpg\"\r\n".getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: image/jpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        body.write("fake-image-content".getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
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

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String uniqueEmail(String prefix) {
        String safePrefix = prefix.substring(0, Math.min(prefix.length(), 20));
        return safePrefix + "-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }
}
