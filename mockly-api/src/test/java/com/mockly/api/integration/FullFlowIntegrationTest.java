package com.mockly.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mockly.core.dto.artifact.CompleteUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadRequest;
import com.mockly.core.dto.auth.RegisterRequest;
import com.mockly.core.dto.auth.TokenResponse;
import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.dto.session.CreateSessionRequest;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.Report;
import com.mockly.data.enums.ArtifactType;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.ReportRepository;
import com.mockly.data.repository.SessionRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full flow integration test covering:
 * 1. Create session
 * 2. Join session
 * 3. Upload artifact (request-upload → PUT → complete)
 * 4. Trigger report
 * 5. Mock ML service response
 * 6. Assert report saved to DB
 */


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Full Flow Integration Test")
@TestPropertySource(properties = {
        "spring.security.enabled=false"
})
class FullFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("mockly_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(
            DockerImageName.parse("minio/minio:latest")
    )
            .withExposedPorts(9000, 9001)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data", "--console-address", ":9001")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
    )
            .withExposedPorts(6379);

    private WireMockServer mlServiceMock;
    private int mlServicePort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private ReportRepository reportRepository;

    @LocalServerPort
    private int serverPort;

    private String baseUrl;
    private String candidateToken;
    private String interviewerToken;
    private UUID candidateId;
    private UUID interviewerId;
    private UUID sessionId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);


        registry.add("minio.endpoint", () -> 
                String.format("http://%s:%d", minio.getHost(), minio.getMappedPort(9000)));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket-name", () -> "mockly-artifacts");


        registry.add("ml.service.url", () -> "http://localhost:8089");

        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-to-work-properly-in-testing");

        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + serverPort;

        mlServicePort = 8089;
        mlServiceMock = new WireMockServer(mlServicePort);
        mlServiceMock.start();

        // Setup ML service mock response
        String mockResponse = "{\n" +
                "    \"metrics\": {\n" +
                "        \"score\": 85.5,\n" +
                "        \"clarity\": 8.2,\n" +
                "        \"confidence\": 7.8,\n" +
                "        \"pace\": 6.5,\n" +
                "        \"articulation\": 8.0,\n" +
                "        \"engagement\": 7.5,\n" +
                "        \"professionalism\": 8.3,\n" +
                "        \"technical_accuracy\": 7.9\n" +
                "    },\n" +
                "    \"summary\": \"The candidate demonstrated strong communication skills with a clarity score of 8.2/10. The interview showed good engagement (7.5/10) and professional demeanor (8.3/10). Technical accuracy was solid at 7.9/10. Overall performance score: 85.5/100.\",\n" +
                "    \"recommendations\": \"1. Continue practicing technical explanations to improve clarity\\n2. Work on maintaining consistent pace throughout the interview\\n3. Consider adding more specific examples to support technical claims\\n4. Practice active listening and responding to interviewer questions more directly\",\n" +
                "    \"transcript\": {\n" +
                "        \"full_text\": \"This is a mock transcript of the interview.\",\n" +
                "        \"word_count\": 150,\n" +
                "        \"duration_seconds\": 300\n" +
                "    }\n" +
                "}";

        mlServiceMock.stubFor(post(urlEqualTo("/api/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));


        initializeMinIO();


        registerUsers();
    }

    @AfterEach
    void tearDown() {
        if (mlServiceMock != null) {
            mlServiceMock.stop();
        }
    }

    private void initializeMinIO() {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(String.format("http://%s:%d", minio.getHost(), minio.getMappedPort(9000)))
                    .credentials("minioadmin", "minioadmin")
                    .build();

            boolean exists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket("mockly-artifacts")
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder()
                                .bucket("mockly-artifacts")
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO", e);
        }
    }

    private void registerUsers() {
        RegisterRequest candidateRequest = new RegisterRequest(
                "candidate@test.com",
                "password123",
                "Candidate",
                "User",
                Profile.ProfileRole.CANDIDATE
        );

        try {
            ResponseEntity<TokenResponse> candidateResponse = restTemplate.postForEntity(
                    "/api/auth/register",
                    candidateRequest,
                    TokenResponse.class
            );

            System.out.println("=== CANDIDATE REGISTRATION ===");
            System.out.println("Status Code: " + candidateResponse.getStatusCode());
            System.out.println("Status Code Value: " + candidateResponse.getStatusCode().value());
            System.out.println("Response Body: " + candidateResponse.getBody());
            System.out.println("Headers: " + candidateResponse.getHeaders());

            // Проверка на null перед использованием
            if (candidateResponse.getBody() == null) {
                throw new AssertionError("Response body is null!");
            }

            assertThat(candidateResponse.getStatusCode().is2xxSuccessful()).isTrue();
            candidateToken = candidateResponse.getBody().accessToken();
            candidateId = candidateResponse.getBody().userId();

            System.out.println("Candidate Token: " + candidateToken);
            System.out.println("Candidate ID: " + candidateId);

        } catch (Exception e) {
            System.err.println("ERROR during candidate registration:");
            e.printStackTrace();
            throw e;
        }

        // Register interviewer (аналогично)
        RegisterRequest interviewerRequest = new RegisterRequest(
                "interviewer@test.com",
                "password123",
                "Interviewer",
                "User",
                Profile.ProfileRole.INTERVIEWER
        );

        try {
            ResponseEntity<TokenResponse> interviewerResponse = restTemplate.postForEntity(
                    "/api/auth/register",
                    interviewerRequest,
                    TokenResponse.class
            );

            System.out.println("=== INTERVIEWER REGISTRATION ===");
            System.out.println("Status Code: " + interviewerResponse.getStatusCode());
            System.out.println("Response Body: " + interviewerResponse.getBody());

            if (interviewerResponse.getBody() == null) {
                throw new AssertionError("Response body is null!");
            }

            assertThat(interviewerResponse.getStatusCode().is2xxSuccessful()).isTrue();
            interviewerToken = interviewerResponse.getBody().accessToken();
            interviewerId = interviewerResponse.getBody().userId();

            System.out.println("Interviewer Token: " + interviewerToken);
            System.out.println("Interviewer ID: " + interviewerId);

        } catch (Exception e) {
            System.err.println("ERROR during interviewer registration:");
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Full flow: Create session → Join → Upload artifact → Trigger report → Verify report")
    void testFullFlow() throws Exception {
        // Step 1: Create session
        CreateSessionRequest createRequest = new CreateSessionRequest(
                interviewerId,
                null
        );

        ResponseEntity<SessionResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/sessions",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, createHeaders(candidateToken)),
                SessionResponse.class
        );

        assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
        SessionResponse session = createResponse.getBody();
        assertThat(session).isNotNull();
        sessionId = session.id();
        assertThat(sessionId).isNotNull();

        // Step 2: Join session
        ResponseEntity<SessionResponse> joinResponse = restTemplate.exchange(
                baseUrl + "/api/sessions/" + sessionId + "/join",
                HttpMethod.POST,
                new HttpEntity<>(null, createHeaders(interviewerToken)),
                SessionResponse.class
        );

        assertThat(joinResponse.getStatusCode().is2xxSuccessful()).isTrue();
        SessionResponse joinedSession = joinResponse.getBody();
        assertThat(joinedSession).isNotNull();

        // Step 3: Upload artifact
        // 3a. Request upload URL
        RequestUploadRequest uploadRequest = new RequestUploadRequest(
                ArtifactType.AUDIO_MIXED,
                "test-audio.mp3",
                1024L,
                "audio/mpeg"
        );

        ResponseEntity<Map> uploadUrlResponse = restTemplate.exchange(
                baseUrl + "/api/sessions/" + sessionId + "/artifacts/request-upload",
                HttpMethod.POST,
                new HttpEntity<>(uploadRequest, createHeaders(candidateToken)),
                Map.class
        );

        assertThat(uploadUrlResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> uploadResponse = uploadUrlResponse.getBody();
        assertThat(uploadResponse).isNotNull();
        String uploadUrl = (String) uploadResponse.get("uploadUrl");
        String artifactId = (String) uploadResponse.get("artifactId");
        assertThat(uploadUrl).isNotNull();
        assertThat(artifactId).isNotNull();

        // 3b. Upload file to MinIO (simulate with small file)
        byte[] fileContent = new byte[1024];
        java.util.Arrays.fill(fileContent, (byte) 1);

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        uploadHeaders.setContentLength(1024L);

        ResponseEntity<String> putResponse = restTemplate.exchange(
                uploadUrl,
                HttpMethod.PUT,
                new HttpEntity<>(fileContent, uploadHeaders),
                String.class
        );

        assertThat(putResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // 3c. Complete upload
        CompleteUploadRequest completeRequest = new CompleteUploadRequest(
                1024L,
                60
        );

        ResponseEntity<Map> completeResponse = restTemplate.exchange(
                baseUrl + "/api/sessions/" + sessionId + "/artifacts/" + artifactId + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(completeRequest, createHeaders(candidateToken)),
                Map.class
        );

        assertThat(completeResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify artifact saved
        Artifact artifact = artifactRepository.findById(UUID.fromString(artifactId)).orElse(null);
        assertThat(artifact).isNotNull();
        assertThat(artifact.getType()).isEqualTo(ArtifactType.AUDIO_MIXED);
        assertThat(artifact.getSizeBytes()).isEqualTo(1024L);

        // Step 4: Trigger report (for AUDIO_MIXED, this should happen automatically)
        // But we can also trigger manually
        ResponseEntity<ReportResponse> triggerResponse = restTemplate.exchange(
                baseUrl + "/api/sessions/" + sessionId + "/report/trigger",
                HttpMethod.POST,
                new HttpEntity<>(null, createHeaders(candidateToken)),
                ReportResponse.class
        );

        assertThat(triggerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        ReportResponse report = triggerResponse.getBody();
        assertThat(report).isNotNull();
        assertThat(report.status()).isIn(Report.ReportStatus.PENDING, Report.ReportStatus.PROCESSING);

        // Step 5: Wait for ML service to process and report to be ready
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    Report dbReport = reportRepository.findBySessionId(sessionId).orElse(null);
                    return dbReport != null && dbReport.getStatus() == Report.ReportStatus.READY;
                });

        // Step 6: Assert report saved to DB with ML service response
        Report savedReport = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new AssertionError("Report not found in database"));

        assertThat(savedReport.getStatus()).isEqualTo(Report.ReportStatus.READY);
        assertThat(savedReport.getMetrics()).isNotNull();
        assertThat(savedReport.getSummary()).isNotNull();
        assertThat(savedReport.getRecommendations()).isNotNull();
        assertThat(savedReport.getErrorMessage()).isNull();

        // Verify metrics contain expected fields
        Map<String, Object> metrics = savedReport.getMetrics();
        assertThat(metrics).containsKey("score");
        assertThat(metrics).containsKey("clarity");
        assertThat(metrics).containsKey("confidence");

        assertThat(savedReport.getSummary()).isNotBlank();
        assertThat(savedReport.getRecommendations()).isNotBlank();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = createHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}

