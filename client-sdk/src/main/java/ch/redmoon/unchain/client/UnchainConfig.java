package ch.redmoon.unchain.client;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Builder
public class UnchainConfig {
    private final String apiUrl;
    private final Supplier<String> tokenSupplier;
    private final String environment;
    private final List<String> projects;
    @Builder.Default
    private final long refreshIntervalSeconds = 120;
    @Builder.Default
    private boolean waitforInit = false;
    @Builder.Default
    private long initWaitTimeSeconds = 5;
    @Builder.Default
    private boolean sseEnabled = false;
}
