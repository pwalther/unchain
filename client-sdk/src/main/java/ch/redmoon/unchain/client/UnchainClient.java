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
import java.util.stream.Collectors;

public class UnchainClient {
    private final UnchainConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, StrategyEvaluator> evaluators = new HashMap<>();
    private final Map<String, Feature> featureCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private static final Logger log = LoggerFactory.getLogger(UnchainClient.class);

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
                String url = config.getApiUrl() + "/projects/" + projectId + "/features";
                String token = config.getTokenSupplier() != null ? config.getTokenSupplier().get() : null;

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
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
                        }
                        log.debug("Refreshed features for project: {}", projectId);
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
        return isEnabled(config.getProjects().get(0), featureName, config.getEnvironment(), context);
    }

    public boolean isEnabled(String projectId, String featureName, String environment, UnchainContext context) {
        Feature feature = featureCache.get(projectId + ":" + featureName);
        if (feature == null) {
            return false;
        }

        Optional<FeatureEnvironment> envOpt = feature.getEnvironments().stream()
                .filter(e -> e.getName().equals(environment))
                .findFirst();

        if (envOpt.isEmpty() || !envOpt.get().isEnabled()) {
            return false;
        }

        FeatureEnvironment env = envOpt.get();
        if (env.getStrategies() == null || env.getStrategies().isEmpty()) {
            // If enabled but no strategies, we treat it as always ON
            return true;
        }

        for (Strategy strategy : env.getStrategies()) {
            StrategyEvaluator evaluator = evaluators.get(strategy.getName());
            if (evaluator != null && evaluator.isEnabled(getParametersMap(strategy), context)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, Boolean> getAllFeaturesEnabled(UnchainContext context) {
        String projectId = config.getProjects().get(0);
        return getAllFeaturesEnabled(projectId, config.getEnvironment(), context);
    }

    public Map<String, Boolean> getAllFeaturesEnabled(String projectId, String environment, UnchainContext context) {
        String prefix = projectId + ":";
        return featureCache.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(prefix.length()),
                        e -> isEnabled(projectId, e.getKey().substring(prefix.length()), environment, context)));
    }

    public Set<String> getFeatureNames() {
        return getFeatureNames(config.getProjects().get(0));
    }

    public Set<String> getFeatureNames(String projectId) {
        String prefix = projectId + ":";
        return featureCache.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> k.substring(prefix.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isFeaturePresent(String projectId, String featureName) {
        return featureCache.containsKey(projectId + ":" + featureName);
    }

    public Feature getFeature(String featureName) {
        return getFeature(config.getProjects().get(0), featureName);
    }

    public Feature getFeature(String projectId, String featureName) {
        return featureCache.get(projectId + ":" + featureName);
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
        if (totalWeight == 0)
            return null;

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

        int hash = Math.abs((featureName + ":" + stickyValue).hashCode()) % totalWeight;
        int currentWeight = 0;
        for (Variant variant : variants) {
            currentWeight += variant.getWeight();
            if (hash < currentWeight) {
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
}
