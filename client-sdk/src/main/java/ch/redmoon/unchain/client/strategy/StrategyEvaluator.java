package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import java.util.Map;

public interface StrategyEvaluator {
    String getName();
    boolean isEnabled(Map<String, String> parameters, UnchainContext context);
}
