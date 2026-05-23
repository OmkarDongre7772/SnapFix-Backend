package com.snapfix.integration.release2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.entity.Bid;
import com.snapfix.bid.entity.BidStatus;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;
import com.snapfix.user.repository.WorkerProfileRepository;
import com.snapfix.verification.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
// import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class Release2IntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProofRepository proofRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportSupportRepository reportSupportRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        verificationRepository.deleteAll();
        proofRepository.deleteAll();
        adminRepository.deleteAll();
        taskRepository.deleteAll();
        bidRepository.deleteAll();
        notificationRepository.deleteAll();
        reportSupportRepository.deleteAll();
        reportRepository.deleteAll();
        reset(storageService);
        when(storageService.uploadImage(any(MultipartFile.class)))
                .thenReturn("https://cdn.snapfix.test/release2-report.jpg");
    }

    @SuppressWarnings("unchecked")
@Test
    @DisplayName("Worker can complete profile, update PostGIS location, and discover nearby reports from stored location")
    void workerProfileLocationAndNearbyDiscovery_workEndToEnd() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r2-citizen"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r2-worker"), "WORKER");
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Streetlight near worker",
                "STREETLIGHT",
                12.9716,
                77.5946).body());

        HttpResponse<String> createProfile = postJson(
                "/workers/profile",
                """
                        {
                          "skills": ["electrical", "lights"],
                          "lat": 12.9716,
                          "lng": 77.5946
                        }
                        """,
                workerToken);
        JsonNode profile = objectMapper.readTree(createProfile.body());

        assertThat(createProfile.statusCode()).as(createProfile.body()).isEqualTo(200);
        assertThat(profile.get("lat").asDouble()).isEqualTo(12.9716);
        assertThat(profile.get("lng").asDouble()).isEqualTo(77.5946);
        assertThat(profile.get("available").asBoolean()).isTrue();

        HttpResponse<String> updateLocation = postJson(
                "/workers/location",
                """
                        {
                          "lat": 12.9717,
                          "lng": 77.5947
                        }
                        """,
                workerToken);
        assertThat(updateLocation.statusCode()).as(updateLocation.body()).isEqualTo(200);

        HttpResponse<String> nearby = get("/workers/reports/nearby", workerToken);
        JsonNode nearbyBody = objectMapper.readTree(nearby.body());

        assertThat(nearby.statusCode()).as(nearby.body()).isEqualTo(200);
        assertThat(nearbyBody).hasSize(1);
        assertThat(nearbyBody.get(0).get("id").asText()).isEqualTo(report.get("id").asText());

        assertThat(workerProfileRepository.findAll().get(0).getCurrentLocation()).isNotNull();
    }

    @Test
    @DisplayName("Worker can place and withdraw bid, duplicate bid is rejected, and resource note is optional")
    void workerBidLifecycle_enforcesDuplicateAndWithdrawRules() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r2-report-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r2-bid-worker"), "WORKER");
        completeWorkerProfile(workerToken, 19.076, 72.8777);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Road damage ready for bidding",
                "ROAD_DAMAGE",
                19.076,
                72.8777).body());

        HttpResponse<String> bid = postJson(
                "/bids",
                """
                        {
                          "reportId": "%s",
                          "bidAmount": 1500,
                          "durationEstimate": 6
                        }
                        """.formatted(report.get("id").asText()),
                workerToken);
        JsonNode bidBody = objectMapper.readTree(bid.body());

        assertThat(bid.statusCode()).as(bid.body()).isEqualTo(200);
        assertThat(bidBody.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(bidBody.get("resourceNote").asText()).isEmpty();

        HttpResponse<String> duplicate = postJson(
                "/bids",
                """
                        {
                          "reportId": "%s",
                          "bidAmount": 1800,
                          "durationEstimate": 5,
                          "resourceNote": "extra materials"
                        }
                        """.formatted(report.get("id").asText()),
                workerToken);
        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(409);

        HttpResponse<String> withdraw = delete("/bids/" + bidBody.get("id").asText(), workerToken);
        assertThat(withdraw.statusCode()).as(withdraw.body()).isEqualTo(200);

        Bid saved = bidRepository.findById(UUID.fromString(bidBody.get("id").asText())).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BidStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("Admin approval rejects competing bids, creates one task, updates report, and worker starts own task")
    void adminApprovalCreatesTaskAndWorkerStartsIt() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r2-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r2-approved-worker"), "WORKER");
        String competingWorkerToken = registerAndLogin(uniqueEmail("r2-rejected-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r2-admin"), "ADMIN");
        completeWorkerProfile(workerToken, 18.52, 73.85);
        completeWorkerProfile(competingWorkerToken, 18.5201, 73.8501);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Garbage cleanup marketplace task",
                "GARBAGE",
                18.52,
                73.85).body());

        JsonNode approvedBid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText(), 1200).body());
        JsonNode rejectedBid = objectMapper.readTree(placeBid(competingWorkerToken, report.get("id").asText(), 1300).body());

        HttpResponse<String> approve = postNoBody("/admin/bids/" + approvedBid.get("id").asText() + "/approve", adminToken);
        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);

        Bid savedApprovedBid = bidRepository.findById(UUID.fromString(approvedBid.get("id").asText())).orElseThrow();
        Bid savedRejectedBid = bidRepository.findById(UUID.fromString(rejectedBid.get("id").asText())).orElseThrow();
        Report savedReport = reportRepository.findById(UUID.fromString(report.get("id").asText())).orElseThrow();
        List<Task> tasks = taskRepository.findAll();

        assertThat(savedApprovedBid.getStatus()).isEqualTo(BidStatus.APPROVED);
        assertThat(savedRejectedBid.getStatus()).isEqualTo(BidStatus.REJECTED);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(adminRepository.findAll()).hasSize(1);

        String taskId = tasks.get(0).getId().toString();
        HttpResponse<String> otherWorkerTask = get("/tasks/" + taskId, competingWorkerToken);
        assertThat(otherWorkerTask.statusCode()).as(otherWorkerTask.body()).isEqualTo(403);

        HttpResponse<String> assignedTasks = get("/workers/tasks", workerToken);
        JsonNode assignedTasksBody = objectMapper.readTree(assignedTasks.body());
        assertThat(assignedTasks.statusCode()).as(assignedTasks.body()).isEqualTo(200);
        assertThat(assignedTasksBody).hasSize(1);
        assertThat(assignedTasksBody.get(0).get("id").asText()).isEqualTo(taskId);

        HttpResponse<String> start = patch("/tasks/" + taskId + "/start", workerToken);
        assertThat(start.statusCode()).as(start.body()).isEqualTo(200);
        assertThat(taskRepository.findById(tasks.get(0).getId()).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.IN_PROGRESS);
    }

    private HttpResponse<String> placeBid(String workerToken, String reportId, int amount) throws Exception {
        return postJson(
                "/bids",
                """
                        {
                          "reportId": "%s",
                          "bidAmount": %d,
                          "durationEstimate": 4,
                          "resourceNote": "basic tools"
                        }
                        """.formatted(reportId, amount),
                workerToken);
    }

    private void completeWorkerProfile(String workerToken, double lat, double lng) throws Exception {
        HttpResponse<String> response = postJson(
                "/workers/profile",
                """
                        {
                          "skills": ["roadwork", "cleanup"],
                          "lat": %s,
                          "lng": %s
                        }
                        """.formatted(lat, lng),
                workerToken);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private String registerAndLogin(String email, String role) throws Exception {
        String password = "Password123!";
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s",
                  "name": "Release 2 Test User"
                }
                """.formatted(email, password, role);
        HttpResponse<String> register = postJson("/auth/register", registerBody, null);
        assertThat(register.statusCode()).as(register.body()).isEqualTo(200);

        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        HttpResponse<String> login = postJson("/auth/login", loginBody, null);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);

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

    private HttpResponse<String> postNoBody(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.noBody())
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

    private HttpResponse<String> delete(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
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

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String uniqueEmail(String prefix) {
        String safePrefix = prefix.substring(0, Math.min(prefix.length(), 20));
        return safePrefix + "-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }
}
