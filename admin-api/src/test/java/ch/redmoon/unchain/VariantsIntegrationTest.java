package ch.redmoon.unchain;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ch.redmoon.unchain.repository.*;
import ch.redmoon.unchain.entity.*;

import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class VariantsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private StrategyDefinitionRepository strategyDefinitionRepository;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private FeatureStrategyRepository featureStrategyRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        featureStrategyRepository.deleteAll();
        featureRepository.deleteAll();
        projectRepository.deleteAll();
        environmentRepository.deleteAll();
        strategyDefinitionRepository.deleteAll();

        ProjectEntity project = new ProjectEntity();
        project.setId("default");
        project.setName("Default Project");
        projectRepository.save(project);

        EnvironmentEntity env = new EnvironmentEntity("development", "development", true, 1, 1);
        environmentRepository.save(env);

        StrategyDefinitionEntity strategyDef = new StrategyDefinitionEntity();
        strategyDef.setName("default");
        strategyDefinitionRepository.save(strategyDef);
    }

    @Test
    void testFeatureVariants() {
        String featureJson = """
                {
                    "name": "variant-feature",
                    "type": "release",
                    "variants": [
                        {
                            "name": "control",
                            "weight": 500,
                            "payload": { "type": "string", "value": "control-payload" }
                        },
                        {
                            "name": "test",
                            "weight": 500,
                            "payload": { "type": "string", "value": "test-payload" }
                        }
                    ]
                }
                """;

        // Create feature with variants
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(featureJson)
                .when()
                .post("/projects/default/features")
                .then()
                .statusCode(201);

        // Verify variants are returned in full DTO
        given()
                .auth().oauth2("anything")
                .when()
                .get("/projects/default/features/variant-feature")
                .then()
                .statusCode(200)
                .body("variants", hasSize(2))
                .body("variants[0].name", anyOf(equalTo("control"), equalTo("test")))
                .body("variants[1].payload.value", anyOf(equalTo("control-payload"), equalTo("test-payload")));

        // Update variants
        String updateJson = """
                {
                    "variants": [
                        { "name": "new-variant", "weight": 1000 }
                    ]
                }
                """;

        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(updateJson)
                .when()
                .patch("/projects/default/features/variant-feature")
                .then()
                .statusCode(200);

        given()
                .auth().oauth2("anything")
                .when()
                .get("/projects/default/features/variant-feature")
                .then()
                .statusCode(200)
                .body("variants", hasSize(1))
                .body("variants[0].name", equalTo("new-variant"))
                .body("variants[0].weight", equalTo(1000));
    }

    @Test
    void testStrategyVariants() {
        // Create feature first
        FeatureEntity feature = new FeatureEntity();
        feature.setName("strategy-variant-feature");
        feature.setProject(projectRepository.findById("default").get());
        feature.setType("release");
        feature.setCreatedAt(OffsetDateTime.now());
        featureRepository.save(feature);

        String strategyJson = """
                {
                    "name": "default",
                    "variants": [
                        {
                            "name": "strat-control",
                            "weight": 300,
                            "payload": { "type": "string", "value": "v1" }
                        },
                        {
                            "name": "strat-test",
                            "weight": 700,
                            "payload": { "type": "string", "value": "v2" },
                            "stickiness": "userId"
                        }
                    ]
                }
                """;

        // Add strategy with variants
        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(strategyJson)
                .when()
                .post("/projects/default/features/strategy-variant-feature/environments/development/strategies")
                .then()
                .statusCode(201)
                .body("variants", hasSize(2))
                .body("variants[1].stickiness", equalTo("userId"));

        // Get strategy
        String strategyId = given()
                .auth().oauth2("anything")
                .when()
                .get("/projects/default/features/strategy-variant-feature")
                .then()
                .statusCode(200)
                .extract().path("environments.find { it.name == 'development' }.strategies[0].id");

        // Update strategy variants
        String updateStrategyJson = """
                {
                    "name": "default",
                    "variants": [
                        { "name": "updated-strat-v", "weight": 1000 }
                    ]
                }
                """;

        given()
                .auth().oauth2("anything")
                .contentType(ContentType.JSON)
                .body(updateStrategyJson)
                .when()
                .put("/projects/default/features/strategy-variant-feature/environments/development/strategies/"
                        + strategyId)
                .then()
                .statusCode(200);

        // Verify in full feature DTO
        given()
                .auth().oauth2("anything")
                .when()
                .get("/projects/default/features/strategy-variant-feature")
                .then()
                .statusCode(200)
                .body("environments.find { it.name == 'development' }.strategies[0].variants", hasSize(1))
                .body("environments.find { it.name == 'development' }.strategies[0].variants[0].name",
                        equalTo("updated-strat-v"));
    }
}
