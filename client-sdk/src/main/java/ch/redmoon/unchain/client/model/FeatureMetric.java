package ch.redmoon.unchain.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureMetric {
    private String projectId;
    private String featureName;
    private String environment;
    private int count;
    private OffsetDateTime timestamp;
}
