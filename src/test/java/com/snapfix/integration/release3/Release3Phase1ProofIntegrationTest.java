package com.snapfix.integration.release3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.proof.entity.Proof;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Release3Phase1ProofIntegrationTest extends BaseIntegrationTest {

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
        stubStorageUpload("https://cdn.snapfix.test/release3-proof.jpg");
    }

    @Test
    @DisplayName("Worker uploads proof, task becomes proof submitted, and permitted users can view proof")
    void uploadProof_successPersistsProofAndEnforcesViewOwnership() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3-owner"), "CITIZEN");
        String otherCitizenToken = registerAndLogin(uniqueEmail("r3-other-citizen"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3-worker"), "WORKER");
        String otherWorkerToken = registerAndLogin(uniqueEmail("r3-other-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3-admin"), "ADMIN");

        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Proof upload target task",
                "POTHOLE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText(), 1400).body());
        approveBid(adminToken, bid.get("id").asText());

        Task task = taskRepository.findAll().get(0);
        String taskId = task.getId().toString();
        assertThat(patch("/tasks/" + taskId + "/start", workerToken).statusCode()).isEqualTo(200);
        stubStorageUpload("https://cdn.snapfix.test/release3-proof.jpg");

        HttpResponse<String> upload = uploadProof(taskId, workerToken, true, 12.9717, 77.5947, "Work completed");
        JsonNode uploadBody = objectMapper.readTree(upload.body());

        assertThat(upload.statusCode()).as(upload.body()).isEqualTo(200);
        assertThat(uploadBody.get("taskId").asText()).isEqualTo(taskId);
        assertThat(uploadBody.get("workerId").asText()).isEqualTo(task.getWorker().getId().toString());
        assertThat(uploadBody.get("imageUrl").asText()).isEqualTo("https://cdn.snapfix.test/release3-proof.jpg");
        assertThat(uploadBody.get("lat").asDouble()).isEqualTo(12.9717);
        assertThat(uploadBody.get("lng").asDouble()).isEqualTo(77.5947);
        assertThat(uploadBody.get("remarks").asText()).isEqualTo("Work completed");

        Task savedTask = taskRepository.findById(task.getId()).orElseThrow();
        Proof savedProof = proofRepository.findByTask_Id(task.getId()).orElseThrow();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PROOF_SUBMITTED);
        assertThat(savedProof.getTask().getId()).isEqualTo(task.getId());
        assertThat(savedProof.getWorker().getId()).isEqualTo(task.getWorker().getId());
        verify(storageService).uploadImage(any(MultipartFile.class));

        assertThat(get("/tasks/" + taskId + "/proof", workerToken).statusCode()).isEqualTo(200);
        assertThat(get("/tasks/" + taskId + "/proof", citizenToken).statusCode()).isEqualTo(200);
        assertThat(get("/tasks/" + taskId + "/proof", adminToken).statusCode()).isEqualTo(200);
        assertThat(get("/tasks/" + taskId + "/proof", otherWorkerToken).statusCode()).isEqualTo(403);
        assertThat(get("/tasks/" + taskId + "/proof", otherCitizenToken).statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Wrong worker cannot upload proof")
    void uploadProof_wrongWorkerReturns403() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3-wrong-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3-right-worker"), "WORKER");
        String otherWorkerToken = registerAndLogin(uniqueEmail("r3-wrong-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3-wrong-admin"), "ADMIN");
        String taskId = createAssignedTask(citizenToken, workerToken, adminToken);

        assertThat(patch("/tasks/" + taskId + "/start", workerToken).statusCode()).isEqualTo(200);

        HttpResponse<String> upload = uploadProof(taskId, otherWorkerToken, true, 12.9717, 77.5947, "Not my task");

        assertThat(upload.statusCode()).as(upload.body()).isEqualTo(403);
        assertThat(proofRepository.findByTask_Id(UUID.fromString(taskId))).isEmpty();
    }

    @Test
    @DisplayName("Proof cannot be uploaded before task is in progress")
    void uploadProof_taskNotInProgressReturns409() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3-state-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3-state-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3-state-admin"), "ADMIN");
        String taskId = createAssignedTask(citizenToken, workerToken, adminToken);

        HttpResponse<String> upload = uploadProof(taskId, workerToken, true, 12.9717, 77.5947, "Too early");

        assertThat(upload.statusCode()).as(upload.body()).isEqualTo(409);
        assertThat(proofRepository.findByTask_Id(UUID.fromString(taskId))).isEmpty();
    }

    @Test
    @DisplayName("Proof upload rejects missing image")
    void uploadProof_missingImageReturns400() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3-image-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3-image-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3-image-admin"), "ADMIN");
        String taskId = createAssignedTask(citizenToken, workerToken, adminToken);
        assertThat(patch("/tasks/" + taskId + "/start", workerToken).statusCode()).isEqualTo(200);

        HttpResponse<String> upload = uploadProof(taskId, workerToken, false, 12.9717, 77.5947, "No image");

        assertThat(upload.statusCode()).as(upload.body()).isEqualTo(400);
        assertThat(proofRepository.findByTask_Id(UUID.fromString(taskId))).isEmpty();
    }

    @Test
    @DisplayName("Only one proof can be uploaded for a task")
    void uploadProof_duplicateProofReturns409() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3-dupe-owner"), "CITIZEN");
        String workerToken = registerAndLogin(uniqueEmail("r3-dupe-worker"), "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3-dupe-admin"), "ADMIN");
        String taskId = createAssignedTask(citizenToken, workerToken, adminToken);
        assertThat(patch("/tasks/" + taskId + "/start", workerToken).statusCode()).isEqualTo(200);
        assertThat(uploadProof(taskId, workerToken, true, 12.9717, 77.5947, "First proof").statusCode())
                .isEqualTo(200);

        HttpResponse<String> duplicate = uploadProof(taskId, workerToken, true, 12.9718, 77.5948, "Second proof");

        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(409);
        assertThat(proofRepository.findAll()).hasSize(1);
    }

    private String createAssignedTask(String citizenToken, String workerToken, String adminToken) throws Exception {
        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Release 3 proof task",
                "ROAD_DAMAGE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText(), 1500).body());
        approveBid(adminToken, bid.get("id").asText());

        return taskRepository.findAll().get(0).getId().toString();
    }

    private void stubStorageUpload(String imageUrl) {
        reset(storageService);
        when(storageService.uploadImage(any(MultipartFile.class))).thenReturn(imageUrl);
    }

    private void approveBid(String adminToken, String bidId) throws Exception {
        HttpResponse<String> approve = postNoBody("/admin/bids/" + bidId + "/approve", adminToken);
        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
    }

    private HttpResponse<String> placeBid(String workerToken, String reportId, int amount) throws Exception {
        return postJson(
                "/bids",
                """
                        {
                          "reportId": "%s",
                          "bidAmount": %d,
                          "durationEstimate": 4,
                          "resourceNote": "proof tools"
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
        HttpResponse<String> register = postJson(
                "/auth/register",
                """
                        {
                          "email": "%s",
                          "password": "%s",
                          "role": "%s",
                          "name": "Release 3 Test User"
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

    private HttpResponse<String> uploadProof(
            String taskId,
            String accessToken,
            boolean includeImage,
            double lat,
            double lng,
            String remarks) throws Exception {

        String boundary = "snapfix-proof-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeTextPart(body, boundary, "lat", String.valueOf(lat));
        writeTextPart(body, boundary, "lng", String.valueOf(lng));
        writeTextPart(body, boundary, "remarks", remarks);
        if (includeImage) {
            writeFilePart(body, boundary, "image", "proof.jpg");
        }
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
}
