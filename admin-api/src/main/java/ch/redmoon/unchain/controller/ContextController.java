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

package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.ContextApi;
import ch.redmoon.unchain.api.model.ContextField;
import ch.redmoon.unchain.api.model.LegalValue;
import ch.redmoon.unchain.entity.ContextFieldEntity;
import ch.redmoon.unchain.entity.LegalValueEntity;
import ch.redmoon.unchain.repository.ContextFieldRepository;
import ch.redmoon.unchain.repository.LegalValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ContextController implements ContextApi {

    private final ContextFieldRepository contextFieldRepository;
    private final LegalValueRepository legalValueRepository;

    @Override
    public ResponseEntity<List<ContextField>> listContextFields() {
        List<ContextField> dtos = contextFieldRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<Void> createContextField(ContextField contextField) {
        if (contextFieldRepository.existsById(contextField.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ContextFieldEntity entity = new ContextFieldEntity();
        entity.setName(contextField.getName());
        entity.setDescription(contextField.getDescription());
        entity.setStickiness(Boolean.TRUE.equals(contextField.getStickiness()));
        entity.setSortOrder(contextField.getSortOrder());
        contextFieldRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> updateContextField(String contextField, ContextField contextField2) {
        return contextFieldRepository.findById(contextField)
                .map(entity -> {
                    entity.setDescription(contextField2.getDescription());
                    entity.setStickiness(Boolean.TRUE.equals(contextField2.getStickiness()));
                    entity.setSortOrder(contextField2.getSortOrder());
                    contextFieldRepository.save(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteContextField(String contextField) {
        if (contextFieldRepository.existsById(contextField)) {
            contextFieldRepository.deleteById(contextField);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> addLegalValueToContextField(String contextField, LegalValue legalValue) {
        return contextFieldRepository.findById(contextField)
                .map(field -> {
                    LegalValueEntity entity = new LegalValueEntity();
                    entity.setContextFieldName(contextField);
                    entity.setLegalValue(legalValue.getValue());

                    entity.setDescription(legalValue.getDescription());
                    legalValueRepository.save(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> removeLegalValueFromContextField(String contextField, String value) {
        return contextFieldRepository.findById(contextField)
                .map(field -> {
                    legalValueRepository.deleteByContextFieldNameAndLegalValue(contextField, value);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ContextField mapToDto(ContextFieldEntity entity) {
        ContextField dto = new ContextField();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStickiness(entity.isStickiness());
        dto.setSortOrder(entity.getSortOrder());
        if (entity.getLegalValues() != null) {
            dto.setLegalValues(entity.getLegalValues().stream().map(lv -> {
                LegalValue legalValue = new LegalValue();
                legalValue.setValue(lv.getLegalValue());
                legalValue.setDescription(lv.getDescription());
                return legalValue;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
