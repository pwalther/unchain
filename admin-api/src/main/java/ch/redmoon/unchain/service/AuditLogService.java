package ch.redmoon.unchain.service;

import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(AuditLogEntity auditLog) {
        auditLogRepository.save(auditLog);
    }
}
