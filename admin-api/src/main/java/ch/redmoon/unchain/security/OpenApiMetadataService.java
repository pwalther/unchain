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

package ch.redmoon.unchain.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenApiMetadataService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public record MappingResult(String pattern, List<String> permissions) {
    }

    public MappingResult getRequiredPermissions(String method, String requestUri) {
        // Strip any query parameters if present
        String path = requestUri.split("\\?")[0];

        // Use the build-time generated metadata map
        // Note: GeneratedApiMetadata is created by the gmavenplus-plugin during the
        // generate-sources phase
        for (Map.Entry<String, List<String>> entry : GeneratedApiMetadata.PERMISSION_MAP.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            String patternMethod = parts[0];
            String pattern = parts[1];

            if (patternMethod.equalsIgnoreCase(method) && pathMatcher.match(pattern, path)) {
                return new MappingResult(pattern, entry.getValue());
            }
        }
        return null;
    }
}
