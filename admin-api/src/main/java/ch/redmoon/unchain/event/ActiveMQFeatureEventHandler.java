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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ActiveMQFeatureEventHandler implements FeatureEventHandler {

    // In a real implementation, you would inject a JmsTemplate or similar
    // private final JmsTemplate jmsTemplate;

    // We need a way to notify the controller or service that manages SSE
    // connections.
    // Ideally, this class listens to MQ and calls the controller.
    // For this sample, we just log that we would be listening.

    private final FeaturesController featuresController;

    // @JmsListener(destination = "feature-updates") // Example annotation
    public void onMessage(String message) {
        // Parse message to get projectId
        // String projectId = parse(message);
        // onFeatureUpdate(projectId);
    }

    @Override
    public void onFeatureUpdate(String projectId) {
        log.info("Received external feature update event for project: {}", projectId);
        if (featuresController != null) {
            featuresController.notifyClients(projectId);
        }
    }
}
