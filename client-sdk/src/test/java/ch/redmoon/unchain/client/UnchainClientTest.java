package ch.redmoon.unchain.client;

import ch.redmoon.unchain.client.model.Feature;
import ch.redmoon.unchain.client.model.FeatureEnvironment;
import ch.redmoon.unchain.client.model.Strategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UnchainClientTest {

    private UnchainClient client;
    private final String ENV = "production";

    @BeforeEach
    public void setup() {
        UnchainConfig config = UnchainConfig.builder()
                .apiUrl("http://localhost:8080")
                .tokenSupplier(() -> "test-token")
                .environment(ENV)
                .projects(List.of("default"))
                .build();
        client = new UnchainClient(config);
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    public void shouldBeDisabledIfFeatureMissing() {
        UnchainContext context = UnchainContext.builder().userId("user1").build();
        assertFalse(client.isEnabled("missing-feature", context));
    }

    @Test
    public void shouldEvaluateDefaultStrategy() {
        Feature f = new Feature();
        f.setName("test-feature");

        FeatureEnvironment fe = new FeatureEnvironment();
        fe.setName(ENV);
        fe.setEnabled(true);

        Strategy s = new Strategy();
        s.setName("default");
        fe.setStrategies(List.of(s));

        f.setEnvironments(List.of(fe));
        client.addFeature(f);

        UnchainContext context = UnchainContext.builder().userId("user1").build();
        assertTrue(client.isEnabled("test-feature", context));
    }

    @Test
    public void shouldBeDisabledIfEnvDisabled() {
        Feature f = new Feature();
        f.setName("disabled-feature");

        FeatureEnvironment fe = new FeatureEnvironment();
        fe.setName(ENV);
        fe.setEnabled(false);

        Strategy s = new Strategy();
        s.setName("default");
        fe.setStrategies(List.of(s));

        f.setEnvironments(List.of(fe));
        client.addFeature(f);

        UnchainContext context = UnchainContext.builder().userId("user1").build();
        assertFalse(client.isEnabled("disabled-feature", context));
    }

    @Test
    public void shouldReturnVariant() {
        Feature f = new Feature();
        f.setName("variant-feature");

        FeatureEnvironment fe = new FeatureEnvironment();
        fe.setName(ENV);
        fe.setEnabled(true);

        Strategy s = new Strategy();
        s.setName("default");
        fe.setStrategies(List.of(s));

        f.setEnvironments(List.of(fe));

        ch.redmoon.unchain.client.model.Variant v1 = new ch.redmoon.unchain.client.model.Variant();
        v1.setName("control");
        v1.setWeight(1000); // 100% control
        f.setVariants(List.of(v1));

        client.addFeature(f);

        UnchainContext context = UnchainContext.builder().userId("user1").build();
        ch.redmoon.unchain.client.model.Variant selected = client.getVariant("variant-feature", context);

        assertNotNull(selected);
        assertEquals("control", selected.getName());
    }
}
