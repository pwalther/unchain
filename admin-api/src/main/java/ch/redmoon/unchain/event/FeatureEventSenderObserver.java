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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "unchain.sse.sender")
public class FeatureEventSenderObserver implements UnchainEventObserver {

    private final FeatureEventSender featureEventSender;

    @Override
    public void onFeatureEnabled(String projectId, String featureName, String environment, String user) {
        log.debug("Sending feature update to sender for feature enabled: {}", featureName);
        featureEventSender.sendFeatureUpdate(projectId);
    }

    @Override
    public void onFeatureDisabled(String projectId, String featureName, String environment, String user) {
        log.debug("Sending feature update to sender for feature disabled: {}", featureName);
        featureEventSender.sendFeatureUpdate(projectId);
    }

    @Override
    public void onFeatureCreated(String projectId, String featureName, String user) {
        log.debug("Sending feature update to sender for feature created: {}", featureName);
        featureEventSender.sendFeatureUpdate(projectId);
    }

    @Override
    public void onFeatureUpdated(String projectId, String featureName, String user) {
        log.debug("Sending feature update to sender for feature updated: {}", featureName);
        featureEventSender.sendFeatureUpdate(projectId);
    }

    @Override
    public void onFeatureDeleted(String projectId, String featureName, String user) {
        log.debug("Sending feature update to sender for feature deleted: {}", featureName);
        featureEventSender.sendFeatureUpdate(projectId);
    }
}
