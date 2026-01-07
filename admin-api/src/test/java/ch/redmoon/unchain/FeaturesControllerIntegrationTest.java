package ch.redmoon.unchain;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ch.redmoon.unchain.repository.*;
import ch.redmoon.unchain.entity.ProjectEntity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class FeaturesControllerIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private ProjectRepository projectRepository;
        @Autowired
        private FeatureRepository featureRepository;
        @Autowired
        private FeatureStrategyRepository featureStrategyRepository;

        @BeforeEach
        void setUp() {
                RestAssured.port = port;
                featureStrategyRepository.deleteAll();
                featureRepository.deleteAll(); // Delete features first due to FK
                projectRepository.deleteAll();

                ProjectEntity p = new ProjectEntity();
                p.setId("default");
                p.setName("Default Project");
                projectRepository.save(p);
        }

        @Test
        void testFeatureLifecycle() {
                String featureJson = "{ \"name\": \"feature-A\", \"description\": \"Feature A\", \"type\": \"release\", \"impressionData\": false }";

                // Create
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(featureJson)
                                .when()
                                .post("/projects/default/features")
                                .then()
                                .statusCode(201)
                                .body("name", equalTo("feature-A"));

                // Get
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/default/features/feature-A")
                                .then()
                                .statusCode(200)
                                .body("name", equalTo("feature-A"));

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/default/features")
                                .then()
                                .statusCode(200)
                                .body("features", hasSize(1));

                // Update
                String updateJson = "{ \"description\": \"Updated Desc\" }";
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(updateJson)
                                .when()
                                .patch("/projects/default/features/feature-A")
                                .then()
                                .statusCode(200);

                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/default/features/feature-A")
                                .then()
                                .statusCode(200)
                                .body("description", equalTo("Updated Desc"));

                // Delete
                given()
                                .auth().oauth2("anything")
                                .when()
                                .delete("/projects/default/features/feature-A")
                                .then()
                                .statusCode(200);

                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/projects/default/features/feature-A")
                                .then()
                                .statusCode(404);
        }

        @Test
        void testCreateDuplicateFeature() {
                String featureJson = "{ \"name\": \"unique-feature\", \"type\": \"release\" }";

                // Create first time
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(featureJson)
                                .when()
                                .post("/projects/default/features")
                                .then()
                                .statusCode(201);

                // Create second time - should be 409 Conflict
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(featureJson)
                                .when()
                                .post("/projects/default/features")
                                .then()
                                .statusCode(409);
        }

        @Test
        void testCreateDuplicateFeatureCaseInsensitive() {
                // Create first time
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body("{ \"name\": \"My-Feature\", \"type\": \"release\" }")
                                .when()
                                .post("/projects/default/features")
                                .then()
                                .statusCode(201);

                // Create second time with different case - should be 409 Conflict
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body("{ \"name\": \"my-feature\", \"type\": \"release\" }")
                                .when()
                                .post("/projects/default/features")
                                .then()
                                .statusCode(409);
        }
}
