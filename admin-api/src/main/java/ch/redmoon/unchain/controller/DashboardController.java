package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.DashboardApi;
import ch.redmoon.unchain.api.model.AuditLogItem;
import ch.redmoon.unchain.api.model.DashboardSummary;
import ch.redmoon.unchain.api.model.ProjectDashboardItem;
import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.entity.FeatureEntity;
import ch.redmoon.unchain.entity.ProjectEntity;
import ch.redmoon.unchain.repository.AuditLogRepository;
import ch.redmoon.unchain.repository.FeatureRepository;
import ch.redmoon.unchain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final ProjectRepository projectRepository;
    private final FeatureRepository featureRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        DashboardSummary summary = new DashboardSummary();

        List<ProjectEntity> projects = projectRepository.findAll();
        summary.setProjectCount(projects.size());

        List<FeatureEntity> allFeatures = featureRepository.findAll();
        summary.setFeatureCount(allFeatures.size());

        long activeFeatures = allFeatures.stream()
                .filter(f -> !f.getEnvironments().isEmpty())
                .count();
        summary.setActiveFeatureCount((int) activeFeatures);

        long staleFeatures = allFeatures.stream()
                .filter(FeatureEntity::isStale)
                .count();
        summary.setStaleFeatureCount((int) staleFeatures);

        List<ProjectDashboardItem> projectItems = projects.stream().map(p -> {
            ProjectDashboardItem item = new ProjectDashboardItem();
            item.setId(p.getId());
            item.setName(p.getName());
            item.setFeatureCount(p.getFeatures().size());
            item.setHealth(p.getHealth());
            return item;
        }).collect(Collectors.toList());
        summary.setProjects(projectItems);

        List<AuditLogEntity> recentAuditLogs = auditLogRepository.findTop10ByOrderByChangedAtDesc();
        List<AuditLogItem> auditLogItems = recentAuditLogs.stream().map(log -> {
            AuditLogItem item = new AuditLogItem();
            item.setId(log.getId() != null ? log.getId().intValue() : null);
            item.setEntityType(log.getEntityType());
            item.setEntityId(log.getEntityId());
            item.setAction(log.getAction());
            item.setChangedBy(log.getChangedBy());
            item.setChangedAt(log.getChangedAt());

            String data = log.getData();
            if ("FeatureStrategyEntity".equals(log.getEntityType()) && data != null) {
                try {
                    java.util.Map<String, Object> dataMap = objectMapper.readValue(data,
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                    if (!dataMap.containsKey("project")) {
                        String featureName = (String) dataMap.get("featureName");
                        if (featureName != null) {
                            java.util.Optional<FeatureEntity> feature = featureRepository.findById(featureName);
                            if (feature.isPresent() && feature.get().getProject() != null) {
                                dataMap.put("project", feature.get().getProject().getId());
                                data = objectMapper.writeValueAsString(dataMap);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback to original data on error
                }
            }
            item.setData(data);
            return item;
        }).collect(Collectors.toList());
        summary.setRecentChanges(auditLogItems);

        return ResponseEntity.ok(summary);
    }
}
