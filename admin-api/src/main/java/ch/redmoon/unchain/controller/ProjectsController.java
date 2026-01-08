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

import ch.redmoon.unchain.api.ProjectsApi;
import ch.redmoon.unchain.api.model.CreateProjectRequest;
import ch.redmoon.unchain.api.model.Error;
import ch.redmoon.unchain.api.model.ListProjects200Response;
import ch.redmoon.unchain.api.model.Project;
import ch.redmoon.unchain.api.model.UpdateProjectRequest;
import ch.redmoon.unchain.entity.ChangeRequestState;
import ch.redmoon.unchain.entity.ProjectEntity;
import ch.redmoon.unchain.repository.ProjectRepository;
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
public class ProjectsController implements ProjectsApi {

    private final ProjectRepository projectRepository;
    private final ch.redmoon.unchain.repository.ChangeRequestRepository changeRequestRepository;

    @Override
    public ResponseEntity<ListProjects200Response> listProjects(Optional<Boolean> archived) {
        List<ProjectEntity> entities;
        if (archived.isPresent() && archived.get()) {
            entities = projectRepository.findAll();
        } else {
            entities = projectRepository.findAllByArchivedIsFalse();
        }

        List<Project> dtos = entities.stream().map(this::mapToDto).collect(Collectors.toList());
        ListProjects200Response response = new ListProjects200Response();
        response.setProjects(dtos);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Project> createProject(CreateProjectRequest createProjectRequest) {
        if (projectRepository.existsById(createProjectRequest.getId())) {
            Error error = new Error().message("Project with ID '" + createProjectRequest.getId() + "' already exists.");
            return new ResponseEntity(error, HttpStatus.CONFLICT);
        }
        ProjectEntity entity = new ProjectEntity();
        entity.setId(createProjectRequest.getId());
        entity.setName(createProjectRequest.getName());
        entity.setDescription(createProjectRequest.getDescription());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setHealth(100);
        entity.setArchived(false);

        ProjectEntity saved = projectRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    public ResponseEntity<Project> getProject(String projectId) {
        return projectRepository.findById(projectId)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> updateProject(String projectId, UpdateProjectRequest updateProjectRequest) {
        return projectRepository.findById(projectId)
                .map(entity -> {
                    if (updateProjectRequest.getName() != null
                            && !updateProjectRequest.getName().equals(entity.getName())) {
                        if (changeRequestRepository.existsByProjectIdAndStateIn(projectId,
                                ChangeRequestState.getPendingStates())) {
                            throw new BusinessRuleViolationException(
                                    "Cannot rename project because it has pending change requests.");
                        }
                        entity.setName(updateProjectRequest.getName());
                    }
                    if (updateProjectRequest.getDescription() != null)
                        entity.setDescription(updateProjectRequest.getDescription());
                    entity.setUpdatedAt(OffsetDateTime.now());
                    projectRepository.save(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.notFound().build();
        }
        if (changeRequestRepository.existsByProjectIdAndStateIn(projectId, ChangeRequestState.getPendingStates())) {
            throw new BusinessRuleViolationException("Cannot delete project because it has pending change requests.");
        }
        projectRepository.deleteById(projectId);
        return ResponseEntity.ok().build();
    }

    private Project mapToDto(ProjectEntity entity) {
        Project dto = new Project();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setHealth(entity.getHealth());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
