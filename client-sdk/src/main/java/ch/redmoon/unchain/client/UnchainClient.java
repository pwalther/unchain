package ch.redmoon.unchain.client;

import ch.redmoon.unchain.client.model.*;
import ch.redmoon.unchain.client.strategy.DefaultStrategyEvaluator;
import ch.redmoon.unchain.client.strategy.GradualRolloutStrategyEvaluator;
import ch.redmoon.unchain.client.strategy.StrategyEvaluator;
import ch.redmoon.unchain.client.strategy.UserWithIdStrategyEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

public class UnchainClient {
    private final UnchainConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, StrategyEvaluator> evaluators = new HashMap<>();
    private final Map<String, Feature> featureCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> metricsMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private static final Logger log = LoggerFactory.getLogger(UnchainClient.class);
    private static final String VERSION = loadVersion();

    private static String loadVersion() {
        try (var is = UnchainClient.class.getResourceAsStream("/unchain-client.properties")) {
            Properties props = new Properties();
            if (is != null) {
                props.load(is);
                return props.getProperty("version", "unknown");
            }
        } catch (Exception e) {
            log.warn("Could not load SDK version", e);
        }
        return "unknown";
    }

    public UnchainClient(UnchainConfig config) {
        this(config, HttpClient.newBuilder().build());
    }

    public UnchainClient(UnchainConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        registerEvaluator(new DefaultStrategyEvaluator());
        registerEvaluator(new GradualRolloutStrategyEvaluator());
        registerEvaluator(new UserWithIdStrategyEvaluator());
        // flexibleRollout is often an alias or uses very similar logic
        registerEvaluator(new GradualRolloutStrategyEvaluator() {
            @Override
            public String getName() {
                return "flexibleRollout";
            }
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "unchain-refresh-scheduler");
            t.setDaemon(true);
            return t;
        });

        this.scheduler.scheduleAtFixedRate(this::refresh, 0, config.getRefreshIntervalSeconds(), TimeUnit.SECONDS);
        this.scheduler.scheduleAtFixedRate(this::sendMetrics, 30, 10 * 60, TimeUnit.SECONDS);

