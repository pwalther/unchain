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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "context_field_legal_value")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(LegalValueEntity.LegalValueId.class)
public class LegalValueEntity {

    @Id
    @Column(name = "context_field_name")
    private String contextFieldName;

    @Id
    @Column(name = "legal_value")
    private String legalValue;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_field_name", insertable = false, updatable = false)
    private ContextFieldEntity contextField;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegalValueId implements Serializable {
        private String contextFieldName;
        private String legalValue;
    }
}
