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

import ch.redmoon.unchain.api.ChangeRequestsApi;
import ch.redmoon.unchain.api.model.ChangeRequest;
import ch.redmoon.unchain.api.model.ChangeRequestConfig;
import ch.redmoon.unchain.api.model.ChangeRequestCreatedBy;
import ch.redmoon.unchain.api.model.ChangeRequestFeaturesInner;
import ch.redmoon.unchain.api.model.ChangeRequestFeaturesInnerChangesInner;
import ch.redmoon.unchain.api.model.CreateChangeRequestRequest;
import ch.redmoon.unchain.api.model.UpdateChangeRequestStateRequest;
import ch.redmoon.unchain.entity.ChangeRequestChangeEntity;
import ch.redmoon.unchain.entity.ChangeRequestEntity;
import ch.redmoon.unchain.entity.EnvironmentEntity;
import ch.redmoon.unchain.repository.ChangeRequestChangeRepository;
import ch.redmoon.unchain.repository.ChangeRequestRepository;
import ch.redmoon.unchain.repository.EnvironmentRepository;
import ch.redmoon.unchain.event.UnchainEventPublisher;
import ch.redmoon.unchain.service.ChangeRequestService;
import ch.redmoon.unchain.exception.BusinessRuleViolationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChangeRequestsController implements ChangeRequestsApi {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestChangeRepository changeRequestChangeRepository;
    private final EnvironmentRepository environmentRepository;
    private final ChangeRequestService changeRequestService;
    private final UnchainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<List<ChangeRequestConfig>> getChangeRequestConfig(String projectId) {
        List<ChangeRequestConfig> configs = environmentRepository.findAll().stream()
                .map(env -> {
                    ChangeRequestConfig config = new ChangeRequestConfig();
                    config.setEnvironment(env.getName());
                    config.setType(env.getType());
                    config.setChangeRequestEnabled(
                            env.getRequiredApprovals() != null && env.getRequiredApprovals() > 0);
                    config.setRequiredApprovals(env.getRequiredApprovals());
                    return config;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(configs);
    }

    @Override
    public ResponseEntity<List<ChangeRequest>> listChangeRequests(String projectId) {
        List<ChangeRequest> requests = changeRequestRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }

    @Override
    @Transactional
    public ResponseEntity<ChangeRequest> createChangeRequest(String projectId,
            CreateChangeRequestRequest request) {
        ChangeRequestEntity entity = new ChangeRequestEntity();
        entity.setTitle(request.getTitle());
        entity.setEnvironment(request.getEnvironment());
        entity.setProjectId(projectId);
        entity.setState("Draft"); // Start as Draft

        EnvironmentEntity env = environmentRepository.findById(request.getEnvironment()).orElse(null);
        entity.setMinApprovals(env != null ? env.getRequiredApprovals() : 0);

        if (request.getScheduledAt() != null
                && request.getScheduledAt().plusMinutes(1).isBefore(java.time.OffsetDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        entity.setScheduledAt(request.getScheduledAt());

        ChangeRequestEntity saved = changeRequestRepository.save(entity);

        if (request.getChanges() != null) {
            List<ChangeRequestChangeEntity> savedChanges = new ArrayList<>();
            for (var changeReq : request.getChanges()) {
                String payloadStr = null;
                try {
                    payloadStr = objectMapper.writeValueAsString(changeReq.getPayload());
                } catch (Exception e) {
                    log.error("Failed to serialize payload", e);
                    throw new BusinessRuleViolationException(
                            "Failed to serialize payload for feature: " + changeReq.getFeature());
                }

                final String finalPayloadStr = payloadStr;
                boolean isDuplicate = savedChanges.stream()
                        .anyMatch(e -> e.getFeatureName().equals(changeReq.getFeature()) &&
                                e.getAction().equals(changeReq.getAction()) &&
                                e.getPayload().equals(finalPayloadStr));

                if (!isDuplicate) {
                    ChangeRequestChangeEntity change = new ChangeRequestChangeEntity();
                    change.setChangeRequestId(saved.getId());
                    change.setFeatureName(changeReq.getFeature());
                    change.setAction(changeReq.getAction());
                    change.setPayload(finalPayloadStr);
                    changeRequestChangeRepository.save(change);
                    savedChanges.add(change);
                }
            }
        }

        eventPublisher.publishChangeRequestCreated(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    public ResponseEntity<ChangeRequest> getChangeRequest(String projectId, Integer changeRequestId) {
        return changeRequestRepository.findById(changeRequestId)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<ChangeRequest> addChangesToRequest(String projectId, Integer changeRequestId,
            ch.redmoon.unchain.api.model.AddChangesToRequestRequest request) {
        ChangeRequestEntity cr = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new BusinessRuleViolationException("Change request not found"));

        if (!"Draft".equals(cr.getState())) {
            throw new BusinessRuleViolationException("Changes can only be added to Draft change requests");
        }

        List<ChangeRequestChangeEntity> existingChanges = changeRequestChangeRepository
                .findByChangeRequestId(cr.getId());

        if (request.getChanges() != null) {
            for (var changeReq : request.getChanges()) {
                String payloadStr = null;
                try {
                    payloadStr = objectMapper.writeValueAsString(changeReq.getPayload());
                } catch (Exception e) {
                    log.error("Failed to serialize payload", e);
                    throw new BusinessRuleViolationException(
                            "Failed to serialize payload for feature: " + changeReq.getFeature());
                }

                final String finalPayloadStr = payloadStr;
                boolean isDuplicate = existingChanges.stream()
                        .anyMatch(e -> e.getFeatureName().equals(changeReq.getFeature()) &&
                                e.getAction().equals(changeReq.getAction()) &&
                                e.getPayload().equals(finalPayloadStr));

                if (!isDuplicate) {
                    ChangeRequestChangeEntity change = new ChangeRequestChangeEntity();
                    change.setChangeRequestId(cr.getId());
                    change.setFeatureName(changeReq.getFeature());
                    change.setAction(changeReq.getAction());
                    change.setPayload(finalPayloadStr);
                    changeRequestChangeRepository.save(change);
                    // Add to existingChanges list to avoid adding same change twice in the same
                    // batch
                    existingChanges.add(change);
                }
            }
        }

        eventPublisher.publishChangeRequestUpdated(cr, "ChangesAdded");
        return ResponseEntity.ok(mapToDto(cr));
    }

    @Override
    public ResponseEntity<Void> deleteChangeRequest(String projectId, Integer changeRequestId) {
        changeRequestRepository.findById(changeRequestId).ifPresent(cr -> {
            changeRequestRepository.delete(cr);
            eventPublisher.publishChangeRequestUpdated(cr, "Deleted");
        });
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateChangeRequestState(String projectId, Integer changeRequestId,
            UpdateChangeRequestStateRequest updateRequest) {
        changeRequestRepository.findById(changeRequestId).ifPresent(cr -> {
            String newState = updateRequest.getState().getValue();
            if ("Applied".equals(newState) && !"Applied".equals(cr.getState())) {
                changeRequestService.applyChangeRequest(changeRequestId);
            } else {
                cr.setState(newState);
                changeRequestRepository.save(cr);
                eventPublisher.publishChangeRequestUpdated(cr, "StateChanged:" + newState);
            }
        });
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> approveChangeRequest(String projectId, Integer changeRequestId) {
        changeRequestRepository.findById(changeRequestId).ifPresent(cr -> {
            if ("Draft".equals(cr.getState())) {
                throw new BusinessRuleViolationException(
                        "Cannot approve a Draft change request. Please submit it for review first.");
            }
            if (cr.getScheduledAt() == null) {
                // If no schedule, apply immediately
                cr.setState("Approved"); // Technicality, applyChangeRequest checks for Approved
                changeRequestRepository.save(cr);
                changeRequestService.applyChangeRequest(changeRequestId);
                eventPublisher.publishChangeRequestUpdated(cr, "ApprovedAndApplied");
            } else {
                cr.setState("Approved");
                changeRequestRepository.save(cr);
                eventPublisher.publishChangeRequestUpdated(cr, "Approved");
            }
        });
        return ResponseEntity.ok().build();
    }

    private ChangeRequest mapToDto(ChangeRequestEntity entity) {
        ChangeRequest dto = new ChangeRequest();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setState(ChangeRequest.StateEnum.fromValue(entity.getState()));
        dto.setEnvironment(entity.getEnvironment());
        dto.setProject(entity.getProjectId());
        dto.setMinApprovals(entity.getMinApprovals());
        dto.setScheduledAt(entity.getScheduledAt());

        ChangeRequestCreatedBy createdBy = new ChangeRequestCreatedBy();
        createdBy.setUsername("admin"); // Mock user
        dto.setCreatedBy(createdBy);

        List<ChangeRequestChangeEntity> changes = changeRequestChangeRepository.findByChangeRequestId(entity.getId());
        Map<String, List<ChangeRequestChangeEntity>> grouped = changes.stream()
                .collect(Collectors.groupingBy(ChangeRequestChangeEntity::getFeatureName));

        List<ChangeRequestFeaturesInner> features = new ArrayList<>();
        grouped.forEach((featureName, featureChanges) -> {
            ChangeRequestFeaturesInner featureDto = new ChangeRequestFeaturesInner();
            featureDto.setName(featureName);
            featureDto.setChanges(featureChanges.stream().map(c -> {
                ChangeRequestFeaturesInnerChangesInner changeDto = new ChangeRequestFeaturesInnerChangesInner();
                changeDto.setAction(c.getAction());
                try {
                    changeDto.setPayload(objectMapper.readValue(c.getPayload(), Map.class));
                } catch (Exception e) {
                    log.error("Failed to parse payload", e);
                }
                return changeDto;
            }).collect(Collectors.toList()));
            features.add(featureDto);
        });
        dto.setFeatures(features);

        return dto;
    }
}
