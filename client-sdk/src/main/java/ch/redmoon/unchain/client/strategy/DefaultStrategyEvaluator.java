package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import java.util.Map;

public class DefaultStrategyEvaluator implements StrategyEvaluator {
    @Override
    public String getName() {
        return "default";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters, UnchainContext context) {
        return true;
    }
}
