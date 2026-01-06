package ch.redmoon.unchain;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ch.redmoon.unchain.repository.EnvironmentRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvironmentsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        environmentRepository.deleteAll();
    }

    @Test
    void testEnvironmentLifecycle() {
        String envJson = "{ \"name\": \"dev\", \"type\": \"development\", \"enabled\": true, \"sortOrder\": 1, \"requiredApprovals\": 0 }";

        // Create
        given()
                .contentType(ContentType.JSON)
                .body(envJson)
                .when()
                .post("/environments")
                .then()
                .statusCode(201)
                .body("name", equalTo("dev"));

        // Get
        given()
                .when()
                .get("/environments/dev")
                .then()
                .statusCode(200)
                .body("name", equalTo("dev"));

        // List
        given()
                .when()
                .get("/environments")
                .then()
                .statusCode(200)
                .body("environments", hasSize(1));

        // Update - check API path in controller or OpenAPI
        String updateJson = "{ \"type\": \"production\" }";
        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .when()
                .put("/api/admin/environments/update/dev")
                .then()
                .statusCode(200)
                .body("type", equalTo("production"));

        // Delete
        given()
                .when()
                .delete("/environments/dev")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/environments/dev")
                .then()
                .statusCode(404);
    }
}
