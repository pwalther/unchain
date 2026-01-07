package ch.redmoon.unchain.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

@Getter
@Builder
public class UnchainContext {
    private final String userId;
    private final String sessionId;
    private final String environment;
    @Singular
    private final Map<String, String> properties;

    public String getProperty(String name) {
        if (properties == null)
            return null;
        return properties.get(name);
    }
}
