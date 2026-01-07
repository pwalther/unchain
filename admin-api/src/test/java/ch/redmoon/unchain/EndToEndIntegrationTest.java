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
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class EndToEndIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private StrategyDefinitionRepository strategyDefinitionRepository;
    @Autowired
    private FeatureStrategyRepository featureStrategyRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // Clean up in reverse order of dependencies
        featureStrategyRepository.deleteAll();
        featureRepository.deleteAll();
        environmentRepository.deleteAll();
        projectRepository.deleteAll();
        strategyDefinitionRepository.deleteAll();
    }

    @Test
    void testEndToEndFlow() {
        // 1. Create a project
        String projectJson = "{ \"id\": \"default\", \"name\": \"Default Project\", \"description\": \"Main project\" }";
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(projectJson)
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        // 2. Add a feature to the previously created project
        String featureJson = "{ \"name\": \"new-feature\", \"type\": \"release\", \"description\": \"A new feature\" }";
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(featureJson)
                .when()
                .post("/projects/default/features")
                .then()
                .statusCode(201);

        // 3. Create an environment
        String envJson = """
                {
                    "name": "production",
                    "type": "production",
                    "enabled": true,
                    "sortOrder": 1
                }
                """;
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(envJson)
                .when()
                .post("/environments")
                .then()
                .statusCode(201);

        // 4. Create a strategy
        String strategyJson = """
                {
                    "name": "default",
                    "description": "Default rollout",
                    "parameters": []
                }
                """;
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(strategyJson)
                .when()
                .post("/strategies")
                .then()
                .statusCode(201);

        // 5. Add an environment to the previously created feature (Enable it)
        given()
                .auth().oauth2("anything")
                .when()
                .post("/projects/default/features/new-feature/environments/production/on")
                .then()
                .statusCode(200);

        // 6. Add the previously created strategy to the feature for the created
        // environment
        String addStrategyJson = """
                {
                    "name": "default",
                    "constraints": [],
                    "parameters": {}
                }
                """;
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(addStrategyJson)
                .when()
                .post("/projects/default/features/new-feature/environments/production/strategies")
                .then()
                .statusCode(201)
                .body("name", equalTo("default"));
    }
}
