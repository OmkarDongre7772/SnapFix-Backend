package com.snapfix.integration.release3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.notification.repository.NotificationRepository;
import com.snapfix.payment.entity.PaymentStatus;
import com.snapfix.payment.repository.PaymentRepository;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;
import com.snapfix.user.entity.User;
import com.snapfix.user.repository.UserRepository;
import com.snapfix.user.repository.WorkerProfileRepository;
import com.snapfix.verification.repository.VerificationRepository;
import com.snapfix.wallet.repository.TransactionRepository;
import com.snapfix.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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

public class Release3Phase5PaymentWalletIntegrationTest extends BaseIntegrationTest {

    private static final BigDecimal BID_AMOUNT = new BigDecimal("1500");

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
    private WalletRepository walletRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
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
        stubStorageUpload("https://cdn.snapfix.test/release3-phase5.jpg");
    }

    @Test
    @DisplayName("Worker profile creation auto-creates an empty wallet")
    void workerProfileCreation_autoCreatesWalletWithZeroBalance() throws Exception {
        String workerEmail = uniqueEmail("r3p5-wallet-worker");
        String workerToken = registerAndLogin(workerEmail, "WORKER");

        completeWorkerProfile(workerToken, 12.9716, 77.5946);

        User worker = userRepository.findByEmail(workerEmail).orElseThrow();
        HttpResponse<String> walletResponse = get("/workers/wallet", workerToken);
        JsonNode walletBody = objectMapper.readTree(walletResponse.body());

        assertThat(walletResponse.statusCode()).as(walletResponse.body()).isEqualTo(200);
        assertThat(new BigDecimal(walletBody.get("balance").asText())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(walletRepository.findByWorker_Id(worker.getId())).isNotNull();
        assertThat(workerProfileRepository.findById(worker.getId()).orElseThrow().getWallet()).isNotNull();
    }

    @Test
    @DisplayName("Admin final approval creates a pending payment for the approved bid amount")
    void approveTask_completedTaskCreatesPendingPayment() throws Exception {
        Scenario scenario = createVerifiedScenario();

        HttpResponse<String> approve = postNoBody("/admin/tasks/" + scenario.taskId() + "/approve", scenario.adminToken());

        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
        assertThat(paymentRepository.findAll()).singleElement().satisfies(payment -> {
            assertThat(payment.getTask().getId().toString()).isEqualTo(scenario.taskId());
            assertThat(payment.getWorker().getId().toString()).isEqualTo(scenario.workerId());
            assertThat(payment.getAmount()).isEqualByComparingTo(BID_AMOUNT);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        });
    }

    @Test
    @DisplayName("Admin releases completed task payment and worker sees updated wallet and history")
    void releasePayment_completedTaskCreditsWalletCreatesCreditTransactionAndHistory() throws Exception {
        Scenario scenario = createCompletedScenario();

        HttpResponse<String> release = postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.adminToken());
        JsonNode releaseBody = objectMapper.readTree(release.body());
        HttpResponse<String> wallet = get("/workers/wallet", scenario.workerToken());
        HttpResponse<String> history = get("/workers/payments", scenario.workerToken());
        JsonNode walletBody = objectMapper.readTree(wallet.body());
        JsonNode historyBody = objectMapper.readTree(history.body());

        assertThat(release.statusCode()).as(release.body()).isEqualTo(200);
        assertThat(releaseBody.get("status").asText()).isEqualTo("RELEASED");
        assertThat(new BigDecimal(releaseBody.get("amount").asText())).isEqualByComparingTo(BID_AMOUNT);

        Task savedTask = taskRepository.findById(UUID.fromString(scenario.taskId())).orElseThrow();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PAYMENT_RELEASED);
        assertThat(new BigDecimal(walletBody.get("balance").asText())).isEqualByComparingTo(BID_AMOUNT);

        assertThat(paymentRepository.findAll()).singleElement().satisfies(payment -> {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RELEASED);
            assertThat(payment.getAmount()).isEqualByComparingTo(BID_AMOUNT);
            assertThat(payment.getReleasedAt()).isNotNull();
        });
        assertThat(transactionRepository.findAll()).singleElement().satisfies(transaction -> {
            assertThat(transaction.getAmount()).isEqualByComparingTo(BID_AMOUNT);
            assertThat(transaction.getType()).isEqualTo("CREDIT");
            assertThat(transaction.getReferenceId().getTask().getId().toString()).isEqualTo(scenario.taskId());
            assertThat(transaction.getTimestamp()).isNotNull();
        });
        assertThat(history.statusCode()).as(history.body()).isEqualTo(200);
        assertThat(historyBody).singleElement().satisfies(payment ->
                assertThat(payment.get("status").asText()).isEqualTo("RELEASED"));
        assertThat(notificationRepository.findByRecipient_Id(UUID.fromString(scenario.workerId())))
                .anySatisfy(notification -> assertThat(notification.getMessage()).containsIgnoringCase("payment"));
    }

    @Test
    @DisplayName("Only admin can release payment")
    void releasePayment_requiresAdminRole() throws Exception {
        Scenario scenario = createCompletedScenario();

        HttpResponse<String> workerRelease = postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.workerToken());
        HttpResponse<String> citizenRelease = postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.citizenToken());

        assertThat(workerRelease.statusCode()).as(workerRelease.body()).isEqualTo(403);
        assertThat(citizenRelease.statusCode()).as(citizenRelease.body()).isEqualTo(403);
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Payment release requires a completed task")
    void releasePayment_incompleteTaskReturns409AndDoesNotMutateMoney() throws Exception {
        Scenario scenario = createAssignedScenario();

        HttpResponse<String> release = postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.adminToken());

        assertThat(release.statusCode()).as(release.body()).isEqualTo(409);
        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(transactionRepository.findAll()).isEmpty();
        assertThat(walletRepository.findByWorker_Id(UUID.fromString(scenario.workerId())).getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Payment cannot be released twice")
    void releasePayment_duplicateReleaseReturns409AndKeepsSingleTransaction() throws Exception {
        Scenario scenario = createCompletedScenario();
        assertThat(postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.adminToken()).statusCode())
                .isEqualTo(200);

        HttpResponse<String> duplicate = postNoBody("/admin/payments/" + scenario.taskId() + "/release", scenario.adminToken());

        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(409);
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(walletRepository.findByWorker_Id(UUID.fromString(scenario.workerId())).getBalance())
                .isEqualByComparingTo(BID_AMOUNT);
    }

    private Scenario createCompletedScenario() throws Exception {
        Scenario scenario = createVerifiedScenario();
        HttpResponse<String> approve = postNoBody("/admin/tasks/" + scenario.taskId() + "/approve", scenario.adminToken());
        assertThat(approve.statusCode()).as(approve.body()).isEqualTo(200);
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
        stubStorageUpload("https://cdn.snapfix.test/release3-phase5-proof.jpg");
        HttpResponse<String> proof = uploadProof(scenario.taskId(), scenario.workerToken());
        assertThat(proof.statusCode()).as(proof.body()).isEqualTo(200);
        return scenario;
    }

    private Scenario createAssignedScenario() throws Exception {
        String citizenToken = registerAndLogin(uniqueEmail("r3p5-owner"), "CITIZEN");
        String workerEmail = uniqueEmail("r3p5-worker");
        String workerToken = registerAndLogin(workerEmail, "WORKER");
        String adminToken = registerAndLogin(uniqueEmail("r3p5-admin"), "ADMIN");
        String workerId = userRepository.findByEmail(workerEmail).orElseThrow().getId().toString();

        completeWorkerProfile(workerToken, 12.9716, 77.5946);
        JsonNode report = objectMapper.readTree(createReport(
                citizenToken,
                "Release 3 phase 5 payment task",
                "ROAD_DAMAGE",
                12.9716,
                77.5946).body());
        JsonNode bid = objectMapper.readTree(placeBid(workerToken, report.get("id").asText()).body());
        approveBid(adminToken, bid.get("id").asText());

        Task task = taskRepository.findAll().get(0);
        return new Scenario(task.getId().toString(), workerId, citizenToken, workerToken, adminToken);
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
                          "bidAmount": %s,
                          "durationEstimate": 4,
                          "resourceNote": "payment phase tools"
                        }
                        """.formatted(reportId, BID_AMOUNT),
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
                          "name": "Release 3 Phase 5 User"
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
        writeTextPart(body, boundary, "remarks", "Ready for payment phase");
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
            String workerId,
            String citizenToken,
            String workerToken,
            String adminToken) {
    }
}
