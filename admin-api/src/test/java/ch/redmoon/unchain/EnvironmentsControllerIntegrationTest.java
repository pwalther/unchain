package ch.redmoon.unchain;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ch.redmoon.unchain.repository.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("demo")
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class EnvironmentsControllerIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private EnvironmentRepository environmentRepository;
        @Autowired
        private FeatureStrategyRepository featureStrategyRepository;
        @Autowired
        private FeatureRepository featureRepository;
        @Autowired
        private ProjectRepository projectRepository;

        @BeforeEach
        void setUp() {
                RestAssured.port = port;
                featureStrategyRepository.deleteAll();
                featureRepository.deleteAll();
                environmentRepository.deleteAll();
                projectRepository.deleteAll();
        }

        @Test
        void testEnvironmentLifecycle() {
                String envJson = "{ \"name\": \"dev\", \"type\": \"development\", \"enabled\": true, \"sortOrder\": 1, \"requiredApprovals\": 0 }";

                // Create
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(envJson)
                                .when()
                                .post("/environments")
                                .then()
                                .statusCode(201)
                                .body("name", equalTo("dev"));

                // Get
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/environments/dev")
                                .then()
                                .statusCode(200)
                                .body("name", equalTo("dev"));

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/environments")
                                .then()
                                .statusCode(200)
                                .body("environments", hasSize(1));

                // Update - check API path in controller or OpenAPI
                String updateJson = "{ \"type\": \"production\" }";
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(updateJson)
                                .when()
                                .put("/environments/update/dev")
                                .then()
                                .statusCode(200)
                                .body("type", equalTo("production"));

                // Delete
                given()
                                .auth().oauth2("anything")
                                .when()
                                .delete("/environments/dev")
                                .then()
                                .statusCode(200);

                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/environments/dev")
                                .then()
                                .statusCode(404);
        }
}
