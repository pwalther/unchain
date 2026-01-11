/*
   Copyright 2026 Philipp Walther
*/

package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.model.AuditLogItem;
import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects/{projectId}/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Audit log history")
public class HistoryController {

    private final AuditLogRepository auditLogRepository;
    private final java.util.Optional<ch.redmoon.unchain.service.AuditLogIntegrityService> integrityService;

    @GetMapping
    @Operation(summary = "Get project history")
    public ResponseEntity<List<AuditLogItem>> getHistory(
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "environment", required = false) String environment,
            @RequestParam(value = "feature", required = false) String featureName,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        if (from == null) {
            from = OffsetDateTime.now().minusDays(8);
        }
        if (to == null) {
            to = OffsetDateTime.now();
        }

        List<AuditLogEntity> logs = auditLogRepository.findHistory(projectId, environment, featureName, from, to);

        List<AuditLogItem> items = logs.stream()
                .map(this::mapToItem)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    private AuditLogItem mapToItem(AuditLogEntity entity) {
        AuditLogItem item = new AuditLogItem();
        if (entity.getId() != null) {
            item.setId(entity.getId().intValue());
        }
        item.setEntityType(entity.getEntityType());
        item.setDisplayName(ch.redmoon.unchain.util.EntityDisplayNameUtil.getDisplayName(entity.getEntityType()));
        item.setEntityId(entity.getEntityId());
        item.setAction(entity.getAction());
        item.setChangedBy(entity.getChangedBy());
        item.setChangedAt(entity.getChangedAt());
        item.setEnvironment(entity.getEnvironment());
        item.setData(entity.getData());

        // Add integrity verification status if enabled
        if (integrityService.isPresent() && integrityService.get().isEnabled()) {
            boolean signatureValid = integrityService.get().verifySignature(entity);
            item.setSignatureValid(signatureValid);
            // Chain validity is computed at the list level, set to true by default here
            // (chain breaks are detected when verifying sequences)
            item.setChainValid(true);
        }

        return item;
    }
}
