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
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnchainAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final OpenApiMetadataService metadataService;
    private final AuthorizationProvider authorizationProvider;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        String method = context.getRequest().getMethod();
        String uri = context.getRequest().getRequestURI();

        List<String> requiredPermissions = List.of();
        Map<String, String> resourceAttributes = new HashMap<>();

        // 1. Extract required permissions and path variables from OpenAPI metadata
        var mapping = metadataService.getRequiredPermissions(method, uri);
        if (mapping != null) {
            requiredPermissions = mapping.permissions();
            String path = uri.split("\\?")[0];
            try {
                resourceAttributes.putAll(new org.springframework.util.AntPathMatcher()
                        .extractUriTemplateVariables(mapping.pattern(), path));
            } catch (Exception e) {
                log.trace("Could not extract path variables: {}", e.getMessage());
            }
        }

        // 2. Extract query parameters as additional context
        context.getRequest().getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                resourceAttributes.put(key, values[0]);
            }
        });

        boolean isAnonymous = auth == null || trustResolver.isAnonymous(auth);

        if (!isAnonymous && auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                username = oidcUser.getPreferredUsername() != null ? oidcUser.getPreferredUsername()
                        : oidcUser.getFullName();
            }
            log.info("Access check: User '{}' attempting operation '{} {}' [Requires: {}, Context: {}]",
                    username, method, uri, requiredPermissions, resourceAttributes);

            // Delegate to the configured authorization provider
            boolean authorized = authorizationProvider.isAuthorized(auth, requiredPermissions, resourceAttributes);
            log.debug("Authorization provider result for user '{}': {}", username, authorized);
            return new AuthorizationDecision(authorized);
        }

        // If anonymous, only allow if NO permissions are required by the spec
        if (requiredPermissions.isEmpty()) {
            log.debug("Allowing anonymous access to {} {} (no permissions required)", method, uri);
            return new AuthorizationDecision(true);
        }

        log.warn("Blocking anonymous access to {} {} [Requires: {}]", method, uri, requiredPermissions);
        return new AuthorizationDecision(false);
    }
}
