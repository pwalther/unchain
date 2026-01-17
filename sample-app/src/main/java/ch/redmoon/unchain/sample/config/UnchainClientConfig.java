package ch.redmoon.unchain.sample.config;

import ch.redmoon.unchain.client.UnchainClient;
import ch.redmoon.unchain.client.UnchainConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.function.Supplier;

@Configuration
@Profile("demo")
public class UnchainClientConfig {

    @Value("${unchain.api.url:http://localhost:8080}")
    private String apiUrl;

    @Value("${unchain.environment:development}")
    private String environment;

    @Value("${unchain.project:default}")
    private String project;

    @Bean
    public UnchainClient unchainClient() {
        // In a real application, the token would be retrieved from an Identity Provider
        // (IdP)
        // using Client Credentials flow or a similar OIDC/OAuth2 mechanism.
        // For the demo profile, we return a mock token.
        Supplier<String> tokenSupplier = () -> {
            // Logic to fetch token from Keycloak/Okta/Auth0 would go here
            return "demo-token-123";
        };

        UnchainConfig config = UnchainConfig.builder()
                .apiUrl(apiUrl)
                .tokenSupplier(tokenSupplier)
                .environment(environment)
                .projects(List.of(project))
                .refreshIntervalSeconds(15) // Frequent refresh for demo purposes
                .sseEnabled(true)
                .build();

        return new UnchainClient(config);
    }
}
