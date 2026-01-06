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

import ch.redmoon.unchain.api.DependenciesApi;
import ch.redmoon.unchain.api.model.CreateDependencyRequest;
import ch.redmoon.unchain.repository.FeatureDependencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DependenciesController implements DependenciesApi {

    private final FeatureDependencyRepository featureDependencyRepository;

    @Override
    public ResponseEntity<Void> addDependency(String projectId, String featureName,
            CreateDependencyRequest createDependencyRequest) {
        // Logic to add dependency. Requires FeatureDependencyEntity linked to feature
        // and parent feature.
        // Assuming complex linking, stubbing for now as instructed for unclear/complex
        // ops.
        throw new UnsupportedOperationException("Add dependency not implemented");
    }

    @Override
    public ResponseEntity<Void> removeDependencies(String projectId, String featureName) {
        throw new UnsupportedOperationException("Remove all dependencies not implemented");
    }

    @Override
    public ResponseEntity<Void> removeDependency(String projectId, String featureName, String parentFeature) {
        throw new UnsupportedOperationException("Remove dependency not implemented");
    }
}
