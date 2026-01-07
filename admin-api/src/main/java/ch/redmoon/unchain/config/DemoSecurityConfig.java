package ch.redmoon.unchain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;

@Configuration
@Profile("demo")
public class DemoSecurityConfig {

        @Bean
        @Order(1)
        public SecurityFilterChain demoFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(new DemoAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class)
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

                return http.build();
        }

        private static class DemoAuthenticationFilter extends OncePerRequestFilter {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain)
                                throws ServletException, IOException {

                        // Skip for static resources or console if needed, but authenticated() handles
                        // it

                        Map<String, Object> claims = Map.of(
                                        "sub", "demo-user",
                                        "name", "Demo User",
                                        "email", "demo@demodomain.com",
                                        "preferred_username", "demo",
                                        "picture", "https://api.dicebear.com/7.x/avataaars/svg?seed=demo");

                        OidcIdToken idToken = new OidcIdToken("mock-token", Instant.now(),
                                        Instant.now().plusSeconds(3600), claims);
                        OidcUser mockUser = new DefaultOidcUser(
                                        AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN"),
                                        idToken);

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        mockUser, null, mockUser.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                }
        }
}
