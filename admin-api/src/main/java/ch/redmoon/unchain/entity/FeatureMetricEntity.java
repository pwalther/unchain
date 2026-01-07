package ch.redmoon.unchain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;

@Entity
@Table(name = "feature_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(name = "environment", nullable = false)
    private String environment;

    @Column(name = "sdk_version", nullable = false)
    private String sdkVersion;

    @Column(name = "call_count", nullable = false)
    private Integer callCount;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;
}
