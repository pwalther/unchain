package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import ch.redmoon.unchain.client.model.Constraint;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintEvaluatorTest {

    @Test
    void evaluate_EmptyConstraints_ReturnsTrue() {
        assertTrue(ConstraintEvaluator.evaluate(Collections.emptyList(), UnchainContext.builder().build()));
        assertTrue(ConstraintEvaluator.evaluate((List<Constraint>) null, UnchainContext.builder().build()));
    }

    @Test
    void evaluate_IN_Success() {
        Constraint c = Constraint.builder()
                .contextName("region")
                .operator(Constraint.Operator.IN)
                .values(List.of("eu-west-1", "us-east-1"))
                .build();

        UnchainContext ctx = UnchainContext.builder().property("region", "eu-west-1").build();
        assertTrue(ConstraintEvaluator.evaluate(c, ctx));
    }

    @Test
    void evaluate_IN_Fail() {
        Constraint c = Constraint.builder()
                .contextName("region")
                .operator(Constraint.Operator.IN)
                .values(List.of("eu-west-1", "us-east-1"))
                .build();

        UnchainContext ctx = UnchainContext.builder().property("region", "ap-northeast-1").build();
        assertFalse(ConstraintEvaluator.evaluate(c, ctx));
    }

    @Test
    void evaluate_IN_CaseInsensitive() {
        Constraint c = Constraint.builder()
                .contextName("region")
                .operator(Constraint.Operator.IN)
                .values(List.of("EU-WEST-1"))
                .caseInsensitive(true)
                .build();

        UnchainContext ctx = UnchainContext.builder().property("region", "eu-west-1").build();
        assertTrue(ConstraintEvaluator.evaluate(c, ctx));
    }

    @Test
    void evaluate_NOT_IN_Success() {
        Constraint c = Constraint.builder()
                .contextName("region")
                .operator(Constraint.Operator.NOT_IN)
                .values(List.of("eu-west-1"))
                .build();

        UnchainContext ctx = UnchainContext.builder().property("region", "us-east-1").build();
        assertTrue(ConstraintEvaluator.evaluate(c, ctx));
    }

    @Test
    void evaluate_NumericMatches() {
        Constraint c = Constraint.builder()
                .contextName("score")
                .operator(Constraint.Operator.NUM_GT)
                .values(List.of("10"))
                .build();

        assertTrue(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("score", "11").build()));
        assertFalse(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("score", "10").build()));
        assertFalse(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("score", "9").build()));
    }

    @Test
    void evaluate_SemVer() {
        Constraint c = Constraint.builder()
                .contextName("ver")
                .operator(Constraint.Operator.SEMVER_GT)
                .values(List.of("1.2.3"))
                .build();

        assertTrue(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("ver", "1.2.4").build()));
        assertTrue(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("ver", "1.3.0").build()));
        assertTrue(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("ver", "2.0.0").build()));

        assertFalse(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("ver", "1.2.3").build()));
        assertFalse(ConstraintEvaluator.evaluate(c, UnchainContext.builder().property("ver", "1.2.2").build()));
    }

    @Test
    void evaluate_Date() {
        Constraint c = Constraint.builder()
                .contextName("currentTime")
                .operator(Constraint.Operator.DATE_AFTER)
                .values(List.of("2024-01-01T00:00:00Z"))
                .build();

        assertTrue(ConstraintEvaluator.evaluate(c,
                UnchainContext.builder().property("currentTime", "2024-02-01T00:00:00Z").build()));
        assertFalse(ConstraintEvaluator.evaluate(c,
                UnchainContext.builder().property("currentTime", "2023-12-31T23:59:59Z").build()));
    }

    @Test
    void evaluate_StandardContextFields() {
        // userId
        Constraint cUser = Constraint.builder()
                .contextName("userId")
                .operator(Constraint.Operator.IN)
                .values(List.of("123"))
                .build();
        assertTrue(ConstraintEvaluator.evaluate(cUser, UnchainContext.builder().userId("123").build()));

        // sessionId
        Constraint cSession = Constraint.builder()
                .contextName("sessionId")
                .operator(Constraint.Operator.IN)
                .values(List.of("abc"))
                .build();
        assertTrue(ConstraintEvaluator.evaluate(cSession, UnchainContext.builder().sessionId("abc").build()));

        // environment
        Constraint cEnv = Constraint.builder()
                .contextName("environment")
                .operator(Constraint.Operator.IN)
                .values(List.of("prod"))
                .build();
        assertTrue(ConstraintEvaluator.evaluate(cEnv, UnchainContext.builder().environment("prod").build()));
    }
}
