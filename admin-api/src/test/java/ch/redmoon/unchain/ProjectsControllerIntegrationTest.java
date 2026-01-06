package ch.redmoon.unchain;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ch.redmoon.unchain.repository.ProjectRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        projectRepository.deleteAll();
    }

    @Test
    void testProjectLifecycle() {
        // Create
        String projectJson = "{ \"id\": \"test-proj-1\", \"name\": \"Test Project 1\", \"description\": \"Description 1\" }";

        given()
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
                .when()
                .get("/projects/test-proj-1")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-proj-1"));

        // List
        given()
                .when()
                .get("/projects")
                .then()
                .statusCode(200)
                .body("projects", hasSize(1))
                .body("projects[0].id", equalTo("test-proj-1"));

        // Update
        String updateJson = "{ \"name\": \"Updated Name\" }";
        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .when()
                .put("/projects/test-proj-1")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/projects/test-proj-1")
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Name"));

        // Delete
        given()
                .when()
                .delete("/projects/test-proj-1")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/projects/test-proj-1")
                .then()
                .statusCode(404);
    }
}
