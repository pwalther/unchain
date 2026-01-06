package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import java.util.Arrays;
import java.util.Map;

public class UserWithIdStrategyEvaluator implements StrategyEvaluator {
    @Override
    public String getName() {
        return "userWithId";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters, UnchainContext context) {
        String userIds = parameters.get("userIds");
        if (userIds == null || context.getUserId() == null) {
            return false;
        }
        
        return Arrays.stream(userIds.split(","))
                .map(String::trim)
                .anyMatch(id -> id.equals(context.getUserId()));
    }
}
