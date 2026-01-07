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

package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.FeaturesApi;
import ch.redmoon.unchain.api.model.*;
import ch.redmoon.unchain.entity.*;
import ch.redmoon.unchain.repository.*;
import ch.redmoon.unchain.event.UnchainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ch.redmoon.unchain.exception.BusinessRuleViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FeaturesController implements FeaturesApi {

    private final FeatureRepository featureRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final FeatureStrategyRepository featureStrategyRepository;
    private final ch.redmoon.unchain.repository.ChangeRequestRepository changeRequestRepository;
    private final UnchainEventPublisher eventPublisher;

    private static final java.util.List<String> PENDING_STATES = java.util.List.of("Draft", "In review", "Approved");

    @Override
    public ResponseEntity<GetFeaturesByProject200Response> getFeaturesByProject(String projectId) {
        List<FeatureEntity> entities = featureRepository.findByProjectId(projectId);
        List<Feature> dtos = entities.stream().map(this::mapToSummaryDto).collect(Collectors.toList());

        GetFeaturesByProject200Response response = new GetFeaturesByProject200Response();
        response.setFeatures(dtos);

        return ResponseEntity.ok(response);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Feature> createFeature(String projectId, CreateFeatureRequest createFeatureRequest) {
        String featureName = createFeatureRequest.getName() != null ? createFeatureRequest.getName().trim() : null;

        if (featureName == null || featureName.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Attempting to create feature '{}' in project '{}'", featureName, projectId);

        return projectRepository.findById(projectId)
                .map(project -> {
                    if (featureRepository.existsByNameIgnoreCase(featureName)) {
                        log.warn("Feature '{}' already exists (case-insensitive check)", featureName);
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Feature>body(null);
                    }

                    try {
                        FeatureEntity entity = new FeatureEntity();
                        entity.setName(featureName);
                        entity.setDescription(createFeatureRequest.getDescription());
                        if (createFeatureRequest.getType() != null) {
                            entity.setType(createFeatureRequest.getType().getValue());
                        }
                        entity.setImpressionData(Boolean.TRUE.equals(createFeatureRequest.getImpressionData()));
                        entity.setProject(project);
                        entity.setCreatedAt(OffsetDateTime.now());
                        entity.setStale(false);

                        if (createFeatureRequest.getVariants() != null) {
                            for (Variant v : createFeatureRequest.getVariants()) {
                                FeatureVariantEntity ve = new FeatureVariantEntity();
                                ve.setFeature(entity);
                                ve.setName(v.getName());
                                ve.setWeight(v.getWeight());
                                ve.setStickiness(v.getStickiness());
                                if (v.getPayload() != null) {
                                    ve.setPayloadType(v.getPayload().getType().getValue());
                                    ve.setPayloadValue(v.getPayload().getValue());
                                }
                                entity.getVariants().add(ve);
                            }
                        }

                        FeatureEntity saved = featureRepository.saveAndFlush(entity);
                        log.info("Feature '{}' created successfully in project '{}'", featureName, projectId);

                        Feature responseDto = mapToSummaryDto(saved);
                        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.error("Conflict detected while saving feature '{}': {}", featureName, e.getMessage());
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Feature>body(null);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Feature> getFeature(String projectId, String featureName) {
        return featureRepository.findById(featureName)
                .filter(f -> f.getProject().getId().equals(projectId))
                .map(this::mapToFullDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> updateFeature(String projectId, String featureName,
            UpdateFeatureRequest updateFeatureRequest) {
        return featureRepository.findById(featureName)
                .filter(f -> f.getProject().getId().equals(projectId))
                .map(f -> {
                    if (updateFeatureRequest.getDescription() != null)
                        f.setDescription(updateFeatureRequest.getDescription());
                    // if (updateFeatureRequest.getType() != null) // Fix: Enum vs String issue.
                    // Assuming String in Entity for now or use getValue()
                    // f.setType(updateFeatureRequest.getType().getValue());
                    if (updateFeatureRequest.getType() != null)
                        f.setType(updateFeatureRequest.getType().getValue());

                    if (updateFeatureRequest.getStale() != null)
                        f.setStale(updateFeatureRequest.getStale());
                    if (updateFeatureRequest.getImpressionData() != null)
                        f.setImpressionData(updateFeatureRequest.getImpressionData());

                    if (updateFeatureRequest.getVariants() != null) {
                        f.getVariants().clear();
                        for (Variant v : updateFeatureRequest.getVariants()) {
                            FeatureVariantEntity ve = new FeatureVariantEntity();
                            ve.setFeature(f);
                            ve.setName(v.getName());
                            ve.setWeight(v.getWeight());
                            ve.setStickiness(v.getStickiness());
                            if (v.getPayload() != null) {
                                ve.setPayloadType(v.getPayload().getType().getValue());
                                ve.setPayloadValue(v.getPayload().getValue());
                            }
                            f.getVariants().add(ve);
                        }
                    }

                    featureRepository.save(f);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteFeature(String projectId, String featureName) {
        return featureRepository.findById(featureName)
                .filter(f -> f.getProject().getId().equals(projectId))
                .map(f -> {
                    if (changeRequestRepository.existsByFeatureNameAndChangeRequestStateIn(featureName,
                            PENDING_STATES)) {
                        throw new BusinessRuleViolationException(
                                "Cannot delete or archive feature because it has pending change requests.");
                    }
                    // Check if the feature is enabled or has strategies in any protected
                    // environments
                    boolean activeInProtectedEnv = environmentRepository.findAll().stream()
                            .filter(env -> env.getRequiredApprovals() != null && env.getRequiredApprovals() > 0)
                            .anyMatch(env -> {
                                boolean isEnabled = f.getEnvironments().contains(env);
                                boolean hasStrategies = !featureStrategyRepository
                                        .findByFeatureNameAndEnvironmentName(f.getName(), env.getName()).isEmpty();
                                return isEnabled || hasStrategies;
                            });

                    if (activeInProtectedEnv) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }

                    featureRepository.delete(f);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> enableFeature(String projectId, String featureName, String environment) {
        Optional<FeatureEntity> featureOpt = featureRepository.findById(featureName);
        Optional<EnvironmentEntity> envOpt = environmentRepository.findById(environment);

        if (featureOpt.isPresent() && envOpt.isPresent()) {
            FeatureEntity feature = featureOpt.get();
            if (!feature.getProject().getId().equals(projectId)) {
                return ResponseEntity.notFound().build();
            }
            EnvironmentEntity env = envOpt.get();

            if (env.getRequiredApprovals() != null && env.getRequiredApprovals() > 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<FeatureStrategyEntity> strategies = featureStrategyRepository.findByFeatureNameAndEnvironmentName(
                    feature.getName(), env.getName());

            if (strategies.isEmpty()) {
                FeatureStrategyEntity defaultStrategy = new FeatureStrategyEntity();
                defaultStrategy.setFeatureName(feature.getName());
                defaultStrategy.setEnvironmentName(env.getName());
                defaultStrategy.setStrategyName("default");
                featureStrategyRepository.save(defaultStrategy);
            }

            if (!feature.getEnvironments().contains(env)) {
                feature.getEnvironments().add(env);
                featureRepository.save(feature);
                eventPublisher.publishFeatureEnabled(projectId, featureName, environment);
            }
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> disableFeature(String projectId, String featureName, String environment) {
        Optional<FeatureEntity> featureOpt = featureRepository.findById(featureName);
        Optional<EnvironmentEntity> envOpt = environmentRepository.findById(environment);

        if (featureOpt.isPresent() && envOpt.isPresent()) {
            FeatureEntity feature = featureOpt.get();
            if (!feature.getProject().getId().equals(projectId)) {
                return ResponseEntity.notFound().build();
            }
            EnvironmentEntity env = envOpt.get();

            if (env.getRequiredApprovals() != null && env.getRequiredApprovals() > 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (feature.getEnvironments().contains(env)) {
                feature.getEnvironments().remove(env);
                featureRepository.save(feature);
                eventPublisher.publishFeatureDisabled(projectId, featureName, environment);
            }
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Feature mapToSummaryDto(FeatureEntity entity) {
        Feature dto = new Feature();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType());
        dto.setStale(entity.isStale());
        dto.setImpressionData(entity.isImpressionData());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProject(entity.getProject().getId());
        dto.setVariants(mapVariants(entity.getVariants()));

        // Summary includes environments that are enabled or have strategies
        List<EnvironmentEntity> allEnvs = environmentRepository.findAll();
        List<FeatureEnvironment> featureEnvs = allEnvs.stream()
                .map(env -> {
                    boolean isEnabled = entity.getEnvironments().contains(env);
                    List<Strategy> strategies = featureStrategyRepository
                            .findByFeatureNameAndEnvironmentName(entity.getName(), env.getName())
                            .stream()
                            .map(this::mapToStrategyDto)
                            .collect(Collectors.toList());

                    if (isEnabled || !strategies.isEmpty()) {
                        FeatureEnvironment fe = new FeatureEnvironment();
                        fe.setName(env.getName());
                        fe.setEnabled(isEnabled);
                        fe.setStrategies(strategies);
                        return fe;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        dto.setEnvironments(featureEnvs);
        return dto;
    }

    private Feature mapToFullDto(FeatureEntity entity) {
        Feature dto = new Feature();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType());
        dto.setStale(entity.isStale());
        dto.setImpressionData(entity.isImpressionData());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProject(entity.getProject().getId());
        dto.setVariants(mapVariants(entity.getVariants()));

        // Full DTO includes ALL environments
        List<EnvironmentEntity> allEnvs = environmentRepository.findAll();
        List<FeatureEnvironment> featureEnvs = allEnvs.stream().map(env -> {
            FeatureEnvironment fe = new FeatureEnvironment();
            fe.setName(env.getName());

            boolean isEnabled = entity.getEnvironments().stream()
                    .anyMatch(e -> e.getName().equals(env.getName()));
            fe.setEnabled(isEnabled);

            List<Strategy> strategies = featureStrategyRepository
                    .findByFeatureNameAndEnvironmentName(entity.getName(), env.getName())
                    .stream()
                    .map(this::mapToStrategyDto)
                    .collect(Collectors.toList());
            fe.setStrategies(strategies);

            return fe;
        }).collect(Collectors.toList());

        dto.setEnvironments(featureEnvs);
        return dto;
    }

    private Strategy mapToStrategyDto(FeatureStrategyEntity entity) {
        Strategy dto = new Strategy();
        dto.setId(entity.getId().toString());
        dto.setName(entity.getStrategyName());

        List<Constraint> constraints = entity.getConstraints().stream().map(c -> {
            Constraint constraint = new Constraint();
            constraint.setContextName(c.getContextName());
            try {
                constraint.setOperator(Constraint.OperatorEnum.fromValue(c.getOperator()));
            } catch (Exception e) {
            }
            constraint.setCaseInsensitive(c.isCaseInsensitive());
            constraint.setInverted(c.isInverted());
            constraint.setValues(
                    c.getValues().stream().map(StrategyConstraintValueEntity::getValue).collect(Collectors.toList()));
            return constraint;
        }).collect(Collectors.toList());
        dto.setConstraints(constraints);

        if (entity.getParameters() != null) {
            List<StrategyParameter> params = entity.getParameters().stream().map(p -> {
                StrategyParameter sp = new StrategyParameter();
                sp.setName(p.getName());
                sp.setValue(p.getValue());
                return sp;
            }).collect(Collectors.toList());
            dto.setParameters(params);
        }

        dto.setVariants(mapStrategyVariants(entity.getVariants()));
        return dto;
    }

    private List<Variant> mapStrategyVariants(List<FeatureStrategyVariantEntity> entities) {
        if (entities == null)
            return null;
        return entities.stream().map(v -> {
            Variant vd = new Variant();
            vd.setName(v.getName());
            vd.setWeight(v.getWeight());
            vd.setStickiness(v.getStickiness());
            if (v.getPayloadType() != null) {
                VariantPayload vp = new VariantPayload();
                vp.setType(VariantPayload.TypeEnum.fromValue(v.getPayloadType()));
                vp.setValue(v.getPayloadValue());
                vd.setPayload(vp);
            }
            return vd;
        }).collect(Collectors.toList());
    }

    private List<Variant> mapVariants(List<FeatureVariantEntity> entities) {
        if (entities == null)
            return null;
        return entities.stream().map(v -> {
            Variant vd = new Variant();
            vd.setName(v.getName());
            vd.setWeight(v.getWeight());
            vd.setStickiness(v.getStickiness());
            if (v.getPayloadType() != null) {
                VariantPayload vp = new VariantPayload();
                vp.setType(VariantPayload.TypeEnum.fromValue(v.getPayloadType()));
                vp.setValue(v.getPayloadValue());
                vd.setPayload(vp);
            }
            return vd;
        }).collect(Collectors.toList());
    }
}
