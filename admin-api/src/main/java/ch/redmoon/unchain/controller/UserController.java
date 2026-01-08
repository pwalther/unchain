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

package ch.redmoon.unchain.controller;

import ch.redmoon.unchain.api.UserApi;
import ch.redmoon.unchain.api.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class UserController implements UserApi {

    @Override
    public ResponseEntity<User> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = new User();

        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser)) {
            user.setAuthenticated(false);
            return ResponseEntity.ok(user);
        }

        OidcUser principal = (OidcUser) authentication.getPrincipal();
        user.setAuthenticated(true);
        user.setName(principal.getFullName());
        user.setEmail(principal.getEmail());
        user.setUsername(
                principal.getPreferredUsername() != null ? principal.getPreferredUsername() : principal.getSubject());
        user.setPicture(principal.getPicture() != null ? principal.getPicture() : "");
        user.setAuthorities(principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(user);
    }
}
