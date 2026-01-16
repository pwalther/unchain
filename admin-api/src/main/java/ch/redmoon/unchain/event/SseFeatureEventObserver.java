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

package ch.redmoon.unchain.event;

import ch.redmoon.unchain.controller.FeaturesController;
import ch.redmoon.unchain.entity.ChangeRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseFeatureEventObserver implements UnchainEventObserver {

    private final FeaturesController featuresController;

    @Override
    public void onFeatureEnabled(String projectId, String featureName, String environment, String user) {
        log.debug("Notifying SSE clients for feature enabled: {}", featureName);
        featuresController.notifyClients(projectId);
    }

    @Override
    public void onFeatureDisabled(String projectId, String featureName, String environment, String user) {
        log.debug("Notifying SSE clients for feature disabled: {}", featureName);
        featuresController.notifyClients(projectId);
    }

    // You might also want to trigger on feature creation/update/deletion if
    // `FeaturesController` methods publish those events.
    // However, `FeaturesController` methods `createFeature`, `updateFeature` etc
    // not defined in `UnchainEventObserver`.
    // The current `UnchainEventObserver` only tracks enable/disable and change
    // requests.
    // For completeness, we should ideally extend `UnchainEventObserver` or just
    // manually call `notifyClients` in the `FeaturesController` modification
    // methods.
    // Given the task scope, sticking to `onFeatureEnabled/Disabled` is a good
    // start, but usually toggle changes (update variants etc) are critical too.
}
