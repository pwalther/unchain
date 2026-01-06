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

package ch.redmoon.unchain.scheduler;

import ch.redmoon.unchain.entity.ChangeRequestEntity;
import ch.redmoon.unchain.repository.ChangeRequestRepository;
import ch.redmoon.unchain.service.ChangeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChangeRequestScheduler {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestService changeRequestService;

    @Scheduled(cron = "0 * * * * *") // Every minute
    @SchedulerLock(name = "applyChangeRequestsLock", lockAtMostFor = "50s", lockAtLeastFor = "10s")
    public void applyScheduledChangeRequests() {
        log.debug("Checking for change requests to apply...");

        OffsetDateTime now = OffsetDateTime.now();

        // Find all approved change requests
        List<ChangeRequestEntity> approvedCRs = changeRequestRepository.findByState("Approved");

        for (ChangeRequestEntity cr : approvedCRs) {
            // Apply if scheduledAt is null (immediately) or scheduledAt <= now
            if (cr.getScheduledAt() == null || cr.getScheduledAt().isBefore(now) || cr.getScheduledAt().isEqual(now)) {
                try {
                    changeRequestService.applyChangeRequest(cr.getId());
                } catch (Exception e) {
                    log.error("Failed to apply change request {}: {}", cr.getId(), e.getMessage(), e);
                    // Optionally: mark as "Failed" or leave as "Approved" to retry?
                    // For now, it stays Approved and will retry next minute.
                }
            }
        }
    }
}
