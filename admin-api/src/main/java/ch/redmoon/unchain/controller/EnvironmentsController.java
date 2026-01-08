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
import ch.redmoon.unchain.api.model.Environment;
import ch.redmoon.unchain.api.model.EnvironmentList;
import ch.redmoon.unchain.api.model.UpdateEnvironmentRequest;
import ch.redmoon.unchain.api.model.CreateEnvironmentRequest;
import ch.redmoon.unchain.entity.ChangeRequestState;
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

    @Override
    public ResponseEntity<EnvironmentList> getAllEnvironments() {
        List<Environment> dtos = environmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        EnvironmentList response = new EnvironmentList();
        response.setEnvironments(dtos);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Environment> createEnvironment(CreateEnvironmentRequest createEnvironmentRequest) {
        if (environmentRepository.existsById(createEnvironmentRequest.getName())) {
            throw new BusinessRuleViolationException(
                    "Environment '" + createEnvironmentRequest.getName() + "' already exists.");
        }
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setName(createEnvironmentRequest.getName());
        entity.setType(createEnvironmentRequest.getType());
        entity.setEnabled(Boolean.TRUE.equals(createEnvironmentRequest.getEnabled()));
        entity.setSortOrder(createEnvironmentRequest.getSortOrder());
        entity.setRequiredApprovals(createEnvironmentRequest.getRequiredApprovals());

        EnvironmentEntity saved = environmentRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    public ResponseEntity<Environment> getEnvironment(String name) {
        return environmentRepository.findById(name)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Environment> updateEnvironment(String name,
            UpdateEnvironmentRequest updateEnvironmentRequest) {
        return environmentRepository.findById(name)
                .map(entity -> {
                    if (updateEnvironmentRequest.getType() != null)
                        entity.setType(updateEnvironmentRequest.getType());
                    if (updateEnvironmentRequest.getSortOrder() != null)
                        entity.setSortOrder(updateEnvironmentRequest.getSortOrder());
                    if (updateEnvironmentRequest.getRequiredApprovals() != null)
                        entity.setRequiredApprovals(updateEnvironmentRequest.getRequiredApprovals());

                    EnvironmentEntity saved = environmentRepository.save(entity);
                    return ResponseEntity.ok(mapToDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> removeEnvironment(String name) {
        return environmentRepository.findById(name)
                .map(entity -> {
                    if (changeRequestRepository.existsByEnvironmentAndStateIn(name,
                            ChangeRequestState.getPendingStates())) {
                        throw new BusinessRuleViolationException(
                                "Cannot delete environment because it has pending change requests.");
                    }
                    environmentRepository.delete(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Environment mapToDto(EnvironmentEntity entity) {
        Environment dto = new Environment();
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setEnabled(entity.isEnabled());
        dto.setSortOrder(entity.getSortOrder());
        dto.setRequiredApprovals(entity.getRequiredApprovals());
        dto.setProtected(entity.getRequiredApprovals() != null && entity.getRequiredApprovals() > 0);

        long featureCount = featureRepository.countByEnvironmentsName(entity.getName());
        dto.setEnabledToggleCount((int) featureCount);

        long projectCount = projectRepository.count();
        dto.setProjectCount((int) projectCount);

        return dto;
    }
}
