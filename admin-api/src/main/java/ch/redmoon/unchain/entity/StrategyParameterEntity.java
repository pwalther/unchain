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
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "strategy_parameter")
@IdClass(StrategyParameterId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyParameterEntity {

    @Id
    @Column(name = "strategy_name")
    private String strategyName;

    @Id
    private String name;

    private String type;

    private String description;

    @Column(name = "is_required")
    private boolean required;

    @ManyToOne
    @JoinColumn(name = "strategy_name", insertable = false, updatable = false)
    private StrategyDefinitionEntity strategyDefinition;
}
