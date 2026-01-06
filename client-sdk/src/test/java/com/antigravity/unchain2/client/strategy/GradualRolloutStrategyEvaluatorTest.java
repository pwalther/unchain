package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.Assertions.*;

public class GradualRolloutStrategyEvaluatorTest {

    private final GradualRolloutStrategyEvaluator evaluator = new GradualRolloutStrategyEvaluator();

    @Test
    public void shouldBeEnabledWhenPercentageIs100() {
        Map<String, String> params = new HashMap<>();
        params.put("percentage", "100");

        UnchainContext context = UnchainContext.builder()
                .userId("user1")
                .build();

        assertTrue(evaluator.isEnabled(params, context));
    }

    @Test
    public void shouldBeDisabledWhenPercentageIs0() {
        Map<String, String> params = new HashMap<>();
        params.put("percentage", "0");

        UnchainContext context = UnchainContext.builder()
                .userId("user1")
                .build();

        assertFalse(evaluator.isEnabled(params, context));
    }

    @Test
    public void shouldBeDeterministic() {
        Map<String, String> params = new HashMap<>();
        params.put("percentage", "50");
        params.put("stickiness", "userId");
        params.put("groupId", "feature1");

        UnchainContext context = UnchainContext.builder()
                .userId("user123")
                .build();

        boolean firstResult = evaluator.isEnabled(params, context);
        for (int i = 0; i < 100; i++) {
            assertEquals(firstResult, evaluator.isEnabled(params, context));
        }
    }

    @Test
    public void shouldDistributeUsers() {
        Map<String, String> params = new HashMap<>();
        params.put("percentage", "50");
        params.put("stickiness", "userId");
        params.put("groupId", "test-feature");

        int enabledCount = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            UnchainContext context = UnchainContext.builder()
                    .userId("user" + i)
                    .build();
            if (evaluator.isEnabled(params, context)) {
                enabledCount++;
            }
        }

        // With 50%, we expect roughly 500 enabled.
        // Allowing some variance due to hash distribution, but it should be close.
        assertTrue(enabledCount > 400 && enabledCount < 600, "Count was: " + enabledCount);
    }
}
