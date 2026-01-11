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

package ch.redmoon.unchain.util;

/**
 * Utility class for converting entity type class names to human-readable
 * display names.
 * Used primarily in audit logs and other user-facing contexts where entity
 * types need to be displayed.
 */
public final class EntityDisplayNameUtil {

    private EntityDisplayNameUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts an entity type class name to a human-readable display name.
     *
     * @param entityType the fully qualified or simple class name of the entity
     *                   (e.g., "FeatureEntity")
     * @return the human-readable display name (e.g., "Feature")
     */
    public static String getDisplayName(String entityType) {
        if (entityType == null) {
            return "Unknown";
        }

        return switch (entityType) {
            case "FeatureEntity" -> "Feature";
            case "FeatureStrategyEntity" -> "Feature Strategy";
            case "ProjectEntity" -> "Project";
            case "EnvironmentEntity" -> "Environment";
            case "ChangeRequestEntity" -> "Change Request";
            case "StrategyDefinitionEntity" -> "Strategy Definition";
            case "TagEntity" -> "Tag";
            case "TagTypeEntity" -> "Tag Type";
            case "ContextFieldEntity" -> "Context Field";
            case "SegmentEntity" -> "Segment";
            default -> entityType; // Fallback to the original name if not mapped
        };
    }
}
