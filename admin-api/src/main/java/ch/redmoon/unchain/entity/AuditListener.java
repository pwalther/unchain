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

    private void logAudit(Object entity, String action) {
        try {
            AuditLogService auditLogService = BeanUtil.getBean(AuditLogService.class);

            String entityType = entity.getClass().getSimpleName();
            String entityId = getEntityId(entity);
            String data = null;

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
                    try {
                        if (f.getProject() != null) {
                            summary.put("project", f.getProject().getId());
                        }
                    } catch (Exception e) {
                        // Ignore if project cannot be accessed
                    }
                } else if (entity instanceof ProjectEntity p) {
                    summary.put("name", p.getName());
                } else if (entity instanceof FeatureStrategyEntity fs) {
                    summary.put("featureName", fs.getFeatureName());
                    summary.put("strategyName", fs.getStrategyName());
                    summary.put("environmentName", fs.getEnvironmentName());
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
