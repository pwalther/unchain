package ch.redmoon.unchain.client;

import ch.redmoon.unchain.client.model.Feature;
import ch.redmoon.unchain.client.model.FeatureEnvironment;
import ch.redmoon.unchain.client.model.FeatureResponse;
import ch.redmoon.unchain.client.model.Strategy;
import ch.redmoon.unchain.client.strategy.DefaultStrategyEvaluator;
import ch.redmoon.unchain.client.strategy.GradualRolloutStrategyEvaluator;
import ch.redmoon.unchain.client.strategy.StrategyEvaluator;
import ch.redmoon.unchain.client.strategy.UserWithIdStrategyEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
            public String getName() { return "flexibleRollout"; }
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "unchain-refresh-scheduler");
            t.setDaemon(true);
            return t;
        });

        this.scheduler.scheduleAtFixedRate(this::refresh, 0, config.getRefreshIntervalSeconds(), TimeUnit.SECONDS);
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
                    FeatureResponse fr = objectMapper.readValue(response.body(), FeatureResponse.class);
                    if (fr.getFeatures() != null) {
                        for (Feature f : fr.getFeatures()) {
                            featureCache.put(f.getName(), f);
                        }
                    }
                }
            } catch (Exception e) {
                // In a real SDK, we might want to log this or throw a custom exception
                e.printStackTrace();
            }
        }
    }

    public boolean isEnabled(String featureName, UnchainContext context) {
        Feature feature = featureCache.get(featureName);
        if (feature == null) {
            return false;
        }

        Optional<FeatureEnvironment> envOpt = feature.getEnvironments().stream()
                .filter(e -> e.getName().equals(config.getEnvironment()))
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
            if (evaluator != null && evaluator.isEnabled(strategy.getParameters(), context)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, Boolean> getAllFeaturesEnabled(UnchainContext context) {
        return featureCache.keySet().stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> isEnabled(name, context)
                ));
    }

    public Set<String> getFeatureNames() {
        return Collections.unmodifiableSet(featureCache.keySet());
    }

    public boolean isFeaturePresent(String featureName) {
        return featureCache.containsKey(featureName);
    }

    public Feature getFeature(String featureName) {
        return featureCache.get(featureName);
    }

    // For testing purposes
    void addFeature(Feature feature) {
        featureCache.put(feature.getName(), feature);
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
}
