# Unchain Sample Application

This is a demonstration application that showcases how to integrate the `unchain-client-sdk` into a Spring Boot project.

## How it Works

1. **Configuration:** The `UnchainClientConfig` class bean-initializes the `UnchainClient`. It includes a `Supplier<String>` for the API token.
    - *Note:* In this demo, the supplier returns a hardcoded string. In a production environment, this lambda should contain logic to fetch an OIDC token from your Identity Provider (e.g., Keycloak or Okta) using the Client Credentials flow.
2. **Real-time Updates:** The SDK is configured to refresh its local cache every 15 seconds.
3. **Evaluation:** The `FeatureCheckController` uses the SDK to determine if `demo-feature` is enabled and which variant should be served for a given `userId`.
4. **UI:** A simple Tailwind-based HTML page provides an interactive way to test the flag and see how different `userId` values might trigger different variants (based on weighted rollout and stickiness).

## Prerequisites

- The **unchain-admin-api** must be running (usually on port 8080).
- You should have at least one feature named `demo-feature` created in the admin UI.

## Running the App

```bash
mvn spring-boot:run -pl sample-app
```

Once started, visit [http://localhost:8081](http://localhost:8081).
