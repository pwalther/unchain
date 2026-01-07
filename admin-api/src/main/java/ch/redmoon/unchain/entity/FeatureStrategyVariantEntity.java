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

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "feature_strategy_variant")
@EntityListeners(AuditListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureStrategyVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_strategy_id", nullable = false)
    private FeatureStrategyEntity featureStrategy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer weight;

    @Column(name = "payload_type")
    private String payloadType;

    @Column(name = "payload_value")
    @Lob
    private String payloadValue;

    private String stickiness;
}
