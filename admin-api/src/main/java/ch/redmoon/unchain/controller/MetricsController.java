package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.MetricsApi;
import ch.redmoon.unchain.api.model.*;
import ch.redmoon.unchain.entity.FeatureEntity;
import ch.redmoon.unchain.entity.FeatureMetricEntity;
import ch.redmoon.unchain.repository.FeatureMetricRepository;
import ch.redmoon.unchain.repository.FeatureRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MetricsController implements MetricsApi {

    private final FeatureMetricRepository metricsRepository;
    private final FeatureRepository featureRepository;
    private final HttpServletRequest request;

    @Override
    public ResponseEntity<ProjectMetrics> getProjectMetrics(String projectId) {
        ProjectMetrics metrics = new ProjectMetrics();

        // 1. Feature Activity
        List<Object[]> activityData = metricsRepository.findFeatureActivity(projectId);
        List<FeatureActivity> activities = activityData.stream().map(row -> {
            FeatureActivity fa = new FeatureActivity();
            fa.setName((String) row[0]);
            fa.setCount(((Long) row[1]).intValue());
            fa.setLastUsage((OffsetDateTime) row[2]);
            return fa;
        }).collect(Collectors.toList());
        metrics.setFeatureActivity(activities);

        // 2. Client Versions
        List<Object[]> versionData = metricsRepository.findClientVersionUsage(projectId);
        List<ClientVersionUsage> versions = versionData.stream().map(row -> {
            ClientVersionUsage cvu = new ClientVersionUsage();
            cvu.setVersion((String) row[0]);
            cvu.setCount(((Long) row[1]).intValue());
            return cvu;
        }).collect(Collectors.toList());
        metrics.setClientVersions(versions);

        // 3. Stale Features
        List<FeatureEntity> staleEntities = featureRepository.findByProjectIdAndStale(projectId, true);
        List<StaleFeature> staleFeatures = staleEntities.stream().map(entity -> {
            StaleFeature sf = new StaleFeature();
            sf.setName(entity.getName());
            sf.setLastUsage(metricsRepository.findLastReportedAt(projectId, entity.getName()).orElse(null));
            return sf;
        }).collect(Collectors.toList());
        metrics.setStaleFeatures(staleFeatures);

        return ResponseEntity.ok(metrics);
    }

    @Override
    public ResponseEntity<Void> reportMetrics(MetricsReportRequest metricsReportRequest) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        log.debug("Received metrics report from User-Agent: {}", userAgent);

        final String finalUserAgent = userAgent;
        List<FeatureMetricEntity> entities = metricsReportRequest.getMetrics().stream()
                .map(m -> mapToEntity(m, finalUserAgent))
                .collect(Collectors.toList());

        metricsRepository.saveAll(entities);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private FeatureMetricEntity mapToEntity(FeatureMetric dto, String userAgent) {
        return FeatureMetricEntity.builder()
                .projectId(dto.getProjectId())
                .featureName(dto.getFeatureName())
                .environment(dto.getEnvironment())
                .callCount(dto.getCount())
                .sdkVersion(userAgent)
                .reportedAt(dto.getTimestamp() != null ? dto.getTimestamp() : OffsetDateTime.now())
                .build();
    }
}
