package ch.redmoon.unchain.service;

import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final Optional<AuditLogIntegrityService> integrityService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(AuditLogEntity auditLog) {
        // If integrity is enabled, compute and set signature with hash chaining
        if (integrityService.isPresent() && integrityService.get().isEnabled()) {
            AuditLogIntegrityService service = integrityService.get();

            // Get the hash of the last audit log entry for chain linking
            String previousHash = auditLogRepository.findFirstByOrderByChangedAtDesc()
                    .map(service::computeHash)
                    .orElse(null);

            auditLog.setPreviousHash(previousHash);

            // Save first to get the ID
            auditLogRepository.save(auditLog);

            // Now compute signature with the ID included
            String signature = service.computeSignature(auditLog, previousHash);
            auditLog.setSignature(signature);

            // Update with signature
            auditLogRepository.save(auditLog);

            log.debug("Saved audit log with signature: id={}, previousHash={}",
                    auditLog.getId(), previousHash != null ? "present" : "null");
        } else {
            // No integrity checking, just save
            auditLogRepository.save(auditLog);
        }
    }
}
