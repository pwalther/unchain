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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import lombok.RequiredArgsConstructor;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenApiMetadataService {

    private final ResourceLoader resourceLoader;

    private final Map<String, List<String>> permissionMap = new HashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:api/openapi.yaml");
            String content;
            try (InputStream is = resource.getInputStream()) {
                content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }

            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content).getOpenAPI();

            if (openAPI != null && openAPI.getPaths() != null) {
                openAPI.getPaths().forEach((pattern, pathItem) -> {
                    // Convert OpenAPI path params {id} to Ant patterns * or {id}
                    // AntPathMatcher supports {id} syntax as well.
                    processOperation(pattern, "GET", pathItem.getGet());
                    processOperation(pattern, "POST", pathItem.getPost());
                    processOperation(pattern, "PUT", pathItem.getPut());
                    processOperation(pattern, "DELETE", pathItem.getDelete());
                    processOperation(pattern, "PATCH", pathItem.getPatch());
                });
                log.info("Loaded {} endpoint-permission mappings from OpenAPI spec", permissionMap.size());
            } else {
                log.error("OpenAPI spec is null or empty!");
            }
        } catch (Exception e) {
            log.error("Error initializing OpenApiMetadataService", e);
        }
    }

    private void processOperation(String pattern, String method, Operation op) {
        if (op != null && op.getExtensions() != null && op.getExtensions().containsKey("x-required-permissions")) {
            Object permsObj = op.getExtensions().get("x-required-permissions");
            if (permsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> perms = (List<String>) permsObj;
                permissionMap.put(method + ":" + pattern, perms);
                log.debug("Mapped {} {} to permissions: {}", method, pattern, perms);
            }
        }
    }

    public List<String> getRequiredPermissions(String method, String requestUri) {
        // Strip any query parameters if present
        String path = requestUri.split("\\?")[0];

        for (Map.Entry<String, List<String>> entry : permissionMap.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            String patternMethod = parts[0];
            String pattern = parts[1];

            if (patternMethod.equalsIgnoreCase(method) && pathMatcher.match(pattern, path)) {
                return entry.getValue();
            }
        }
        return List.of();
    }
}
