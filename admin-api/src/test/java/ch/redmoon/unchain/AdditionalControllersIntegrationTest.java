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
@org.springframework.test.context.ActiveProfiles("demo")
class AdditionalControllersIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TagRepository tagRepository;
        @Autowired
        private ContextFieldRepository contextFieldRepository;
        @Autowired
        private SegmentRepository segmentRepository;
        @Autowired
        private StrategyDefinitionRepository strategyDefinitionRepository;
        @Autowired
        private ProjectRepository projectRepository;
        @Autowired
        private FeatureRepository featureRepository;
        @Autowired
        private EnvironmentRepository environmentRepository;
        @Autowired
        private FeatureStrategyRepository featureStrategyRepository;
        @Autowired
        private TagTypeRepository tagTypeRepository;

        @BeforeEach
        void setUp() {
                RestAssured.port = port;
                tagRepository.deleteAll();
                contextFieldRepository.deleteAll();
                segmentRepository.deleteAll();
                // Clean up dependent entities first
                featureStrategyRepository.deleteAll();
                featureRepository.deleteAll();
                projectRepository.deleteAll();
                environmentRepository.deleteAll();
                strategyDefinitionRepository.deleteAll();

                // Create tag types that tests can reference
                TagTypeEntity simpleType = new TagTypeEntity();
                simpleType.setName("simple");
                simpleType.setDescription("Simple tag type");
                tagTypeRepository.save(simpleType);
        }

        @Test
        void testTags() {
                String tagJson = "{ \"type\": \"simple\", \"value\": \"test-tag\" }";

                // Create
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(tagJson)
                                .when()
                                .post("/tags")
                                .then()
                                .statusCode(201);

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/tags")
                                .then()
                                .statusCode(200)
                                .body("tags", hasSize(1));
        }

        @Test
        void testContextFields() {
                String contextJson = "{ \"name\": \"userId\" }";

                // Create
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(contextJson)
                                .when()
                                .post("/contexts")
                                .then()
                                .statusCode(201);

                // List - returns array directly
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/contexts")
                                .then()
                                .statusCode(200)
                                .body("size()", is(1));

                // Delete
                given()
                                .auth().oauth2("anything")
                                .when()
                                .delete("/contexts/userId")
                                .then()
                                .statusCode(200);
        }

        @Test
        void testSegments() {
                String segmentJson = "{ \"name\": \"beta-users\", \"description\": \"Beta\", \"constraints\": [] }";

                // Create
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(segmentJson)
                                .when()
                                .post("/segments")
                                .then()
                                .statusCode(201);

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/segments")
                                .then()
                                .statusCode(200)
                                .body("segments", hasSize(1));
        }

        @Test
        void testStrategies() {
                String strategyJson = "{ \"name\": \"flexibleRollout\", \"description\": \"Rollout\", \"parameters\": [ { \"name\": \"percentage\", \"type\": \"percentage\", \"required\": true } ] }";

                // Create Definition
                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(strategyJson)
                                .when()
                                .post("/strategies")
                                .then()
                                .statusCode(201);

                // List
                given()
                                .auth().oauth2("anything")
                                .when()
                                .get("/strategies")
                                .then()
                                .statusCode(200)
                                .body("strategies", hasSize(1))
                                .body("strategies[0].name", equalTo("flexibleRollout"))
                                .body("strategies[0].parameters", hasSize(1));
        }

        @Test
        void testStrategyConstraints() {
                // Setup prerequisites
                ProjectEntity project = new ProjectEntity("default", "Default Project", "Description", 100,
                                OffsetDateTime.now(), false, null);
                projectRepository.save(project);

                EnvironmentEntity env = new EnvironmentEntity("development", "development", true, 1, 1);
                environmentRepository.save(env);

                FeatureEntity feature = new FeatureEntity();
                feature.setName("test-feature");
                feature.setProject(project);
                feature.setType("release");
                feature.setCreatedAt(OffsetDateTime.now());
                featureRepository.save(feature);

                StrategyDefinitionEntity strategyDef = new StrategyDefinitionEntity();
                strategyDef.setName("default");
                strategyDefinitionRepository.save(strategyDef);

                // Add strategy with constraints to feature
                String createStrategyJson = """
                                {
                                    "name": "default",
                                    "constraints": [
                                        {
                                            "contextName": "userId",
                                            "operator": "IN",
                                            "values": ["123", "456"],
                                            "caseInsensitive": false,
                                            "inverted": false
                                        }
                                    ]
                                }
                                """;

                given()
                                .auth().oauth2("anything")
                                .contentType(ContentType.JSON)
                                .body(createStrategyJson)
                                .when()
                                .post("/projects/default/features/test-feature/environments/development/strategies")
                                .then()
                                .statusCode(201)
                                .body("constraints", hasSize(1))
                                .body("constraints[0].contextName", equalTo("userId"))
                                .body("constraints[0].operator", equalTo("IN"))
                                .body("constraints[0].values", hasSize(2));
        }
}