        if (config.isWaitforInit()) {
            try {
                // simple wait logic - could be improved with a latch
                Thread.sleep(Math.min(config.getInitWaitTimeSeconds() * 1000, 3000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for initial fetch");
            }
        }

        if (config.isSseEnabled()) {
            startSseConnection();
        }
    }

    private void startSseConnection() {
        new Thread(this::connectSse, "unchain-sse-client").start();
    }

    private void connectSse() {
        while (!scheduler.isShutdown()) {
            for (String projectId : config.getProjects()) {
                try {
                    URI url = URI.create(
                            config.getApiUrl().replaceAll("/$", "") + "/projects/" + projectId + "/features/stream");
                    String token = config.getTokenSupplier() != null ? config.getTokenSupplier().get() : null;

                    log.info("Connecting to SSE stream for project: {}", projectId);
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(url)
                            .header("Accept", "text/event-stream")
                            .header("User-Agent", "unchain-java-client/" + VERSION);

                    if (token != null) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofLines())
                            .thenAccept(response -> {
                                if (response.statusCode() == 200) {
                                    log.info("SSE Connected to project: {}", projectId);
                                    response.body().forEach(line -> {
                                        if (line.startsWith("data:")) {
                                            String data = line.substring(5).trim();
                                            if (!data.isEmpty()) {
                                                try {
                                                    // The SSE stream usually sends the same
                                                    // GetFeaturesByProject200Response structure
                                                    // but wrapped in 'features' property or just the object.
                                                    // FeaturesController sends GetFeaturesByProject200Response which
                                                    // HAS 'features' property.
                                                    FeatureResponse fr = objectMapper.readValue(data,
                                                            FeatureResponse.class);
                                                    if (fr.getFeatures() != null) {
                                                        for (Feature f : fr.getFeatures()) {
                                                            featureCache.put(projectId + ":" + f.getName(), f);
                                                        }
                                                        log.debug("Updated features from SSE for project: {}",
                                                                projectId);
                                                    }
                                                } catch (Exception e) {
                                                    log.error("Failed to parse SSE data", e);
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    log.warn("SSE Connection failed with status: {}", response.statusCode());
                                }
                            })
                            .join(); // Wait for stream to end (disconnect)

                } catch (Exception e) {
                    log.error("SSE Connection error for project {}", projectId, e);
                }
            }

            // Reconnect backoff - 10 seconds
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public UnchainConfig getConfig() {
        return config;
    }

    public void registerEvaluator(StrategyEvaluator evaluator) {
        evaluators.put(evaluator.getName(), evaluator);
    }

    public void refresh() {
        for (String projectId : config.getProjects()) {
            try {
                URI url = URI.create(config.getApiUrl().replaceAll("/$", "") + "/projects/" + projectId + "/features");
                String token = config.getTokenSupplier() != null ? config.getTokenSupplier().get() : null;
                log.debug("Fetching features for project: {} from {}", projectId, url);
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(url)
                        .header("Accept", "application/json")
                        .header("User-Agent", "unchain-java-client/" + VERSION)
                        .GET();

                if (token != null) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                }

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    try {
                        FeatureResponse fr = objectMapper.readValue(response.body(), FeatureResponse.class);
                        if (fr.getFeatures() != null) {
                            for (Feature f : fr.getFeatures()) {
                                featureCache.put(projectId + ":" + f.getName(), f);
                            }
                            log.info("Refreshed {} features for project: {}", fr.getFeatures().size(), projectId);
                        } else {
                            log.debug("No features found for project: {}", projectId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to deserialize feature response for project {}. Body: {}", projectId,
                                response.body(), e);
                    }
                } else {
                    log.error("Failed to fetch features for project {}: Status code {}", projectId,
                            response.statusCode());
                }
            } catch (Exception e) {
                log.error("Unexpected error during feature refresh for project {}", projectId, e);
            }
        }
    }

    public boolean isEnabled(String featureName, UnchainContext context) {
        if (config.getProjects().size() > 1) {
            throw new IllegalStateException("Multiple projects configured, please specify project ID");
        }
        return isEnabled(config.getProjects().get(0), featureName, config.getEnvironment(), context);
    }

    public boolean isEnabled(String projectId, String featureName, String environment, UnchainContext context) {
        Feature feature = featureCache.get(projectId + ":" + featureName);
        if (feature == null) {
            log.trace("Feature not found in cache: {}:{}", projectId, featureName);
            return false;
        }

        if (feature.isImpressionData()) {
            recordMetric(projectId, featureName, environment);
        }

        log.trace("Evaluating feature: {}:{}:{} for context: {}", projectId, featureName, environment, context);

        Optional<FeatureEnvironment> envOpt = feature.getEnvironments().stream()
                .filter(e -> e.getName().equals(environment))
                .findFirst();

        if (envOpt.isEmpty() || !envOpt.get().isEnabled()) {
            log.trace("Feature {}:{} is disabled in environment: {}", projectId, featureName, environment);
            return false;
        }

        FeatureEnvironment env = envOpt.get();
        if (env.getStrategies() == null || env.getStrategies().isEmpty()) {
            // If enabled but no strategies, we treat it as always ON
            return true;
        }

        for (Strategy strategy : env.getStrategies()) {
            StrategyEvaluator evaluator = evaluators.get(strategy.getName());
            if (evaluator != null) {
                boolean enabled = evaluator.isEnabled(getParametersMap(strategy), context);
                log.trace("Strategy {} evaluated to: {}", strategy.getName(), enabled);
                if (enabled) {
                    return true;
                }
            } else {
                log.warn("No evaluator found for strategy: {}", strategy.getName());
            }
        }

        return false;
    }

    // For testing purposes
    void addFeature(Feature feature) {
        addFeature(config.getProjects().get(0), feature);
    }

    void addFeature(String projectId, Feature feature) {
        featureCache.put(projectId + ":" + feature.getName(), feature);
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public Variant getVariant(String featureName, UnchainContext context) {
        return getVariant(config.getProjects().get(0), featureName, config.getEnvironment(), context);
    }

    public Variant getVariant(String projectId, String featureName, String environment, UnchainContext context) {
        Feature feature = featureCache.get(projectId + ":" + featureName);
        if (feature == null) {
            return null;
        }

        // We don't record metric here if it was already recorded in isEnabled,
        // but since we don't know if isEnabled was called, we should ideally
        // have an atomic way. For now, we keep it as is but ensure it's recorded
        // only if feature exists.
        if (feature.isImpressionData()) {
            recordMetric(projectId, featureName, environment);
        }

        Optional<FeatureEnvironment> envOpt = feature.getEnvironments().stream()
                .filter(e -> e.getName().equals(environment))
                .findFirst();

        if (envOpt.isEmpty() || !envOpt.get().isEnabled()) {
            return null;
        }

        FeatureEnvironment env = envOpt.get();
        Strategy matchingStrategy = null;

        if (env.getStrategies() != null && !env.getStrategies().isEmpty()) {
            for (Strategy strategy : env.getStrategies()) {
                StrategyEvaluator evaluator = evaluators.get(strategy.getName());
                if (evaluator != null && evaluator.isEnabled(getParametersMap(strategy), context)) {
                    matchingStrategy = strategy;
                    break;
                }
            }
            if (matchingStrategy == null) {
                return null;
            }
        }

        List<Variant> variants = null;
        if (matchingStrategy != null && matchingStrategy.getVariants() != null
                && !matchingStrategy.getVariants().isEmpty()) {
            variants = matchingStrategy.getVariants();
        } else if (feature.getVariants() != null && !feature.getVariants().isEmpty()) {
            variants = feature.getVariants();
        }

        if (variants == null || variants.isEmpty()) {
            return null;
        }

        // Basic weighted selection logic
        int totalWeight = variants.stream().mapToInt(Variant::getWeight).sum();
        if (totalWeight == 0) {
            log.warn("Total variant weight is 0 for feature: {}", featureName);
            return null;
        }

        // Stickiness logic (simplified)
        String stickyValue = context.getUserId(); // Default stickiness

        // Use stickiness from the first variant as a proxy for the strategy's
        // stickiness configuration
        if (variants.get(0).getStickiness() != null && !variants.get(0).getStickiness().isEmpty()
                && !"default".equals(variants.get(0).getStickiness())) {
            stickyValue = context.getProperty(variants.get(0).getStickiness());
        }

        if (stickyValue == null)
            stickyValue = "anonymous";

        String data = featureName + ":" + stickyValue;
        long hash = com.sangupta.murmur.Murmur3.hash_x86_32(data.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                data.length(), 0);
        int normalized = (int) (hash % totalWeight);

        log.trace("Variant selection for {}: hash={}, stickyValue={}", featureName, normalized, stickyValue);
        int currentWeight = 0;
        for (Variant variant : variants) {
            currentWeight += variant.getWeight();
            if (normalized < currentWeight) {
                log.trace("Selected variant: {} for feature: {}", variant.getName(), featureName);
                return variant;
            }
        }

        return null;
    }

    private Map<String, String> getParametersMap(Strategy strategy) {
        if (strategy.getParameters() == null) {
            return Collections.emptyMap();
        }
        return strategy.getParameters().stream()
                .collect(Collectors.toMap(
                        StrategyParameter::getName,
                        p -> p.getValue() != null ? p.getValue() : "",
                        (v1, v2) -> v1 // Keep first on duplicates
                ));
    }

    private void recordMetric(String projectId, String featureName, String environment) {
        log.trace("Recording metric for feature: {}:{}:{}", projectId, featureName, environment);
        String key = String.format("%s|%s|%s", projectId, featureName, environment);
        metricsMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void sendMetrics() {
        if (metricsMap.isEmpty()) {
            log.trace("No metrics to send");
            return;
        }

        try {
            List<FeatureMetric> metricsList = new ArrayList<>();
            metricsMap.forEach((key, counter) -> {
                int count = counter.getAndSet(0);
                if (count > 0) {
                    String[] parts = key.split("\\|");
                    if (parts.length == 3) {
                        metricsList.add(FeatureMetric.builder()
                                .projectId(parts[0])
                                .featureName(parts[1])
                                .environment(parts[2])
                                .count(count)
                                .timestamp(OffsetDateTime.now())
                                .build());
                    }
                }
            });

            if (metricsList.isEmpty()) {
                return;
            }

            MetricsReportRequest reportRequest = MetricsReportRequest.builder()
                    .metrics(metricsList)
                    .build();

            String url = config.getApiUrl() + "/metrics";
            String token = config.getTokenSupplier() != null ? config.getTokenSupplier().get() : null;

            log.debug("Reporting {} metric buckets to {}", metricsList.size(), url);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "unchain-java-client/" + VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reportRequest)));

            if (token != null) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 202) {
                log.info("Successfully reported {} metric buckets", metricsList.size());
            } else {
                log.error("Failed to report metrics: Status code {}", response.statusCode());
                // Optional: restore metrics on failure? Complex due to concurrent updates.
                // For now, we accept loss on failure as the counts were already reset to 0.
                // Restore counts on failure
                metricsList.forEach(m -> {
                    String key = m.getProjectId() + "|" + m.getFeatureName() + "|" + m.getEnvironment();
                    metricsMap.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(m.getCount());
                });
            }
        } catch (Exception e) {
            log.error("Error sending metrics", e);
        }
    }
}
