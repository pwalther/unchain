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

import ch.redmoon.unchain.api.EnvironmentsApi;
import ch.redmoon.unchain.api.model.CreateEnvironmentSchema;
import ch.redmoon.unchain.api.model.EnvironmentSchema;
import ch.redmoon.unchain.api.model.EnvironmentsSchema;
import ch.redmoon.unchain.api.model.UpdateEnvironmentSchema;
import ch.redmoon.unchain.entity.EnvironmentEntity;
import ch.redmoon.unchain.repository.EnvironmentRepository;
import ch.redmoon.unchain.repository.FeatureRepository;
import ch.redmoon.unchain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ch.redmoon.unchain.exception.BusinessRuleViolationException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EnvironmentsController implements EnvironmentsApi {

    private final EnvironmentRepository environmentRepository;
    private final FeatureRepository featureRepository;
    private final ProjectRepository projectRepository;
    private final ch.redmoon.unchain.repository.ChangeRequestRepository changeRequestRepository;

    private static final java.util.List<String> PENDING_STATES = java.util.List.of("Draft", "In review", "Approved");

    @Override
    public ResponseEntity<EnvironmentsSchema> getAllEnvironments() {
        List<EnvironmentSchema> dtos = environmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        EnvironmentsSchema schema = new EnvironmentsSchema();
        schema.setEnvironments(dtos);
        schema.setVersion(1); // Stub version
        return ResponseEntity.ok(schema);
    }

    @Override
    public ResponseEntity<EnvironmentSchema> createEnvironment(CreateEnvironmentSchema createEnvironmentSchema) {
        if (environmentRepository.existsById(createEnvironmentSchema.getName())) {
            return ResponseEntity.badRequest().build();
        }
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setName(createEnvironmentSchema.getName());
        entity.setType(createEnvironmentSchema.getType());
        entity.setEnabled(createEnvironmentSchema.getEnabled() != null ? createEnvironmentSchema.getEnabled() : true);
        entity.setSortOrder(createEnvironmentSchema.getSortOrder());
        entity.setRequiredApprovals(
                createEnvironmentSchema.getRequiredApprovals() > 0 ? createEnvironmentSchema.getRequiredApprovals()
                        : 0);

        EnvironmentEntity saved = environmentRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    public ResponseEntity<EnvironmentSchema> getEnvironment(String name) {
        return environmentRepository.findById(name)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<EnvironmentSchema> updateEnvironment(String name,
            UpdateEnvironmentSchema updateEnvironmentSchema) {
        return environmentRepository.findById(name)
                .map(entity -> {
                    if (updateEnvironmentSchema.getType() != null)
                        entity.setType(updateEnvironmentSchema.getType());
                    if (updateEnvironmentSchema.getSortOrder() != null)
                        entity.setSortOrder(updateEnvironmentSchema.getSortOrder());
                    if (updateEnvironmentSchema.getRequiredApprovals() != null)
                        entity.setRequiredApprovals(updateEnvironmentSchema.getRequiredApprovals() > 0
                                ? updateEnvironmentSchema.getRequiredApprovals()
                                : 0);
                    EnvironmentEntity saved = environmentRepository.save(entity);
                    return ResponseEntity.ok(mapToDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> removeEnvironment(String name) {
        return environmentRepository.findById(name)
                .map(entity -> {
                    if (changeRequestRepository.existsByEnvironmentAndStateIn(name, PENDING_STATES)) {
                        throw new BusinessRuleViolationException(
                                "Cannot delete environment because it has pending change requests.");
                    }
                    if (entity.getRequiredApprovals() > 0) {
                        return ResponseEntity.badRequest().<Void>build();
                    }
                    environmentRepository.delete(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.ok().build());
    }

    private EnvironmentSchema mapToDto(EnvironmentEntity entity) {
        EnvironmentSchema dto = new EnvironmentSchema();
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setEnabled(entity.isEnabled());
        dto.setProtected(entity.getRequiredApprovals() != null && entity.getRequiredApprovals() > 0);
        dto.setSortOrder(entity.getSortOrder());
        dto.setRequiredApprovals(entity.getRequiredApprovals());

        // Calculate actual counts
        long enabledToggles = featureRepository.findAll().stream()
                .filter(f -> f.getEnvironments().stream().anyMatch(e -> e.getName().equals(entity.getName())))
                .count();

        long projectsUsingEnv = projectRepository.findAll().stream()
                .filter(p -> p.getFeatures().stream()
                        .anyMatch(f -> f.getEnvironments().stream()
                                .anyMatch(e -> e.getName().equals(entity.getName()))))
                .count();

        dto.setProjectCount((int) projectsUsingEnv);
        dto.setEnabledToggleCount((int) enabledToggles);
        return dto;
    }
}
