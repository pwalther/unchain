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
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityListeners;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature")
@EntityListeners(AuditListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureEntity {

    @Id
    private String name;

    private String description;

    private String type;

    private boolean stale;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "impression_data")
    private boolean impressionData;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ProjectEntity project;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "feature_environments", joinColumns = @JoinColumn(name = "feature_name"), inverseJoinColumns = @JoinColumn(name = "environment_name"))
    private List<EnvironmentEntity> environments = new ArrayList<>();
    @OneToMany(mappedBy = "feature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureVariantEntity> variants = new ArrayList<>();
}
