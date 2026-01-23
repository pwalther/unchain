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

import ch.redmoon.unchain.api.FeatureTagApi;
import ch.redmoon.unchain.api.model.FeatureTag;
import ch.redmoon.unchain.api.model.ListTagTypes200Response;
import ch.redmoon.unchain.api.model.ListTags200Response;
import ch.redmoon.unchain.api.model.TagType;
import ch.redmoon.unchain.entity.TagEntity;
import ch.redmoon.unchain.entity.TagTypeEntity;
import ch.redmoon.unchain.entity.FeatureTagEntity;
import ch.redmoon.unchain.entity.FeatureTagId;
import ch.redmoon.unchain.repository.TagRepository;
import ch.redmoon.unchain.repository.TagTypeRepository;
import ch.redmoon.unchain.repository.FeatureTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TagsController implements FeatureTagApi {

    private final TagRepository tagRepository;
    private final TagTypeRepository tagTypeRepository;
    private final FeatureTagRepository featureTagRepository;

    @Override
    public ResponseEntity<ListTags200Response> listTags() {
        List<FeatureTag> dtos = tagRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        ListTags200Response response = new ListTags200Response();
        response.setTags(dtos);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createTag(FeatureTag featureTag) {
        // Tag identity is type+value. But repo might not easily check existence by
        // composite ID without instance.
        // Assuming simple save for now.
        TagEntity entity = new TagEntity();
        entity.setType(featureTag.getType());
        entity.setTag_value(featureTag.getValue());
        tagRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<ListTagTypes200Response> listTagTypes() {
        List<TagType> dtos = tagTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        ListTagTypes200Response response = new ListTagTypes200Response();
        response.setTagTypes(dtos);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FeatureTag> addTagToFeature(String projectId, String featureName, FeatureTag featureTag) {
        // Ensure tag exists
        TagEntity tagEntity = new TagEntity();
        tagEntity.setType(featureTag.getType());
        tagEntity.setTag_value(featureTag.getValue());
        tagRepository.save(tagEntity);

        // Link to feature
        FeatureTagEntity featureTagEntity = new FeatureTagEntity();
        featureTagEntity.setFeatureName(featureName);
        featureTagEntity.setTagType(featureTag.getType());
        featureTagEntity.setTagValue(featureTag.getValue());
        featureTagRepository.save(featureTagEntity);

        return ResponseEntity.status(HttpStatus.CREATED).body(featureTag);
    }

    @Override
    public ResponseEntity<Void> removeTagFromFeature(String projectId, String featureName, String type, String value) {
        FeatureTagId id = new FeatureTagId(featureName, type, value);
        featureTagRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private FeatureTag mapToDto(TagEntity entity) {
        FeatureTag dto = new FeatureTag();
        dto.setType(entity.getType());
        dto.setValue(entity.getTag_value());
        return dto;
    }

    private TagType mapToDto(TagTypeEntity entity) {
        TagType dto = new TagType();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        // dto.setIcon(entity.getIcon()); // if exists
        return dto;
    }
}
