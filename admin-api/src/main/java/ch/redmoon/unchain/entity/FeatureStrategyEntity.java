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

package ch.redmoon.unchain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityListeners;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature_strategy")
@EntityListeners(AuditListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureStrategyEntity {

    @jakarta.persistence.Transient
    private boolean skipAudit;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "feature_name")
    private String featureName;

    @Column(name = "environment_name")
    private String environmentName;

    @Column(name = "strategy_name")
    private String strategyName;

    @ManyToOne
    @JoinColumn(name = "feature_name", insertable = false, updatable = false)
    private FeatureEntity feature;

    @ManyToOne
    @JoinColumn(name = "strategy_name", insertable = false, updatable = false)
    private StrategyDefinitionEntity strategyDefinition;

    // We can also link to Feature and Environment entities if needed,
    // but names are sufficient for now unless we need navigation.
    // For now, let's keep it simple with name references as per schema.

    @OneToMany(mappedBy = "featureStrategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StrategyConstraintEntity> constraints = new ArrayList<>();

    @OneToMany(mappedBy = "featureStrategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureStrategyParameterEntity> parameters = new ArrayList<>();
    @OneToMany(mappedBy = "featureStrategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureStrategyVariantEntity> variants = new ArrayList<>();
}
