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

package ch.redmoon.unchain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import ch.redmoon.unchain.security.UnchainAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

@Configuration
@Profile("oidc")
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final UnchainAuthorizationManager authorizationManager;

        @Value("${unchain.security.oauth2.base-uri}")
        private String oauth2BaseUri;

        @Value("${unchain.security.oauth2.success-url}")
        private String oauth2SuccessUrl;

        @Value("${unchain.security.cors.allowed-origins}")
        private List<String> corsAllowedOrigins;

        @Bean
        @ConditionalOnMissingBean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                                                .requestMatchers("/login/**", "/oauth2/**", "/callback").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .anyRequest().access(authorizationManager))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                                                                org.springframework.http.HttpStatus.UNAUTHORIZED)))
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' "
                                                                                                + String.join(" ",
                                                                                                                corsAllowedOrigins)
                                                                                                + ";"))
                                                .xssProtection(xss -> xss.disable()) // Modern browsers ignore this, CSP
                                                                                     // is better
                                                .frameOptions(frame -> frame.sameOrigin())
                                                .contentTypeOptions(Customizer.withDefaults()))
                                // console
                                .oauth2Login(oauth2 -> oauth2
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri(oauth2BaseUri))
                                                .defaultSuccessUrl(oauth2SuccessUrl, true))
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
                return http.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
                configuration.setAllowedOrigins(corsAllowedOrigins);
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
