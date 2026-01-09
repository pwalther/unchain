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
class ProjectsControllerIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private ProjectRepository projectRepository;
        @Autowired
        private FeatureRepository featureRepository;
        @Autowired
        private FeatureStrategyRepository featureStrategyRepository;
        @Autowired
        private EnvironmentRepository environmentRepository;

        @BeforeEach
        void setUp() {
                RestAssured.port = port;
                featureStrategyRepository.deleteAll();
                featureRepository.deleteAll();
                environmentRepository.deleteAll();
                projectRepository.deleteAll();
        }

        @Test
        void testProjectLifecycle() {
                // Create
                String projectJson = "{ \"id\": \"test-proj-1\", \"name\": \"Test Project 1\", \"description\": \"Description 1\" }";

                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(projectJson)
                                .when()
                                .post("/projects")
                                .then()
                                .statusCode(201)
                                .body("id", equalTo("test-proj-1"))
                                .body("name", equalTo("Test Project 1"));

                // Get
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/test-proj-1")
                                .then()
                                .statusCode(200)
                                .body("id", equalTo("test-proj-1"));

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects")
                                .then()
                                .statusCode(200)
                                .body("projects", hasSize(1))
                                .body("projects[0].id", equalTo("test-proj-1"));

                // Update
                String updateJson = "{ \"name\": \"Updated Name\" }";
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(updateJson)
                                .when()
                                .put("/projects/test-proj-1")
                                .then()
                                .statusCode(200);

                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/test-proj-1")
                                .then()
                                .statusCode(200)
                                .body("name", equalTo("Updated Name"));

                // Delete
                given()
                                .auth().oauth2("anything")
                                .when()
                                .delete("/projects/test-proj-1")
                                .then()
                                .statusCode(200);

                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/test-proj-1")
                                .then()
                                .statusCode(404);
        }
}
