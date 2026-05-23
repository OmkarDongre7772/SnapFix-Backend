package com.snapfix.integration.release3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;
import com.snapfix.verification.entity.Verification;
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

public class Release3Phase2VerificationIntegrationTest extends BaseIntegrationTest {

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
        stubStorageUpload("https://cdn.snapfix.test/release3-phase2.jpg");
    }

    @Test
    @DisplayName("Report citizen verifies proof and task moves to verified by citizen")
    void verifyTask_ownerCitizenMovesTaskToVerified() throws Exception {
        Scenario scenario = createProofSubmittedScenario();

        HttpResponse<String> verify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED&comments=Looks%20good",
                scenario.citizenToken());
        JsonNode body = objectMapper.readTree(verify.body());

        assertThat(verify.statusCode()).as(verify.body()).isEqualTo(200);
        assertThat(body.get("taskId").asText()).isEqualTo(scenario.taskId());
        assertThat(body.get("citizenId").asText()).isEqualTo(scenario.citizenId());
        assertThat(body.get("status").asText()).isEqualTo("VERIFIED");
        assertThat(body.get("comments").asText()).isEqualTo("Looks good");
        assertThat(body.get("timestamp").asText()).isNotBlank();

        Task savedTask = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        Verification savedVerification = verificationRepository.findByTask_Id(savedTask.getId()).orElseThrow();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.VERIFIED_BY_CITIZEN);
        assertThat(savedTask.getRetryCount()).isZero();
        assertThat(savedVerification.getCitizenId().toString()).isEqualTo(scenario.citizenId());
    }

    @Test
    @DisplayName("Report citizen rejects proof and task retry count increments")
    void rejectTask_ownerCitizenMovesTaskToRejectedAndIncrementsRetry() throws Exception {
        Scenario scenario = createProofSubmittedScenario();

        HttpResponse<String> reject = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=REJECTED&comments=Photo%20is%20unclear",
                scenario.citizenToken());
        JsonNode body = objectMapper.readTree(reject.body());

        assertThat(reject.statusCode()).as(reject.body()).isEqualTo(200);
        assertThat(body.get("status").asText()).isEqualTo("REJECTED");
        assertThat(body.get("comments").asText()).isEqualTo("Photo is unclear");

        Task savedTask = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(savedTask.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Only report citizen can verify proof")
    void verifyTask_wrongCitizenAndWorkerAreRejected() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        String otherCitizenToken = registerAndLogin(uniqueEmail("r3p2-other-citizen"), "CITIZEN");

        HttpResponse<String> workerVerify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED",
                scenario.workerToken());
        HttpResponse<String> otherCitizenVerify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED",
                otherCitizenToken);

        assertThat(workerVerify.statusCode()).as(workerVerify.body()).isEqualTo(403);
        assertThat(otherCitizenVerify.statusCode()).as(otherCitizenVerify.body()).isEqualTo(403);
        assertThat(verificationRepository.findByTask_Id(UUID.fromString(scenario.taskId()))).isEmpty();
    }

    @Test
    @DisplayName("Task must be proof submitted before citizen verification")
    void verifyTask_beforeProofSubmittedReturns409() throws Exception {
        Scenario scenario = createAssignedScenario();

        HttpResponse<String> verify = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED",
                scenario.citizenToken());

        assertThat(verify.statusCode()).as(verify.body()).isEqualTo(409);
        assertThat(verificationRepository.findByTask_Id(UUID.fromString(scenario.taskId()))).isEmpty();
    }

    @Test
    @DisplayName("Task cannot be verified twice")
    void verifyTask_duplicateVerificationReturns409() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        assertThat(postNoBody("/tasks/" + scenario.taskId() + "/verify?status=VERIFIED", scenario.citizenToken()).statusCode())
                .isEqualTo(200);

        HttpResponse<String> duplicate = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=VERIFIED",
                scenario.citizenToken());

        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(409);
        assertThat(verificationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Rejected task cannot exceed maximum retry count")
    void verifyTask_rejectAtMaxRetryReturns409() throws Exception {
        Scenario scenario = createProofSubmittedScenario();
        Task task = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        task.setRetryCount(3);
        taskRepository.save(task);

        HttpResponse<String> reject = postNoBody(
                "/tasks/" + scenario.taskId() + "/verify?status=REJECTED&comments=Still%20bad",
                scenario.citizenToken());

        assertThat(reject.statusCode()).as(reject.body()).isEqualTo(409);
        assertThat(verificationRepository.findByTask_Id(task.getId())).isEmpty();
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getRetryCount()).isEqualTo(3);
    }

    private Scenario createProofSubmittedScenario() throws Exception {
        Scenario scenario = createAssignedScenario();
        assertThat(patch("/tasks/" + scenario.taskId() + "/start", scenario.workerToken()).statusCode()).isEqualTo(200);
        stubStorageUpload("https://cdn.snapfix.test/release3-phase2-proof.jpg");
        HttpResponse<String> proof = uploadProof(scenario.taskId(), scenario.workerToken());
        assertThat(proof.statusCode()).as(proof.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createAssignedScenario() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3p2-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3p2-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3p2-admin"), "ADMIN");

        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Release 3 verification task",
                "ROAD_DAMAGE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText()).body());
        approveBid(adminToken, bid.get("id").asText());

        String taskId = taskRepository.findAll().get(0).getId().toString();
        return new Scenario(taskId, report.get("citizenId").asText(), citizenToken, workerToken);
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
                          "resourceNote": "verification tools"
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
                          "name": "Release 3 Phase 2 User"
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
        writeTextPart(body, boundary, "remarks", "Ready for citizen verification");
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

    private record Scenario(String taskId, String citizenId, String citizenToken, String workerToken) {
    }
}
