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

package ch.redmoon.unchain.repository;

import ch.redmoon.unchain.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
        long deleteByChangedAtBefore(java.time.OffsetDateTime threshold);

        java.util.List<ch.redmoon.unchain.entity.AuditLogEntity> findTop10ByEntityTypeOrderByChangedAtDesc(
                        String entityType);

        java.util.List<ch.redmoon.unchain.entity.AuditLogEntity> findTop10ByOrderByChangedAtDesc();

        @org.springframework.data.jpa.repository.Query("SELECT a FROM AuditLogEntity a WHERE " +
                        "(:projectId IS NULL OR a.projectId = :projectId) AND " +
                        "(:environment IS NULL OR a.environment = :environment) AND " +
                        "(:featureName IS NULL OR a.featureName = :featureName) AND " +
                        "a.changedAt BETWEEN :from AND :to " +
                        "ORDER BY a.changedAt DESC")
        java.util.List<ch.redmoon.unchain.entity.AuditLogEntity> findHistory(
                        @org.springframework.data.repository.query.Param("projectId") String projectId,
                        @org.springframework.data.repository.query.Param("environment") String environment,
                        @org.springframework.data.repository.query.Param("featureName") String featureName,
                        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
                        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to);

        /**
         * Finds the oldest audit log entry after the given date (for chain anchoring
         * during housekeeping).
         */
        java.util.Optional<ch.redmoon.unchain.entity.AuditLogEntity> findFirstByChangedAtAfterOrderByChangedAtAsc(
                        java.time.OffsetDateTime after);

        /**
         * Finds the most recent audit log entry (for getting the last hash in the
         * chain).
         */
        java.util.Optional<ch.redmoon.unchain.entity.AuditLogEntity> findFirstByOrderByChangedAtDesc();
}
