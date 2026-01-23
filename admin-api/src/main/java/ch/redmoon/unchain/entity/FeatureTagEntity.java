package ch.redmoon.unchain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "feature_tag")
@IdClass(FeatureTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureTagEntity {

    @Id
    @Column(name = "feature_name")
    private String featureName;

    @Id
    @Column(name = "tag_type")
    private String tagType;

    @Id
    @Column(name = "tag_value")
    private String tagValue;
}
