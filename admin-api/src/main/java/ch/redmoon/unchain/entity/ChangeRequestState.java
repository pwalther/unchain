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

package ch.redmoon.unchain.entity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * State of a Change Request.
 */
public enum ChangeRequestState {
    DRAFT("Draft"),
    IN_REVIEW("In review"),
    APPROVED("Approved"),
    APPLIED("Applied"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected");

    private final String value;

    ChangeRequestState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<String> getPendingStates() {
        return Stream.of(DRAFT, IN_REVIEW, APPROVED)
                .map(ChangeRequestState::getValue)
                .collect(Collectors.toList());
    }

    public static ChangeRequestState fromValue(String value) {
        for (ChangeRequestState state : ChangeRequestState.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state: " + value);
    }
}
