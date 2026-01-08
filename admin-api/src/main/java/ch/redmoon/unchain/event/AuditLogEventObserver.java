package ch.redmoon.unchain.event;

import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.entity.ChangeRequestEntity;
import ch.redmoon.unchain.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Observer that logs Unchain events to the audit log.
 */
@Component
@RequiredArgsConstructor
public class AuditLogEventObserver implements UnchainEventObserver {

    private final AuditLogService auditLogService;

    @Override
    public void onFeatureEnabled(String projectId, String featureName, String environment, String user) {
        logFeatureAction(projectId, featureName, "ENABLED", environment, user);
    }

    @Override
    public void onFeatureDisabled(String projectId, String featureName, String environment, String user) {
        logFeatureAction(projectId, featureName, "DISABLED", environment, user);
    }

    @Override
    public void onChangeRequestCreated(ChangeRequestEntity changeRequest, String user) {
        AuditLogEntity log = AuditLogEntity.builder()
                .entityType("ChangeRequestEntity")
                .entityId(changeRequest.getId().toString())
                .action("CREATED")
                .changedBy(user)
                .changedAt(OffsetDateTime.now())
                .data("{\"title\":\"" + changeRequest.getTitle() + "\", \"environment\":\""
                        + changeRequest.getEnvironment() + "\", \"project\":\"" + changeRequest.getProjectId()
                        + "\"}")
                .build();
        auditLogService.saveAuditLog(log);
    }

    @Override
    public void onChangeRequestUpdated(ChangeRequestEntity changeRequest, String action, String user) {
        AuditLogEntity log = AuditLogEntity.builder()
                .entityType("ChangeRequestEntity")
                .entityId(changeRequest.getId().toString())
                .action(action)
                .changedBy(user)
                .changedAt(OffsetDateTime.now())
                .data("{\"state\":\"" + changeRequest.getState() + "\", \"project\":\""
                        + changeRequest.getProjectId() + "\"}")
                .build();
        auditLogService.saveAuditLog(log);
    }

    private void logFeatureAction(String projectId, String featureName, String action, String environment,
            String user) {
        AuditLogEntity log = AuditLogEntity.builder()
                .entityType("FeatureEntity")
                .entityId(featureName)
                .action(action)
                .changedBy(user)
                .changedAt(OffsetDateTime.now())
                .data("{\"project\":\"" + projectId + "\", \"environment\":\"" + environment + "\", \"featureName\":\""
                        + featureName + "\"}")
                .build();
        auditLogService.saveAuditLog(log);
    }
}
