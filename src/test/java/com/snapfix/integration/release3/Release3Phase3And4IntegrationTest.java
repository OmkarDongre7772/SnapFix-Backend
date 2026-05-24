package com.snapfix.integration.release3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;
import com.snapfix.user.repository.UserRepository;
import com.snapfix.verification.repository.VerificationRepository;
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

public class Release3Phase3And4IntegrationTest extends BaseIntegrationTest {

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
    private UserRepository userRepository;

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
        stubStorageUpload("https://cdn.snapfix.test/release3-phase3-4.jpg");
    }

    @Test
    @DisplayName("Worker retries rejected task and retry count persists")
    void retryTask_rejectedTaskMovesToInProgressAndIncrementsRetryCount() throws Exception {
        Scenario scenario = createRejectedScenario();

        HttpResponse<String> retry = postNoBody("/tasks/" + scenario.taskId() + "/retry", scenario.workerToken());
        JsonNode body = objectMapper.readTree(retry.body());

        assertThat(retry.statusCode()).as(retry.body()).isEqualTo(200);
        assertThat(body.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(body.get("retryCount").asInt()).isEqualTo(1);

        Task savedTask = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(savedTask.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fourth retry attempt returns conflict")
    void retryTask_atMaxRetryReturns409() throws Exception {
        Scenario scenario = createRejectedScenario();
        Task task = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        task.setRetryCount(3);
        taskRepository.save(task);

        HttpResponse<String> retry = postNoBody("/tasks/" + scenario.taskId() + "/retry", scenario.workerToken());

        assertThat(retry.statusCode()).as(retry.body()).isEqualTo(409);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Admin lists verified tasks and views task detail")
    void adminTaskEndpoints_listAndDetailVerifiedTasks() throws Exception {
        Scenario scenario = createVerifiedScenario();

        HttpResponse<String> list = get("/admin/tasks?status=VERIFIED_BY_CITIZEN", scenario.adminToken());
        HttpResponse<String> detail = get("/admin/tasks/" + scenario.taskId(), scenario.adminToken());
        JsonNode listBody = objectMapper.readTree(list.body());
        JsonNode detailBody = objectMapper.readTree(detail.body());

        assertThat(list.statusCode()).as(list.body()).isEqualTo(200);
        assertThat(listBody).anySatisfy(task -> assertThat(task.get("id").asText()).isEqualTo(scenario.taskId()));
        assertThat(detail.statusCode()).as(detail.body()).isEqualTo(200);
        assertThat(detailBody.path("task").path("id").asText()).isEqualTo(scenario.taskId());
        assertThat(detailBody.path("proof").path("taskId").asText()).isEqualTo(scenario.taskId());
        assertThat(detailBody.path("verification").path("taskId").asText()).isEqualTo(scenario.taskId());
    }

    @Test
    @DisplayName("Admin approves citizen verified task and report is completed")
    void approveTask_verifiedTaskMovesToCompleted() throws Exception {
        Scenario scenario = createVerifiedScenario();

        HttpResponse<String> approve = postNoBody("/admin/tasks/" + scenario.taskId() + "/approve", scenario.adminToken());
        JsonNode body = objectMapper.readTree(approve.body());

        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
        assertThat(body.get("status").asText()).isEqualTo("COMPLETED");

        Task task = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getReport().getStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(adminRepository.findAll()).anySatisfy(log -> assertThat(log.getAction()).isEqualTo("Task Approved"));
    }

    @Test
    @DisplayName("Admin rejects citizen verified task")
    void rejectTask_verifiedTaskMovesToRejected() throws Exception {
        Scenario scenario = createVerifiedScenario();

        HttpResponse<String> reject = postNoBody("/admin/tasks/" + scenario.taskId() + "/reject", scenario.adminToken());

        assertThat(reject.statusCode()).as(reject.body()).isEqualTo(200);
        assertThat(taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.REJECTED);
        assertThat(adminRepository.findAll()).anySatisfy(log -> assertThat(log.getAction()).isEqualTo("Task Rejected"));
    }

    @Test
    @DisplayName("Admin reassigns task with request body")
    void reassignTask_setsNewWorkerAndAssignedStatus() throws Exception {
        Scenario scenario = createRejectedScenario();
        String newWorkerEmail = uniqueEmail("r3p4-new-worker");
        String newWorkerToken = registerAndLogin(newWorkerEmail, "WORKER");
        completeWorkerProfile(newWorkerToken, 12.9718, 77.5948);
        String newWorkerId = userRepository.findByEmail(newWorkerEmail).orElseThrow().getId().toString();

        HttpResponse<String> reassign = postJson(
                "/admin/tasks/" + scenario.taskId() + "/reassign",
                """
                        {
                          "newWorkerId": "%s"
                        }
                        """.formatted(newWorkerId),
                scenario.adminToken());
        JsonNode body = objectMapper.readTree(reassign.body());

        assertThat(reassign.statusCode()).as(reassign.body()).isEqualTo(200);
        assertThat(body.get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(body.get("workerId").asText()).isEqualTo(newWorkerId);
        assertThat(adminRepository.findAll()).anySatisfy(log -> assertThat(log.getAction()).isEqualTo("Task Re-assigned"));
    }

    private Scenario createRejectedScenario() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        HttpResponse<String> reject = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=REJECTED&comments=Needs%20more%20work",
                scenario.citizenToken());
        assertThat(reject.statusCode()).as(reject.body()).isEqualTo(200);
        assertThat(taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow().getRetryCount()).isZero();
        return scenario;
    }

    private Scenario createVerifiedScenario() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        HttpResponse<String> verify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED&comments=Accepted",
                scenario.citizenToken());
        assertThat(verify.statusCode()).as(verify.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createProofSubmittedScenario() throws Exception {
        Scenario scenario = createAssignedScenario();
        assertThat(patch("/tasks/" + scenario.taskId() + "/start", scenario.workerToken()).statusCode()).isEqualTo(200);
        stubStorageUpload("https://cdn.snapfix.test/release3-phase3-4-proof.jpg");
        HttpResponse<String> proof = uploadProof(scenario.taskId(), scenario.workerToken());
        assertThat(proof.statusCode()).as(proof.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createAssignedScenario() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3p34-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3p34-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3p34-admin"), "ADMIN");

        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Release 3 phase 3 and 4 task",
                "ROAD_DAMAGE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText()).body());
        approveBid(adminToken, bid.get("id").asText());

        Task task = taskRepository.findAll().get(0);
        return new Scenario(task.getId().toString(), citizenToken, workerToken, adminToken);
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
                          "resourceNote": "phase 3 and 4 tools"
                        }
                        """.formatted(reportId),
                workerToken);
    }

    private HttpResponse<String> completeWorkerProfile(String workerToken, double lat, double lng) throws Exception {
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
        return response;
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
                          "name": "Release 3 Phase 3 and 4 User"
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
        writeTextPart(body, boundary, "remarks", "Ready for final review");
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

    private record Scenario(String taskId, String citizenToken, String workerToken, String adminToken) {
    }
}
