package ch.redmoon.unchain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureTagId implements Serializable {
    private String featureName;
    private String tagType;
    private String tagValue;
}
