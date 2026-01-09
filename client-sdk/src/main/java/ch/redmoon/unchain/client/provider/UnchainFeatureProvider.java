package ch.redmoon.unchain.client.provider;

import ch.redmoon.unchain.client.UnchainClient;
import ch.redmoon.unchain.client.UnchainContext;
import ch.redmoon.unchain.client.model.Variant;
import ch.redmoon.unchain.client.model.VariantPayload;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.GeneralError;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class UnchainFeatureProvider implements FeatureProvider {

    private final UnchainClient unchainClient;

    @Override
    public Metadata getMetadata() {
        return () -> "UnchainFeatureProvider";
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue, EvaluationContext ctx) {
        boolean value = unchainClient.isEnabled(key, mapContext(ctx));
        return ProviderEvaluation.<Boolean>builder()
                .value(value)
                .reason(Reason.TARGETING_MATCH.toString())
                .build();
    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue, EvaluationContext ctx) {
        Variant variant = unchainClient.getVariant(key, mapContext(ctx));
        if (variant == null) {
            return ProviderEvaluation.<String>builder()
                    .value(defaultValue)
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }

        String value = defaultValue;
        if (variant.getPayload() != null && "string".equalsIgnoreCase(variant.getPayload().getType())) {
            value = variant.getPayload().getValue();
        } else if (variant.getPayload() == null) {
            // Using variant name as string value if no payload
            value = variant.getName();
        }

        return ProviderEvaluation.<String>builder()
                .value(value)
                .variant(variant.getName())
                .reason(Reason.TARGETING_MATCH.toString())
                .build();
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue, EvaluationContext ctx) {
        Variant variant = unchainClient.getVariant(key, mapContext(ctx));
        if (variant == null || variant.getPayload() == null) {
            return ProviderEvaluation.<Integer>builder()
                    .value(defaultValue)
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }

        try {
            int value = Integer.parseInt(variant.getPayload().getValue());
            return ProviderEvaluation.<Integer>builder()
                    .value(value)
                    .variant(variant.getName())
                    .reason(Reason.TARGETING_MATCH.toString())
                    .build();
        } catch (NumberFormatException e) {
            throw new GeneralError("Failed to parse integer from payload: " + variant.getPayload().getValue());
        }
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue, EvaluationContext ctx) {
        Variant variant = unchainClient.getVariant(key, mapContext(ctx));
        if (variant == null || variant.getPayload() == null) {
            return ProviderEvaluation.<Double>builder()
                    .value(defaultValue)
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }

        try {
            double value = Double.parseDouble(variant.getPayload().getValue());
            return ProviderEvaluation.<Double>builder()
                    .value(value)
                    .variant(variant.getName())
                    .reason(Reason.TARGETING_MATCH.toString())
                    .build();
        } catch (NumberFormatException e) {
            throw new GeneralError("Failed to parse double from payload: " + variant.getPayload().getValue());
        }
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue, EvaluationContext ctx) {
        // Not supporting full object binding yet, treating as generic Value from logic
        return ProviderEvaluation.<Value>builder()
                .value(defaultValue)
                .reason(Reason.DEFAULT.toString())
                .build();
    }

    @Override
    public void shutdown() {
        unchainClient.shutdown();
    }

    @Override
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        // Already initialized
    }

    private UnchainContext mapContext(EvaluationContext ctx) {
        UnchainContext.UnchainContextBuilder builder = UnchainContext.builder()
                .userId(ctx.getTargetingKey());

        ctx.asMap().forEach((k, v) -> {
            if (v.isString()) {
                builder.property(k, v.asString());
            } else {
                builder.property(k, v.asObject() != null ? v.asObject().toString() : null);
            }
        });

        return builder.build();
    }
}
