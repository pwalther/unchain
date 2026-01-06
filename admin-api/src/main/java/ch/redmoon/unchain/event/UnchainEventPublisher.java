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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Central publisher for Unchain events. Dispatches events to all registered
 * {@link UnchainEventObserver}s.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnchainEventPublisher {

    private final List<UnchainEventObserver> observers;

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous";
    }

    public void publishFeatureEnabled(String projectId, String featureName, String environment) {
        String user = getCurrentUser();
        log.debug("Publishing FeatureEnabled event for {}/{} in {} by {}", projectId, featureName, environment, user);
        observers.forEach(o -> {
            try {
                o.onFeatureEnabled(projectId, featureName, environment, user);
            } catch (Exception e) {
                log.error("Error in observer onFeatureEnabled", e);
            }
        });
    }

    public void publishFeatureDisabled(String projectId, String featureName, String environment) {
        String user = getCurrentUser();
        log.debug("Publishing FeatureDisabled event for {}/{} in {} by {}", projectId, featureName, environment, user);
        observers.forEach(o -> {
            try {
                o.onFeatureDisabled(projectId, featureName, environment, user);
            } catch (Exception e) {
                log.error("Error in observer onFeatureDisabled", e);
            }
        });
    }

    public void publishChangeRequestCreated(ChangeRequestEntity changeRequest) {
        String user = getCurrentUser();
        log.debug("Publishing ChangeRequestCreated event for ID {} by {}", changeRequest.getId(), user);
        observers.forEach(o -> {
            try {
                o.onChangeRequestCreated(changeRequest, user);
            } catch (Exception e) {
                log.error("Error in observer onChangeRequestCreated", e);
            }
        });
    }

    public void publishChangeRequestUpdated(ChangeRequestEntity changeRequest, String action) {
        String user = getCurrentUser();
        log.debug("Publishing ChangeRequestUpdated event ({}) for ID {} by {}", action, changeRequest.getId(), user);
        observers.forEach(o -> {
            try {
                o.onChangeRequestUpdated(changeRequest, action, user);
            } catch (Exception e) {
                log.error("Error in observer onChangeRequestUpdated", e);
            }
        });
    }
}
