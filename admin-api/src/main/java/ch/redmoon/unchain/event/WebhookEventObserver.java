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

import ch.redmoon.unchain.entity.ChangeRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link UnchainEventObserver} that sends events to a
 * webhook.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventObserver implements UnchainEventObserver {

    @Qualifier("unchainWebhookRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${unchain.webhook.url:}")
    private String webhookUrl;

    @Value("${unchain.webhook.enabled:false}")
    private boolean enabled;

    @Autowired
    @Lazy
    private WebhookEventObserver self;

    @Override
    @Async
    public void onFeatureEnabled(String projectId, String featureName, String environment, String user) {
        self.sendWebhook("FeatureEnabled", Map.of(
                "projectId", projectId,
                "featureName", featureName,
                "environment", environment,
                "user", user));
    }

    @Override
    @Async
    public void onFeatureDisabled(String projectId, String featureName, String environment, String user) {
        self.sendWebhook("FeatureDisabled", Map.of(
                "projectId", projectId,
                "featureName", featureName,
                "environment", environment,
                "user", user));
    }

    @Override
    @Async
    public void onChangeRequestCreated(ChangeRequestEntity changeRequest, String user) {
        self.sendWebhook("ChangeRequestCreated", Map.of(
                "id", changeRequest.getId(),
                "title", changeRequest.getTitle(),
                "projectId", changeRequest.getProjectId(),
                "environment", changeRequest.getEnvironment(),
                "state", changeRequest.getState(),
                "user", user));
    }

    @Override
    @Async
    public void onChangeRequestUpdated(ChangeRequestEntity changeRequest, String action, String user) {
        self.sendWebhook("ChangeRequestUpdated", Map.of(
                "id", changeRequest.getId(),
                "title", changeRequest.getTitle(),
                "projectId", changeRequest.getProjectId(),
                "environment", changeRequest.getEnvironment(),
                "state", changeRequest.getState(),
                "action", action,
                "user", user));
    }

    @Retryable(retryFor = {
            org.springframework.web.client.RestClientException.class }, maxAttemptsExpression = "${unchain.webhook.retry.max-attempts:5}", backoff = @Backoff(delayExpression = "${unchain.webhook.retry.initial-delay-ms:1000}", multiplierExpression = "${unchain.webhook.retry.multiplier:2.0}"))
    public void sendWebhook(String eventType, Map<String, Object> payload) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("timestamp", java.time.OffsetDateTime.now().toString());
        body.put("data", payload);

        log.debug("Sending webhook event {} to {} (Attempt context: {})", eventType, webhookUrl, payload);
        restTemplate.postForEntity(webhookUrl, body, Void.class);
    }
}
