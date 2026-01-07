package ch.redmoon.unchain.repository;

import ch.redmoon.unchain.entity.FeatureMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureMetricRepository extends JpaRepository<FeatureMetricEntity, Long> {
    List<FeatureMetricEntity> findByProjectIdAndFeatureName(String projectId, String featureName);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(m.reportedAt) FROM FeatureMetricEntity m WHERE m.projectId = :projectId AND m.featureName = :featureName")
    Optional<OffsetDateTime> findLastReportedAt(String projectId, String featureName);

    @org.springframework.data.jpa.repository.Query("SELECT m.featureName as featureName, SUM(m.callCount) as count, MAX(m.reportedAt) as lastUsage FROM FeatureMetricEntity m WHERE m.projectId = :projectId GROUP BY m.featureName")
    List<Object[]> findFeatureActivity(@org.springframework.data.repository.query.Param("projectId") String projectId);

    @org.springframework.data.jpa.repository.Query("SELECT m.sdkVersion as sdkVersion, SUM(m.callCount) as count FROM FeatureMetricEntity m WHERE m.projectId = :projectId GROUP BY m.sdkVersion")
    List<Object[]> findClientVersionUsage(
            @org.springframework.data.repository.query.Param("projectId") String projectId);
}
