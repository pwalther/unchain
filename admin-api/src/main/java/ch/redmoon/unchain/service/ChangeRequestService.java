/*
   Copyright 2026 Philipp Walther

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.redmoon.unchain.service;

import ch.redmoon.unchain.api.model.Constraint;
import ch.redmoon.unchain.entity.*;
import ch.redmoon.unchain.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestChangeRepository changeRequestChangeRepository;
    private final FeatureRepository featureRepository;
    private final EnvironmentRepository environmentRepository;
    private final FeatureStrategyRepository featureStrategyRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public void applyChangeRequest(Integer changeRequestId) {
        ChangeRequestEntity cr = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new RuntimeException("Change request not found: " + changeRequestId));

        if (!"Approved".equals(cr.getState())) {
            log.warn("Attempted to apply change request {} which is in state {}", changeRequestId, cr.getState());
            return;
        }

        log.info("Applying change request {}: {}", changeRequestId, cr.getTitle());

        List<ChangeRequestChangeEntity> changes = changeRequestChangeRepository.findByChangeRequestId(changeRequestId);
        List<Map<String, Object>> changeSummaries = new ArrayList<>();

        for (ChangeRequestChangeEntity change : changes) {
            applyChange(cr.getProjectId(), cr.getEnvironment(), change);

            Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("action", change.getAction());
            summary.put("feature", change.getFeatureName());
            try {
                if (change.getPayload() != null) {
                    summary.put("payload", objectMapper.readValue(change.getPayload(), Map.class));
                }
            } catch (Exception e) {
                summary.put("payload", change.getPayload());
            }
            changeSummaries.add(summary);
        }

        cr.setState("Applied");
        cr.setAppliedAt(java.time.OffsetDateTime.now());
        changeRequestRepository.save(cr);

        // Manually log the summary audit log
        try {
            AuditLogEntity auditLog = AuditLogEntity.builder()
                    .entityType("ChangeRequestEntity")
                    .entityId(String.valueOf(changeRequestId))
                    .action("APPLIED")
                    .data(objectMapper.writeValueAsString(changeSummaries))
                    .changedBy(SecurityContextHolder.getContext().getAuthentication() != null
                            ? SecurityContextHolder.getContext().getAuthentication().getName()
                            : "system")
                    .changedAt(java.time.OffsetDateTime.now())
                    .projectId(cr.getProjectId())
                    .environment(cr.getEnvironment())
                    .build();

            auditLogService.saveAuditLog(auditLog);
        } catch (Exception e) {
            log.error("Failed to create summary audit log for CR {}", changeRequestId, e);
        }

        log.info("Change request {} applied successfully", changeRequestId);
    }

    private void applyChange(String projectId, String environment, ChangeRequestChangeEntity change) {
        String action = change.getAction();
        String featureName = change.getFeatureName();
        String payloadJson = change.getPayload();

        log.info("Applying change: {} on feature {} in environment {}", action, featureName, environment);

        Optional<FeatureEntity> featureOpt = featureRepository.findById(featureName);
        Optional<EnvironmentEntity> envOpt = environmentRepository.findById(environment);

        if (featureOpt.isEmpty() || envOpt.isEmpty()) {
            log.error("Feature {} or Environment {} not found for change {}", featureName, environment, change.getId());
            return;
        }

        FeatureEntity feature = featureOpt.get();
        EnvironmentEntity env = envOpt.get();

        try {
            switch (action) {
                case "enable":
                    enableFeature(feature, env);
                    break;
                case "disable":
                    disableFeature(feature, env);
                    break;
                case "add-strategy":
                    addStrategy(feature, env, payloadJson);
                    break;
                case "update-strategy":
                    updateStrategy(feature, env, payloadJson);
                    break;
                case "delete-strategy":
                    deleteStrategy(feature, env, payloadJson);
                    break;
                case "archive-feature":
                    archiveFeature(feature);
                    break;
                default:
                    log.warn("Unknown action: {}", action);
            }
        } catch (Exception e) {
            log.error("Failed to apply change {}: {}", change.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to apply change " + change.getId(), e);
        }
    }

    private void enableFeature(FeatureEntity feature, EnvironmentEntity env) {
        List<FeatureStrategyEntity> strategies = featureStrategyRepository.findByFeatureNameAndEnvironmentName(
                feature.getName(), env.getName());

        if (strategies.isEmpty()) {
            FeatureStrategyEntity defaultStrategy = new FeatureStrategyEntity();
            defaultStrategy.setFeatureName(feature.getName());
            defaultStrategy.setEnvironmentName(env.getName());
            defaultStrategy.setStrategyName("default");
            defaultStrategy.setSkipAudit(true);
            featureStrategyRepository.save(defaultStrategy);
        }

        if (!feature.getEnvironments().contains(env)) {
            feature.getEnvironments().add(env);
            feature.setSkipAudit(true);
            featureRepository.save(feature);
        }
    }

    private void disableFeature(FeatureEntity feature, EnvironmentEntity env) {
        if (feature.getEnvironments().contains(env)) {
            feature.getEnvironments().remove(env);
            feature.setSkipAudit(true);
            featureRepository.save(feature);
        }
    }

    private void addStrategy(FeatureEntity feature, EnvironmentEntity env, String payloadJson) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
        });

        FeatureStrategyEntity featureStrategy = new FeatureStrategyEntity();
        featureStrategy.setFeatureName(feature.getName());
        featureStrategy.setEnvironmentName(env.getName());
        featureStrategy.setStrategyName((String) payload.get("name"));
        featureStrategy.setSkipAudit(true);

        // Handle constraints
        if (payload.get("constraints") != null) {
            List<Constraint> constraints = objectMapper.convertValue(payload.get("constraints"),
                    new TypeReference<List<Constraint>>() {
                    });
            List<StrategyConstraintEntity> constraintEntities = new ArrayList<>();
            for (Constraint c : constraints) {
                StrategyConstraintEntity sce = new StrategyConstraintEntity();
                sce.setContextName(c.getContextName());
                if (c.getOperator() != null) {
                    sce.setOperator(c.getOperator().toString());
                }
                sce.setCaseInsensitive(Boolean.TRUE.equals(c.getCaseInsensitive()));
                sce.setInverted(Boolean.TRUE.equals(c.getInverted()));
                sce.setFeatureStrategy(featureStrategy);

                List<StrategyConstraintValueEntity> values = new ArrayList<>();
                if (c.getValues() != null) {
                    for (String v : c.getValues()) {
                        StrategyConstraintValueEntity ve = new StrategyConstraintValueEntity();
                        ve.setValue(v);
                        ve.setStrategyConstraint(sce);
                        values.add(ve);
                    }
                }
                sce.setValues(values);
                constraintEntities.add(sce);
            }
            featureStrategy.setConstraints(constraintEntities);
        }

        // Handle parameters
        if (payload.get("parameters") != null) {
            Map<String, String> parameters = objectMapper.convertValue(payload.get("parameters"),
                    new TypeReference<Map<String, String>>() {
                    });
            List<FeatureStrategyParameterEntity> parameterEntities = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                FeatureStrategyParameterEntity pe = new FeatureStrategyParameterEntity();
                pe.setName(entry.getKey());
                pe.setValue(entry.getValue());
                pe.setFeatureStrategy(featureStrategy);
                parameterEntities.add(pe);
            }
            featureStrategy.setParameters(parameterEntities);
        }

        featureStrategyRepository.save(featureStrategy);
    }

    private void updateStrategy(FeatureEntity feature, EnvironmentEntity env, String payloadJson) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
        });
        Object idObj = payload.get("id");
        if (idObj == null) {
            throw new RuntimeException("Strategy ID missing in update-strategy payload");
        }

        Integer strategyId = Integer.parseInt(idObj.toString());
        FeatureStrategyEntity featureStrategy = featureStrategyRepository.findById(strategyId)
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + strategyId));

        if (!featureStrategy.getFeatureName().equals(feature.getName()) ||
                !featureStrategy.getEnvironmentName().equals(env.getName())) {
            throw new RuntimeException("Strategy " + strategyId + " does not belong to feature " + feature.getName()
                    + " in env " + env.getName());
        }

        featureStrategy.setSkipAudit(true);

        // Clear existing
        featureStrategy.getConstraints().clear();
        if (payload.get("constraints") != null) {
            List<Constraint> constraints = objectMapper.convertValue(payload.get("constraints"),
                    new TypeReference<List<Constraint>>() {
                    });
            for (Constraint c : constraints) {
                StrategyConstraintEntity sce = new StrategyConstraintEntity();
                sce.setContextName(c.getContextName());
                if (c.getOperator() != null) {
                    sce.setOperator(c.getOperator().toString());
                }
                sce.setCaseInsensitive(Boolean.TRUE.equals(c.getCaseInsensitive()));
                sce.setInverted(Boolean.TRUE.equals(c.getInverted()));
                sce.setFeatureStrategy(featureStrategy);

                List<StrategyConstraintValueEntity> values = new ArrayList<>();
                if (c.getValues() != null) {
                    for (String v : c.getValues()) {
                        StrategyConstraintValueEntity ve = new StrategyConstraintValueEntity();
                        ve.setValue(v);
                        ve.setStrategyConstraint(sce);
                        values.add(ve);
                    }
                }
                sce.setValues(values);
                featureStrategy.getConstraints().add(sce);
            }
        }

        featureStrategy.getParameters().clear();
        if (payload.get("parameters") != null) {
            Map<String, String> parameters = objectMapper.convertValue(payload.get("parameters"),
                    new TypeReference<Map<String, String>>() {
                    });
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                FeatureStrategyParameterEntity pe = new FeatureStrategyParameterEntity();
                pe.setName(entry.getKey());
                pe.setValue(entry.getValue());
                pe.setFeatureStrategy(featureStrategy);
                featureStrategy.getParameters().add(pe);
            }
        }

        featureStrategyRepository.save(featureStrategy);
    }

    private void deleteStrategy(FeatureEntity feature, EnvironmentEntity env, String payloadJson) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
        });
        Object idObj = payload.get("id");
        if (idObj == null) {
            throw new RuntimeException("Strategy ID missing in delete-strategy payload");
        }
        Integer strategyId = Integer.parseInt(idObj.toString());

        // Fetch to set skipAudit before delete
        FeatureStrategyEntity strategy = featureStrategyRepository.findById(strategyId).orElse(null);
        if (strategy != null) {
            strategy.setSkipAudit(true);
            featureStrategyRepository.delete(strategy);
        }
    }

    private void archiveFeature(FeatureEntity feature) {
        // In this implementation, archiving a feature means deleting it.
        feature.setSkipAudit(true);
        featureRepository.delete(feature);
    }
}
