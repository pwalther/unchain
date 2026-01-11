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

package ch.redmoon.unchain.entity;

import ch.redmoon.unchain.config.BeanUtil;
import ch.redmoon.unchain.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

public class AuditListener {

    private final ObjectMapper objectMapper;

    public AuditListener() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        logAudit(entity, "CREATED");
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        logAudit(entity, "UPDATED");
    }

    @PreRemove
    public void onPreRemove(Object entity) {
        logAudit(entity, "DELETED");
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditListener.class);

    private static final ThreadLocal<Boolean> SKIP_AUDIT = ThreadLocal.withInitial(() -> false);

    public static void suspendAudit(Runnable action) {
        SKIP_AUDIT.set(true);
        try {
            action.run();
        } finally {
            SKIP_AUDIT.remove();
        }
    }

    private void logAudit(Object entity, String action) {
        if (Boolean.TRUE.equals(SKIP_AUDIT.get())) {
            return;
        }

        try {
            // Check for skipAudit flag
            try {
                Field skipAuditField = entity.getClass().getDeclaredField("skipAudit");
                skipAuditField.setAccessible(true);
                if (skipAuditField.getBoolean(entity)) {
                    return;
                }
            } catch (NoSuchFieldException e) {
                // Ignore if field doesn't exist
            }

            AuditLogService auditLogService = BeanUtil.getBean(AuditLogService.class);

            String entityType = entity.getClass().getSimpleName();
            String entityId = getEntityId(entity);
            String data = null;
            String projectId = null;
            String environment = null;
            String featureName = null;

            try {
                // Simplified serialization to avoid recursion and lazy loading issues
                java.util.Map<String, Object> summary = new java.util.HashMap<>();
                summary.put("type", entityType);
                summary.put("id", entityId);
                // Try to add a few more details if possible without recursion
                if (entity instanceof FeatureEntity f) {
                    summary.put("description", f.getDescription());
                    summary.put("type", f.getType());
                    summary.put("featureName", f.getName());
                    featureName = f.getName();
                    try {
                        if (f.getProject() != null) {
                            summary.put("project", f.getProject().getId());
                            projectId = f.getProject().getId();
                        }
                    } catch (Exception e) {
                        // Ignore if project cannot be accessed
                    }
                } else if (entity instanceof ProjectEntity p) {
                    summary.put("name", p.getName());
                    projectId = p.getId();
                } else if (entity instanceof FeatureStrategyEntity fs) {
                    summary.put("featureName", fs.getFeatureName());
                    summary.put("strategyName", fs.getStrategyName());
                    summary.put("environmentName", fs.getEnvironmentName());
                    featureName = fs.getFeatureName();
                    environment = fs.getEnvironmentName();
                    try {
                        ch.redmoon.unchain.repository.FeatureRepository featureRepository = BeanUtil
                                .getBean(ch.redmoon.unchain.repository.FeatureRepository.class);
                        featureRepository.findById(fs.getFeatureName()).ifPresent(f -> {
                            if (f.getProject() != null) {
                                summary.put("project", f.getProject().getId());
                                // We cannot assign to local variable 'projectId' from lambda, so we put it in
                                // map
                                // and retrieve it after, or use atomic reference.
                                // Actually, simpler to not use lambda for assignment to local var.
                            }
                        });
                        // Non-lambda approach to set the local variable 'projectId'
                        var f = featureRepository.findById(fs.getFeatureName()).orElse(null);
                        if (f != null && f.getProject() != null) {
                            projectId = f.getProject().getId();
                            summary.put("project", projectId);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                } else if (entity instanceof ChangeRequestEntity cr) {
                    projectId = cr.getProjectId();
                    environment = cr.getEnvironment();
                    summary.put("title", cr.getTitle());
                }
                data = objectMapper.writeValueAsString(summary);
            } catch (Throwable e) {
                data = "{\"error\": \"Error serializing entity summary: " + e.getMessage() + "\"}";
            }

            AuditLogEntity auditLog = AuditLogEntity.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .data(data)
                    .changedBy(getCurrentUser())
                    .changedAt(OffsetDateTime.now())
                    .projectId(projectId)
                    .environment(environment)
                    .featureName(featureName)
                    .build();

            auditLogService.saveAuditLog(auditLog);
        } catch (Throwable e) {
            log.error("Failed to save audit log for {} {}: {}", entity.getClass().getSimpleName(), action,
                    e.getMessage(), e);
        }
    }

    private String getEntityId(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    return value != null ? value.toString() : "unknown";
                } catch (IllegalAccessException e) {
                    return "unknown";
                }
            }
        }
        return "unknown";
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
