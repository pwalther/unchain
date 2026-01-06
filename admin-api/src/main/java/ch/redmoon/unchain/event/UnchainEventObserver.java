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

/**
 * Interface for observing events in the Unchain platform.
 * Users can provide their own implementation of this interface to extend
 * logging and alerting.
 */
public interface UnchainEventObserver {
    /**
     * Called when a feature flag is enabled in an environment.
     */
    default void onFeatureEnabled(String projectId, String featureName, String environment, String user) {
    }

    /**
     * Called when a feature flag is disabled in an environment.
     */
    default void onFeatureDisabled(String projectId, String featureName, String environment, String user) {
    }

    /**
     * Called when a new change request is created.
     */
    default void onChangeRequestCreated(ChangeRequestEntity changeRequest, String user) {
    }

    /**
     * Called when a change request is updated (state change, approval, rejection,
     * or adding changes).
     */
    default void onChangeRequestUpdated(ChangeRequestEntity changeRequest, String action, String user) {
    }
}
