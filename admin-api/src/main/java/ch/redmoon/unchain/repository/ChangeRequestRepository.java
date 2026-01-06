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

import ch.redmoon.unchain.entity.ChangeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity, Integer> {
    List<ChangeRequestEntity> findByProjectId(String projectId);

    List<ChangeRequestEntity> findByState(String state);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    long deleteByStateAndAppliedAtBefore(String state, java.time.OffsetDateTime threshold);

    boolean existsByProjectIdAndStateIn(String projectId, java.util.Collection<String> states);

    boolean existsByEnvironmentAndStateIn(String environment, java.util.Collection<String> states);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) > 0 FROM ChangeRequestChangeEntity c JOIN ChangeRequestEntity cr ON c.changeRequestId = cr.id WHERE c.featureName = :featureName AND cr.state IN :states")
    boolean existsByFeatureNameAndChangeRequestStateIn(String featureName, java.util.Collection<String> states);
}
