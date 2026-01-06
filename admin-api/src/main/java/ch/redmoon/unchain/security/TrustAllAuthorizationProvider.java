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

import org.springframework.security.core.Authentication;
import java.util.List;

/**
 * A default implementation that trusts all authenticated users.
 */
public class TrustAllAuthorizationProvider implements AuthorizationProvider {
    @Override
    public boolean isAuthorized(Authentication authentication, List<String> requiredPermissions) {
        // Trust all authenticated users regardless of required permissions
        return authentication != null && authentication.isAuthenticated();
    }
}
