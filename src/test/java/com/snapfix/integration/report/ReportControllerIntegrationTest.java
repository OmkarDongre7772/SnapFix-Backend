package com.snapfix.integration.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfix.common.BaseIntegrationTest;
import com.snapfix.report.entity.Category;
import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportSupportRepository reportSupportRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        reportSupportRepository.deleteAll();
        reportRepository.deleteAll();
        reset(storageService);
        when(storageService.uploadImage(any(MultipartFile.class)))
                .thenReturn("https://cdn.snapfix.test/report.jpg");
    }

    @Test
    @DisplayName("Citizen can create report with image upload and fetch it by id")
    void createReport_multipart_uploadsImageAndPersistsPoint() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("report-create"), "CITIZEN");

        HttpResponse<String> response = createReport(
                accessToken,
                "Large pothole near the bus stop",
                "POTHOLE",
                19.076,
                72.8777);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("message").asText()).isEqualTo("Report created successfully");
        assertThat(body.get("imageUrl").asText()).isEqualTo("https://cdn.snapfix.test/report.jpg");
        assertThat(body.get("description").asText()).isEqualTo("Large pothole near the bus stop");
        assertThat(body.get("category").asText()).isEqualTo("POTHOLE");
        assertThat(body.get("status").asText()).isEqualTo("CREATED");
        assertThat(body.get("supportCount").asInt()).isEqualTo(1);
        assertThat(body.get("lat").asDouble()).isEqualTo(19.076);
        assertThat(body.get("lng").asDouble()).isEqualTo(72.8777);

        HttpResponse<String> fetched = get("/reports/" + body.get("id").asText(), accessToken);
        JsonNode fetchedBody = objectMapper.readTree(fetched.body());

        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(fetchedBody.get("id").asText()).isEqualTo(body.get("id").asText());
        assertThat(fetchedBody.get("imageUrl").asText()).isEqualTo("https://cdn.snapfix.test/report.jpg");
        verify(storageService, times(1)).uploadImage(any(MultipartFile.class));
    }

    @Test
    @DisplayName("Nearby reports are returned within radius and sorted by distance")
    void nearbyReports_returnsWithinRadiusSortedByDistance() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("nearby"), "CITIZEN");

        JsonNode near = objectMapper.readTree(createReport(accessToken, "Near report", "POTHOLE", 19.0001, 72.0001).body());
        JsonNode farther = objectMapper.readTree(createReport(accessToken, "Farther report", "POTHOLE", 19.03, 72.03).body());
        createReport(accessToken, "Outside report", "POTHOLE", 19.20, 72.20);

        HttpResponse<String> response = get("/reports/nearby?lat=19.0&lng=72.0&radius=5000", accessToken);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body).hasSize(2);
        assertThat(body.get(0).get("id").asText()).isEqualTo(near.get("id").asText());
        assertThat(body.get(1).get("id").asText()).isEqualTo(farther.get("id").asText());
    }

    @Test
    @DisplayName("Citizen can support once and duplicate support is rejected")
    void supportReport_incrementsOnceAndRejectsDuplicateSupport() throws Exception {
        String creatorToken = registerAndLogin(uniqueEmail("support-creator"), "CITIZEN");
        String supporterToken = registerAndLogin(uniqueEmail("supporter"), "CITIZEN");
        JsonNode report = objectMapper.readTree(createReport(
                creatorToken,
                "Broken streetlight",
                "STREETLIGHT",
                18.52,
                73.85).body());

        HttpResponse<String> support = post("/reports/" + report.get("id").asText() + "/support", supporterToken);
        JsonNode supported = objectMapper.readTree(support.body());

        assertThat(support.statusCode()).isEqualTo(200);
        assertThat(supported.get("message").asText()).isEqualTo("Support added successfully");
        assertThat(supported.get("supportCount").asInt()).isEqualTo(2);

        HttpResponse<String> duplicate = post("/reports/" + report.get("id").asText() + "/support", supporterToken);

        assertThat(duplicate.statusCode()).isEqualTo(400);
        assertThat(duplicate.body()).contains("Already supported");
    }

    @Test
    @DisplayName("Duplicate report within 50 metres and same category adds support to existing report")
    void createReport_duplicateWithin50mSameCategory_addsSupportToExistingReport() throws Exception {
        String creatorToken = registerAndLogin(uniqueEmail("duplicate-creator"), "CITIZEN");
        String duplicateReporterToken = registerAndLogin(uniqueEmail("duplicate-reporter"), "CITIZEN");
        JsonNode original = objectMapper.readTree(createReport(
                creatorToken,
                "Water leak from main line",
                "WATER_LEAK",
                28.6139,
                77.2090).body());

        HttpResponse<String> duplicateResponse = createReport(
                duplicateReporterToken,
                "Same leak, still flowing",
                "WATER_LEAK",
                28.6140,
                77.2091);
        JsonNode duplicate = objectMapper.readTree(duplicateResponse.body());

        assertThat(duplicateResponse.statusCode()).isEqualTo(200);
        assertThat(duplicate.get("id").asText()).isEqualTo(original.get("id").asText());
        assertThat(duplicate.get("supportCount").asInt()).isEqualTo(2);
        assertThat(duplicate.get("message").asText())
                .isEqualTo("Existing report found - your support has been added");
        verify(storageService, times(1)).uploadImage(any(MultipartFile.class));
    }

    @Test
    @DisplayName("Geo query completes under 200ms for 1000 seeded reports")
    void geoQuery_withOneThousandReports_completesUnder200ms() {
        List<Report> reports = new ArrayList<>();
        UUID citizenId = UUID.randomUUID();

        for (int i = 0; i < 1000; i++) {
            Report report = new Report();
            report.setCitizenId(citizenId);
            report.setImageUrl("https://cdn.snapfix.test/seed-" + i + ".jpg");
            report.setDescription("Seed report " + i);
            report.setCategory(Category.ROAD_DAMAGE);
            report.setLocation(geometryFactory.createPoint(new Coordinate(72.0 + (i * 0.00001), 19.0 + (i * 0.00001))));
            report.setStatus(ReportStatus.CREATED);
            report.setSupportCount(1);
            report.setCreatedAt(Instant.now());
            reports.add(report);
        }

        reportRepository.saveAll(reports);
        reportRepository.findNearbyReports(19.0, 72.0, 5000);

        Instant start = Instant.now();
        List<Report> found = reportRepository.findNearbyReports(19.0, 72.0, 5000);
        long elapsedMillis = Duration.between(start, Instant.now()).toMillis();

        assertThat(found).hasSize(1000);
        assertThat(elapsedMillis).isLessThan(200);
    }

    private String registerAndLogin(String email, String role) throws Exception {
        String password = "Password123!";
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s",
                  "name": "Report Test User"
                }
                """.formatted(email, password, role);
        HttpResponse<String> register = postJson("/auth/register", registerBody, null);
        assertThat(register.statusCode()).isEqualTo(200);

        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        HttpResponse<String> login = postJson("/auth/login", loginBody, null);
        assertThat(login.statusCode()).isEqualTo(200);

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

    private HttpResponse<String> post(String path, String accessToken) throws Exception {
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
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
