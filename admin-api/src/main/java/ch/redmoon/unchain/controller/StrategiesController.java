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

import ch.redmoon.unchain.api.StrategiesApi;
import ch.redmoon.unchain.api.model.*;
import ch.redmoon.unchain.entity.*;
import ch.redmoon.unchain.repository.FeatureStrategyRepository;
import ch.redmoon.unchain.repository.StrategyDefinitionRepository;
import ch.redmoon.unchain.repository.FeatureRepository;
import ch.redmoon.unchain.repository.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ch.redmoon.unchain.exception.BusinessRuleViolationException;
import ch.redmoon.unchain.entity.ChangeRequestState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StrategiesController implements StrategiesApi {

    private final StrategyDefinitionRepository strategyDefinitionRepository;
    private final FeatureStrategyRepository featureStrategyRepository;
    private final FeatureRepository featureRepository;
    private final EnvironmentRepository environmentRepository;
    private final ch.redmoon.unchain.repository.ChangeRequestRepository changeRequestRepository;

    @Override
    public ResponseEntity<ListStrategies200Response> listStrategies() {
        List<Strategy> dtos = strategyDefinitionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        ListStrategies200Response response = new ListStrategies200Response();
        response.setStrategies(dtos);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createStrategy(CreateStrategyRequest createStrategyRequest) {
        if (strategyDefinitionRepository.existsById(createStrategyRequest.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        StrategyDefinitionEntity entity = new StrategyDefinitionEntity();
        entity.setName(createStrategyRequest.getName());
        entity.setDescription(createStrategyRequest.getDescription());
        entity.setEditable(true); // Default to editable for new strategies

        List<StrategyParameterEntity> params = new ArrayList<>();
        if (createStrategyRequest.getParameters() != null) {
            for (StrategyParameter paramDto : createStrategyRequest.getParameters()) {
                StrategyParameterEntity param = new StrategyParameterEntity();
                param.setName(paramDto.getName());
                param.setStrategyName(entity.getName());
                param.setType(paramDto.getType().getValue());
                param.setDescription(paramDto.getDescription());
                param.setRequired(Boolean.TRUE.equals(paramDto.getRequired()));
                param.setStrategyDefinition(entity);
                params.add(param);
            }
        }
        entity.setParameters(params);

        strategyDefinitionRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> updateStrategyDefinition(@PathVariable("name") String name,
            @RequestBody CreateStrategyRequest request) {
        if (!strategyDefinitionRepository.existsById(name)) {
            return ResponseEntity.notFound().build();
        }

        if (featureStrategyRepository.existsByStrategyName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        StrategyDefinitionEntity entity = strategyDefinitionRepository.findById(name).get();
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getParameters() != null) {
            List<StrategyParameterEntity> newParams = new ArrayList<>();
            for (StrategyParameter paramDto : request.getParameters()) {
                StrategyParameterEntity param = new StrategyParameterEntity();
                param.setName(paramDto.getName());
                param.setStrategyName(entity.getName());
                param.setType(paramDto.getType().getValue());
                param.setDescription(paramDto.getDescription());
                param.setRequired(Boolean.TRUE.equals(paramDto.getRequired()));
                param.setStrategyDefinition(entity);
                newParams.add(param);
            }
            entity.getParameters().clear();
            entity.getParameters().addAll(newParams);
        }

        strategyDefinitionRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Strategy> createStrategyForFeatureInEnvironment(String projectId, String featureName,
            String environment, CreateFeatureStrategyRequest createStrategyRequest) {

        // 1. Validate Feature, Environment, Project.
        Optional<FeatureEntity> featureOpt = featureRepository.findById(featureName);
        if (featureOpt.isEmpty() || !featureOpt.get().getProject().getId().equals(projectId)) {
            return ResponseEntity.notFound().build();
        }
        if (!environmentRepository.existsById(environment)) {
            return ResponseEntity.notFound().build();
        }

        // 2. Validate Strategy Definition exists
        Optional<StrategyDefinitionEntity> strategyDefOpt = strategyDefinitionRepository
                .findById(createStrategyRequest.getName());
        if (strategyDefOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        StrategyDefinitionEntity strategyDef = strategyDefOpt.get();

        // 2.1 Validate required parameters are present in parameters map
        List<String> requiredParams = strategyDef.getParameters().stream()
                .filter(StrategyParameterEntity::isRequired)
                .map(StrategyParameterEntity::getName)
                .collect(Collectors.toList());

        Map<String, String> providedParams = createStrategyRequest.getParameters() != null
                ? createStrategyRequest.getParameters()
                : new HashMap<>();

        if (!requiredParams.isEmpty()) {
            for (String requiredParam : requiredParams) {
                if (!providedParams.containsKey(requiredParam) || providedParams.get(requiredParam) == null) {
                    log.warn("Missing required strategy parameter: {} for strategy: {}", requiredParam,
                            strategyDef.getName());
                    return ResponseEntity.badRequest().build();
                }
            }
        }

        // 3. Create FeatureStrategyEntity
        FeatureStrategyEntity featureStrategy = new FeatureStrategyEntity();
        featureStrategy.setFeatureName(featureName);
        featureStrategy.setEnvironmentName(environment);
        featureStrategy.setStrategyName(createStrategyRequest.getName());

        // 4. Handle Constraints
        if (createStrategyRequest.getConstraints() != null) {
            List<StrategyConstraintEntity> constraintEntities = new ArrayList<>();
            for (Constraint c : createStrategyRequest.getConstraints()) {
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

        // 5. Handle Parameters
        if (createStrategyRequest.getParameters() != null) {
            List<FeatureStrategyParameterEntity> parameterEntities = new ArrayList<>();
            for (Map.Entry<String, String> entry : createStrategyRequest.getParameters().entrySet()) {
                FeatureStrategyParameterEntity pe = new FeatureStrategyParameterEntity();
                log.info("Parameter K/V: {} = {}", entry.getKey(), entry.getValue());
                pe.setName(entry.getKey());
                pe.setValue(entry.getValue());
                pe.setFeatureStrategy(featureStrategy);
                parameterEntities.add(pe);
            }
            featureStrategy.setParameters(parameterEntities);
        }

        // 6. Handle Variants
        if (createStrategyRequest.getVariants() != null) {
            List<FeatureStrategyVariantEntity> variantEntities = new ArrayList<>();
            for (Variant v : createStrategyRequest.getVariants()) {
                FeatureStrategyVariantEntity ve = new FeatureStrategyVariantEntity();
                ve.setName(v.getName());
                ve.setWeight(v.getWeight());
                ve.setStickiness(v.getStickiness());
                if (v.getPayload() != null) {
                    ve.setPayloadType(v.getPayload().getType().getValue());
                    ve.setPayloadValue(v.getPayload().getValue());
                }
                ve.setFeatureStrategy(featureStrategy);
                variantEntities.add(ve);
            }
            featureStrategy.setVariants(variantEntities);
        }

        FeatureStrategyEntity saved = featureStrategyRepository.save(featureStrategy);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToStrategyDto(saved));
    }

    @Override
    public ResponseEntity<Void> deleteStrategyForFeatureInEnvironment(String projectId, String featureName,
            String environment, String strategyId) {
        if (changeRequestRepository.existsByFeatureNameAndChangeRequestStateIn(featureName,
                ChangeRequestState.getPendingStates())) {
            throw new BusinessRuleViolationException(
                    "Cannot delete strategy because the feature has pending change requests.");
        }
        try {
            Integer id = Integer.parseInt(strategyId);
            featureStrategyRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<Strategy> getStrategyForFeatureInEnvironment(String projectId, String featureName,
            String environment, String strategyId) {
        Integer id;
        try {
            id = Integer.parseInt(strategyId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        Optional<FeatureStrategyEntity> featureStrategyOpt = featureStrategyRepository.findById(id);
        if (featureStrategyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapToStrategyDto(featureStrategyOpt.get()));
    }

    @Override
    public ResponseEntity<Void> updateStrategyForFeatureInEnvironment(String projectId, String featureName,
            String environment, String strategyId, UpdateFeatureStrategyRequest updateStrategyRequest) {
        Integer id;
        try {
            id = Integer.parseInt(strategyId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        Optional<FeatureStrategyEntity> strategyOpt = featureStrategyRepository.findById(id);
        if (strategyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FeatureStrategyEntity featureStrategy = strategyOpt.get();

        if (!featureStrategy.getFeatureName().equals(featureName)
                || !featureStrategy.getEnvironmentName().equals(environment)) {
            return ResponseEntity.notFound().build();
        }

        Optional<FeatureEntity> featureOpt = featureRepository.findById(featureName);
        if (featureOpt.isEmpty() || !featureOpt.get().getProject().getId().equals(projectId)) {
            return ResponseEntity.notFound().build();
        }

        Optional<StrategyDefinitionEntity> strategyDefOpt = strategyDefinitionRepository
                .findById(featureStrategy.getStrategyName());
        if (strategyDefOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        StrategyDefinitionEntity strategyDef = strategyDefOpt.get();

        List<String> requiredParams = strategyDef.getParameters().stream()
                .filter(StrategyParameterEntity::isRequired)
                .map(StrategyParameterEntity::getName)
                .collect(Collectors.toList());

        Map<String, String> providedParams = updateStrategyRequest.getParameters() != null
                ? updateStrategyRequest.getParameters()
                : new HashMap<>();

        if (!requiredParams.isEmpty()) {
            for (String requiredParam : requiredParams) {
                if (!providedParams.containsKey(requiredParam) || providedParams.get(requiredParam) == null) {
                    log.warn("Missing required strategy parameter: {} for strategy: {}", requiredParam,
                            strategyDef.getName());
                    return ResponseEntity.badRequest().build();
                }
            }
        }

        featureStrategy.getConstraints().clear();

        if (updateStrategyRequest.getConstraints() != null) {
            for (Constraint c : updateStrategyRequest.getConstraints()) {
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
        if (updateStrategyRequest.getParameters() != null) {
            for (Map.Entry<String, String> entry : updateStrategyRequest.getParameters().entrySet()) {
                FeatureStrategyParameterEntity pe = new FeatureStrategyParameterEntity();
                pe.setName(entry.getKey());
                pe.setValue(entry.getValue());
                pe.setFeatureStrategy(featureStrategy);
                featureStrategy.getParameters().add(pe);
            }
        }

        featureStrategy.getVariants().clear();
        if (updateStrategyRequest.getVariants() != null) {
            for (Variant v : updateStrategyRequest.getVariants()) {
                FeatureStrategyVariantEntity ve = new FeatureStrategyVariantEntity();
                ve.setName(v.getName());
                ve.setWeight(v.getWeight());
                ve.setStickiness(v.getStickiness());
                if (v.getPayload() != null) {
                    ve.setPayloadType(v.getPayload().getType().getValue());
                    ve.setPayloadValue(v.getPayload().getValue());
                }
                ve.setFeatureStrategy(featureStrategy);
                featureStrategy.getVariants().add(ve);
            }
        }

        featureStrategyRepository.save(featureStrategy);
        return ResponseEntity.ok().build();
    }

    private Strategy mapToDto(StrategyDefinitionEntity entity) {
        Strategy dto = new Strategy();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setEditable(entity.isEditable()); // Map editable property

        List<StrategyParameter> params = entity.getParameters().stream().map(p -> {
            StrategyParameter sp = new StrategyParameter();
            sp.setName(p.getName());
            sp.setDescription(p.getDescription());
            sp.setRequired(p.isRequired());
            try {
                sp.setType(StrategyParameter.TypeEnum.fromValue(p.getType()));
            } catch (Exception e) {
            }
            return sp;
        }).collect(Collectors.toList());
        dto.setParameters(params);
        return dto;
    }

    private Strategy mapToStrategyDto(FeatureStrategyEntity entity) {
        Strategy dto = new Strategy();
        dto.setId(entity.getId().toString()); // Return DB ID
        dto.setName(entity.getStrategyName());
        dto.setDescription(
                entity.getStrategyDefinition() != null ? entity.getStrategyDefinition().getDescription() : "");

        // FeatureStrategy doesn't have editable property derived from Definition
        // directly in this model usually,
        // but if Strategy DTO has it, we could look up definition.
        // For assignment, editable usually refers to the definition itself or if the
        // assignment is editable.
        // Assuming defaults for assignment DTO or we fetch definition.
        // Let's safe fetch definition if needed, but for now leave null/default as it
        // wasn't requested for assignment specifically.

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

        if (entity.getStrategyDefinition() != null) {
            Map<String, String> paramValues = entity.getParameters().stream()
                    .collect(Collectors.toMap(FeatureStrategyParameterEntity::getName,
                            FeatureStrategyParameterEntity::getValue));

            List<StrategyParameter> params = entity.getStrategyDefinition().getParameters().stream().map(defParam -> {
                StrategyParameter sp = new StrategyParameter();
                sp.setName(defParam.getName());
                sp.setDescription(defParam.getDescription());
                sp.setRequired(defParam.isRequired());
                try {
                    sp.setType(StrategyParameter.TypeEnum.fromValue(defParam.getType()));
                } catch (Exception e) {
                }
                sp.setValue(paramValues.get(defParam.getName()));
                return sp;
            }).collect(Collectors.toList());
            dto.setParameters(params);
        }

        List<Variant> variants = entity.getVariants().stream().map(v -> {
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
        dto.setVariants(variants);

        return dto;
    }
}
