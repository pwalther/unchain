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

import ch.redmoon.unchain.api.SegmentsApi;
import ch.redmoon.unchain.api.model.CreateSegmentRequest;
import ch.redmoon.unchain.api.model.ListSegments200Response;
import ch.redmoon.unchain.api.model.Segment;
import ch.redmoon.unchain.api.model.UpdateSegmentRequest;
import ch.redmoon.unchain.entity.SegmentEntity;
import ch.redmoon.unchain.repository.SegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SegmentsController implements SegmentsApi {

    private final SegmentRepository segmentRepository;

    @Override
    public ResponseEntity<ListSegments200Response> listSegments() {
        List<Segment> dtos = segmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        ListSegments200Response response = new ListSegments200Response();
        response.setSegments(dtos);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createSegment(CreateSegmentRequest createSegmentRequest) {
        SegmentEntity entity = new SegmentEntity();
        entity.setName(createSegmentRequest.getName());
        entity.setDescription(createSegmentRequest.getDescription());
        // Handling ID generation? DB usually handles ID if Integer.
        // Assuming JPA generated value.

        segmentRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Segment> getSegment(Integer segmentId) {
        return segmentRepository.findById(segmentId)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> updateSegment(Integer segmentId, UpdateSegmentRequest updateSegmentRequest) {
        return segmentRepository.findById(segmentId)
                .map(entity -> {
                    if (updateSegmentRequest.getName() != null)
                        entity.setName(updateSegmentRequest.getName());
                    segmentRepository.save(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteSegment(Integer segmentId) {
        if (segmentRepository.existsById(segmentId)) {
            segmentRepository.deleteById(segmentId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Segment mapToDto(SegmentEntity entity) {
        Segment dto = new Segment();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
