package com.snapfix.integration.release3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.payment.repository.PaymentRepository;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.rating.repository.RatingRepository;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.repository.TaskRepository;
import com.snapfix.user.repository.UserRepository;
import com.snapfix.user.repository.WorkerProfileRepository;
import com.snapfix.verification.repository.VerificationRepository;
import com.snapfix.wallet.repository.TransactionRepository;
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

public class Release3Phase6RatingIntegrationTest extends BaseIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private ProofRepository proofRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportSupportRepository reportSupportRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        verificationRepository.deleteAll();
        proofRepository.deleteAll();
        adminRepository.deleteAll();
        taskRepository.deleteAll();
        bidRepository.deleteAll();
        notificationRepository.deleteAll();
        reportSupportRepository.deleteAll();
        reportRepository.deleteAll();
        stubStorageUpload("https://cdn.snapfix.test/release3-phase6.jpg");
    }

    @Test
    @DisplayName("Citizen rates completed task and worker rating summary updates")
    void rateWorker_completedTaskUpdatesAverageAndSummary() throws Exception {
        Scenario scenario = createCompletedScenario();

        HttpResponse<String> rate = rateWorker(scenario.workerId(), scenario.taskId(), 4, "Solid repair", scenario.citizenToken());
        JsonNode body = objectMapper.readTree(rate.body());
        HttpResponse<String> summary = get("/workers/" + scenario.workerId() + "/rating", scenario.citizenToken());
        JsonNode summaryBody = objectMapper.readTree(summary.body());

        assertThat(rate.statusCode()).as(rate.body()).isEqualTo(200);
        assertThat(body.get("task").get("id").asText()).isEqualTo(scenario.taskId());
        assertThat(body.get("workerId").asText()).isEqualTo(scenario.workerId());
        assertThat(body.get("citizenId").asText()).isEqualTo(scenario.citizenId());
        assertThat(body.get("score").asInt()).isEqualTo(4);
        assertThat(body.get("comment").asText()).isEqualTo("Solid repair");
        assertThat(body.get("timestamp").asText()).isNotBlank();

        assertThat(summary.statusCode()).as(summary.body()).isEqualTo(200);
        assertThat(summaryBody.get("average_score").asDouble()).isEqualTo(4.0);
        assertThat(summaryBody.get("task_count").asInt()).isEqualTo(1);
        assertThat(workerProfileRepository.findByUser_Id(UUID.fromString(scenario.workerId())).orElseThrow().getRating())
                .isEqualTo(4.0);
        assertThat(workerProfileRepository.findByUser_Id(UUID.fromString(scenario.workerId())).orElseThrow().getCompletedTasks())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Only original report citizen can rate the worker")
    void rateWorker_wrongCitizenReturns403() throws Exception {
        Scenario scenario = createCompletedScenario();
        String otherCitizenToken = registerAndLogin(uniqueEmail("r3p6-other-citizen"), "CITIZEN");

        HttpResponse<String> rate = rateWorker(scenario.workerId(), scenario.taskId(), 5, "Trying", otherCitizenToken);

        assertThat(rate.statusCode()).as(rate.body()).isEqualTo(403);
        assertThat(ratingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Worker and admin cannot submit rating")
    void rateWorker_workerAndAdminRolesReturn403() throws Exception {
        Scenario scenario = createCompletedScenario();

        HttpResponse<String> workerRate = rateWorker(scenario.workerId(), scenario.taskId(), 5, "Nope", scenario.workerToken());
        HttpResponse<String> adminRate = rateWorker(scenario.workerId(), scenario.taskId(), 5, "Nope", scenario.adminToken());

        assertThat(workerRate.statusCode()).as(workerRate.body()).isEqualTo(403);
        assertThat(adminRate.statusCode()).as(adminRate.body()).isEqualTo(403);
        assertThat(ratingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Task must be completed before rating")
    void rateWorker_assignedTaskReturns409() throws Exception {
        Scenario scenario = createAssignedScenario();

        HttpResponse<String> rate = rateWorker(scenario.workerId(), scenario.taskId(), 5, "Too soon", scenario.citizenToken());

        assertThat(rate.statusCode()).as(rate.body()).isEqualTo(409);
        assertThat(ratingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Task can only be rated once")
    void rateWorker_duplicateRatingReturns409() throws Exception {
        Scenario scenario = createCompletedScenario();
        assertThat(rateWorker(scenario.workerId(), scenario.taskId(), 5, "First", scenario.citizenToken()).statusCode())
                .isEqualTo(200);

        HttpResponse<String> duplicate = rateWorker(scenario.workerId(), scenario.taskId(), 3, "Second", scenario.citizenToken());

        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(409);
        assertThat(ratingRepository.findAll()).hasSize(1);
        assertThat(workerProfileRepository.findByUser_Id(UUID.fromString(scenario.workerId())).orElseThrow().getCompletedTasks())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Invalid score returns bad request")
    void rateWorker_invalidScoreReturns400() throws Exception {
        Scenario scenario = createCompletedScenario();

        HttpResponse<String> rate = rateWorker(scenario.workerId(), scenario.taskId(), 6, "Too much", scenario.citizenToken());

        assertThat(rate.statusCode()).as(rate.body()).isEqualTo(400);
        assertThat(ratingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Path worker must match task worker")
    void rateWorker_wrongWorkerPathReturns400() throws Exception {
        Scenario scenario = createCompletedScenario();
        String otherWorkerEmail = uniqueEmail("r3p6-other-worker");
        String otherWorkerToken = registerAndLogin(otherWorkerEmail, "WORKER");
        completeWorkerProfile(otherWorkerToken, 12.9750, 77.5950);
        String otherWorkerId = userRepository.findByEmail(otherWorkerEmail).orElseThrow().getId().toString();

        HttpResponse<String> rate = rateWorker(otherWorkerId, scenario.taskId(), 5, "Wrong path", scenario.citizenToken());

        assertThat(rate.statusCode()).as(rate.body()).isEqualTo(400);
        assertThat(ratingRepository.findAll()).isEmpty();
    }

    private Scenario createCompletedScenario() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        HttpResponse<String> verify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED&comments=Accepted",
                scenario.citizenToken());
        assertThat(verify.statusCode()).as(verify.body()).isEqualTo(200);

        HttpResponse<String> approve = postNoBody("/admin/tasks/" + scenario.taskId() + "/approve", scenario.adminToken());
        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createProofSubmittedScenario() throws Exception {
        Scenario scenario = createAssignedScenario();
        assertThat(patch("/tasks/" + scenario.taskId() + "/start", scenario.workerToken()).statusCode()).isEqualTo(200);
        stubStorageUpload("https://cdn.snapfix.test/release3-phase6-proof.jpg");
        HttpResponse<String> proof = uploadProof(scenario.taskId(), scenario.workerToken());
        assertThat(proof.statusCode()).as(proof.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createAssignedScenario() throws Exception {
        String citizenEmail = uniqueEmail("r3p6-owner");
        String citizenToken = registerAndLogin(citizenEmail, "CITIZEN");
        String workerEmail = uniqueEmail("r3p6-worker");
        String workerToken = registerAndLogin(workerEmail, "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3p6-admin"), "ADMIN");
        String citizenId = userRepository.findByEmail(citizenEmail).orElseThrow().getId().toString();
        String workerId = userRepository.findByEmail(workerEmail).orElseThrow().getId().toString();

        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Release 3 phase 6 rating task",
                "ROAD_DAMAGE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText()).body());
        approveBid(adminToken, bid.get("id").asText());

        Task task = taskRepository.findAll().get(0);
        return new Scenario(task.getId().toString(), citizenId, workerId, citizenToken, workerToken, adminToken);
    }

    private HttpResponse<String> rateWorker(String workerId, String taskId, int score, String comment, String accessToken) throws Exception {
        return postJson(
                "/workers/" + workerId + "/rating",
                """
                        {
                          "taskId": "%s",
                          "score": %s,
                          "comment": "%s"
                        }
                        """.formatted(taskId, score, comment),
                accessToken);
    }

    private void approveBid(String adminToken, String bidId) throws Exception {
        HttpResponse<String> approve = postNoBody("/admin/bids/" + bidId + "/approve", adminToken);
        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
    }

    private HttpResponse<String> placeBid(String workerToken, String reportId) throws Exception {
        return postJson(
                "/bids",
                """
                        {
                          "reportId": "%s",
                          "bidAmount": 1500,
                          "durationEstimate": 4,
                          "resourceNote": "rating phase tools"
                        }
                        """.formatted(reportId),
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
        HttpResponse<String> register = postJson(
                "/auth/register",
                """
                        {
                          "email": "%s",
                          "password": "%s",
                          "role": "%s",
                          "name": "Release 3 Phase 6 User"
                        }
                        """.formatted(email, password, role),
                null);
        assertThat(register.statusCode()).as(register.body()).isEqualTo(200);

        HttpResponse<String> login = postJson(
                "/auth/login",
                """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password),
                null);
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
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeTextPart(body, boundary, "description", description);
        writeTextPart(body, boundary, "category", category);
        writeTextPart(body, boundary, "lat", String.valueOf(lat));
        writeTextPart(body, boundary, "lng", String.valueOf(lng));
        writeFilePart(body, boundary, "image", "issue.jpg");
        finishMultipart(body, boundary);

        HttpRequest request = HttpRequest.newBuilder(uri("/reports"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> uploadProof(String taskId, String accessToken) throws Exception {
        String boundary = "snapfix-proof-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeTextPart(body, boundary, "lat", "12.9717");
        writeTextPart(body, boundary, "lng", "77.5947");
        writeTextPart(body, boundary, "remarks", "Ready for rating phase");
        writeFilePart(body, boundary, "image", "proof.jpg");
        finishMultipart(body, boundary);

        HttpRequest request = HttpRequest.newBuilder(uri("/tasks/" + taskId + "/proof"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void writeTextPart(ByteArrayOutputStream body, String boundary, String name, String value) throws Exception {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream body, String boundary, String name, String filename) throws Exception {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: image/jpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        body.write("fake-image-content".getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void finishMultipart(ByteArrayOutputStream body, String boundary) throws Exception {
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
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

    private HttpResponse<String> postNoBody(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.noBody());

        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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

    private void stubStorageUpload(String imageUrl) {
        reset(storageService);
        when(storageService.uploadImage(any(MultipartFile.class))).thenReturn(imageUrl);
    }

    private record Scenario(
            String taskId,
            String citizenId,
            String workerId,
            String citizenToken,
            String workerToken,
            String adminToken) {
    }
}
