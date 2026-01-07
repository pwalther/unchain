package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import java.util.Map;

public class GradualRolloutStrategyEvaluator implements StrategyEvaluator {
    @Override
    public String getName() {
        return "gradualRollout";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters, UnchainContext context) {
        String percentageStr = parameters.get("percentage");
        if (percentageStr == null)
            return false;

        int percentage = Integer.parseInt(percentageStr);
        if (percentage <= 0)
            return false;
        if (percentage >= 100)
            return true;

        String stickiness = parameters.getOrDefault("stickiness", "userId");
        String groupId = parameters.getOrDefault("groupId", "");

        String stickinessValue = getStickinessValue(stickiness, context);
        if (stickinessValue == null || stickinessValue.isEmpty()) {
            return false;
        }

        String data = groupId + ":" + stickinessValue;
        int hash = Math.abs(com.google.common.hash.Hashing.murmur3_32_fixed()
                .hashString(data, java.nio.charset.StandardCharsets.UTF_8).asInt());
        int normalized = (hash % 100) + 1;

        return normalized <= percentage;
    }

    private String getStickinessValue(String stickiness, UnchainContext context) {
        if ("userId".equals(stickiness))
            return context.getUserId();
        if ("sessionId".equals(stickiness))
            return context.getSessionId();
        return context.getProperties().get(stickiness);
    }
}
