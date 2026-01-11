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

package ch.redmoon.unchain.service.housekeeping;

import ch.redmoon.unchain.repository.AuditLogRepository;
import ch.redmoon.unchain.repository.ChangeRequestRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class HousekeepingScheduler {

    private final ChangeRequestRepository changeRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final int retentionPeriodMonths;
    private final int auditLogRetentionYears;
    private final ch.redmoon.unchain.service.AuditLogIntegrityService integrityService;

    public HousekeepingScheduler(
            ChangeRequestRepository changeRequestRepository,
            AuditLogRepository auditLogRepository,
            @Value("${unchain.housekeeping.retention-period-months:1}") int retentionPeriodMonths,
            @Value("${unchain.housekeeping.audit-log-retention-years:1}") int auditLogRetentionYears,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ch.redmoon.unchain.service.AuditLogIntegrityService integrityService) {
        this.changeRequestRepository = changeRequestRepository;
        this.auditLogRepository = auditLogRepository;
        this.retentionPeriodMonths = retentionPeriodMonths;
        this.auditLogRetentionYears = auditLogRetentionYears;
        this.integrityService = integrityService;
    }

    @Scheduled(cron = "${unchain.housekeeping.cron:0 0 1 * * *}") // Default: daily at 1 AM
    @SchedulerLock(name = "HousekeepingScheduler_cleanupAppliedChangeRequests", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    @Transactional
    public void cleanupAppliedChangeRequests() {
        log.info("Starting housekeeping: cleaning up applied change requests older than {} months",
                retentionPeriodMonths);

        OffsetDateTime threshold = OffsetDateTime.now().minusMonths(retentionPeriodMonths);

        long deletedCount = changeRequestRepository.deleteByStateAndAppliedAtBefore("Applied", threshold);

        log.info("Housekeeping finished: deleted {} applied change requests", deletedCount);
    }

    @Scheduled(cron = "${unchain.housekeeping.audit-log-cron:0 0 2 * * *}") // Default: daily at 2 AM
    @SchedulerLock(name = "HousekeepingScheduler_cleanupAuditLogs", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    @Transactional
    public void cleanupAuditLogs() {
        log.info("Starting housekeeping: cleaning up audit logs older than {} years",
                auditLogRetentionYears);

        OffsetDateTime threshold = OffsetDateTime.now().minusYears(auditLogRetentionYears);

        // If integrity checking is enabled, anchor the chain before deletion
        if (integrityService != null && integrityService.isEnabled()) {
            log.debug("Anchoring hash chain before audit log deletion");

            // Find the oldest entry that will remain after cleanup
            java.util.Optional<ch.redmoon.unchain.entity.AuditLogEntity> newChainStart = auditLogRepository
                    .findFirstByChangedAtAfterOrderByChangedAtAsc(threshold);

            if (newChainStart.isPresent()) {
                // Anchor the chain: reset previousHash and recompute signature
                ch.redmoon.unchain.entity.AuditLogEntity anchor = newChainStart.get();
                anchor.setPreviousHash(null);
                String newSignature = integrityService.computeSignature(anchor, null);
                anchor.setSignature(newSignature);
                auditLogRepository.save(anchor);

                log.info("Anchored hash chain at audit log ID {} before deletion", anchor.getId());
            } else {
                log.debug("No audit logs will remain after cleanup, no anchoring needed");
            }
        }

        // Delete old entries
        long deletedCount = auditLogRepository.deleteByChangedAtBefore(threshold);

        log.info("Housekeeping finished: deleted {} audit log entries", deletedCount);
    }
}
