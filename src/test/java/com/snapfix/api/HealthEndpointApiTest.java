// package com.snapfix.api;

// import com.snapfix.common.BaseIntegrationTest;
// import io.restassured.RestAssured;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.web.server.LocalServerPort;

// import static io.restassured.RestAssured.given;
// import static org.hamcrest.Matchers.equalTo;

// public class HealthEndpointApiTest extends BaseIntegrationTest {

//     @LocalServerPort
//     private int port;

//     @BeforeEach
//     void setUp() {
//         RestAssured.port = port;
//     }

//     @Test
//     @DisplayName("Actuator health endpoint is accessible and returns UP")
//     void getHealth_noAuth_returns200AndUp() {
//         // Given / When
//         given()
//                 .when()
//                 .get("/actuator/health")
//         // Then
//                 .then()
//                 .statusCode(200)
//                 .body("status", equalTo("UP"));
//     }
// }
