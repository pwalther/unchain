# Unchain Admin API

The **Unchain Admin API** is the backend service for the Unchain feature flag management platform. It provides a robust, secure, and extensible REST API for managing projects, feature flags, environments, and strategies.

## Features

- **Feature Flag Management**: Full lifecycle control over feature flags.
- **Feature Variants**: Define multiple variations for a feature flag with weighted rollouts (A/B testing).
- **Environment Support**: Manage flags across multiple environments (Development, Production, etc.).
- **Strategy Engine**: Define and apply complex rollout strategies and constraints.
- **Audit Logging**: Automatic tracking of all changes with detailed state snapshots.
- **OIDC Authentication**: Integration with OpenID Connect providers.
- **Extensible Authorization**: Custom provider support for integrating with homegrown security solutions.
- **API-First Design**: Built using OpenAPI 3.0 specifications.
- **Global UI Banner**: Configurable announcement banner for communicating with users.

## Technologies

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Data JPA** (Hibernate)
- **Spring Security** (OAuth2/OIDC)
- **Liquibase** (Database Migrations)
- **H2 / PostgreSQL** (Database support)
- **Lombok**
- **MapStruct**

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+

### Running the Application

1. Clone the repository.
2. Navigate to the `admin-api` directory.
3. Run the application using Maven:

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Configuration

The application is configured via `src/main/resources/application.yaml`. Key configuration sections include:

### OIDC Authentication

Configure your OIDC provider using the following environment variables or by editing the YAML:

- `OIDC_CLIENT_ID`: Your OIDC client ID.
- `OIDC_CLIENT_SECRET`: Your OIDC client secret.
- `OIDC_ISSUER_URI`: The issuer URI of your provider (e.g., Keycloak, Auth0).

### Custom Authorization

Unchain allows you to plug in your own authorization logic. Implement the `AuthorizationProvider` interface and configure the class path:

```yaml
unchain:
  security:
    authorization-provider-class: ch.redmoon.unchain.security.TrustAllAuthorizationProvider
```

## Event Monitoring & Alerting

Unchain features a central event system that triggers notifications whenever critical actions occur, such as enabling/disabling feature flags or managing change requests.

### Extensibility

Developers can extend the event system by implementing the `UnchainEventObserver` interface. This allows for custom integrations with third-party tools (e.g., Slack, PagerDuty, or internal logging systems).

### Webhook Alerts

The application includes a built-in `WebhookEventObserver` that can be configured to send event data to a custom URL via HTTP POST. Webhooks are executed **asynchronously** to prevent blocking the main application flow.

#### Retry Mechanism
Webhooks include an automatic retry mechanism with **exponential backoff**. If a delivery fails (e.g., network timeout or server error), the system will retry up to 5 times by default.

**Configuration:**

```yaml
unchain:
  webhook:
    enabled: true
    url: ${WEBHOOK_URL:http://your-service.com/webhook}
    retry:
      max-attempts: 5
      initial-delay-ms: 1000
      multiplier: 2.0
```

### Advanced HTTP Client Configuration

The webhook system uses a Spring bean named `unchainWebhookRestTemplate`. You can provide your own implementation of this bean to configure custom timeouts, TLS/mTLS, or interceptors:

```java
@Bean(name = "unchainWebhookRestTemplate")
public RestTemplate customRestTemplate(RestTemplateBuilder builder) {
    return builder
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            // ... add TLS or other custom settings
            .build();
}
```

### Database

By default, the application uses an in-memory H2 database. For production, update the `spring.datasource` properties to point to your persistent database (e.g., PostgreSQL).

## Housekeeping

The application includes automated background tasks to keep the database clean:

- **Applied Change Requests**: Automatically removed after a configurable period (default: 1 month).
- **Audit Logs**: Automatically purged after a configurable period (default: 1 year).

**Configuration:**

```yaml
unchain:
  housekeeping:
    retention-period-months: 1      # Retention for applied change requests
    cron: "0 0 1 * * *"             # Schedule for change request cleanup
    audit-log-retention-years: 1    # Retention for audit log entries
    audit-log-cron: "0 0 2 * * *"   # Schedule for audit log cleanup
```


 ## Change Requests
 
 Change Requests allow for a controlled workflow when modifying feature flags in protected environments. A change request follows these states:
 
 - **Draft**: The initial state where the change is being prepared.
 - **In review**: The change has been submitted and is waiting for peer or administrator approval.
 - **Approved**: Enough approvals have been received, and the change is ready to be applied.
 - **Applied**: The changes have been successfully committed to the target environment.
 - **Cancelled**: The change request was withdrawn by the creator.
 - **Rejected**: The change request was reviewed and explicitly declined.
 
 Note: Entities (Projects, Environments, Features) referenced in a pending change request (**Draft**, **In review**, or **Approved**) cannot be renamed, archived, or deleted until the request is finalized.
 
 ## Audit Logging


All modifications to core entities (Features, Projects, Strategies, Change Requests, Segments, Environments) are automatically logged in the `audit_log` table. Each entry includes:
- Entity Type and ID
- Action performed (CREATED, UPDATED, DELETED)
- User who performed the action
- Timestamp
- JSON representation of the entity state

## Feature Variants

Unchain supports **Variants**, which allow you to define multiple versions of a feature flag. This is useful for:
- **A/B Testing**: Testing different UI elements or backend algorithms.
- **Gradual Rollouts**: Transitioning from one version to another.
- **Personalization**: Serving different content based on user context.

### Configuration
Variants can be configured at two levels:
1. **Feature Level**: Default variants that apply across all environments.
2. **Strategy Level**: Overrides defined within a specific strategy assignment, allowing environment-specific variant definitions.

### Selection Logic
Each variant has a **Weight** (out of 1000). The client SDK uses these weights to deterministically assign a variant to a user based on **Stickiness**. By default, `userId` is used for stickiness, ensuring a consistent experience for the user.

### Payloads
Variants can include an optional **Payload** (String, JSON, or Number), allowing you to pass configuration data directly to your application logic.

## API Documentation

- **OpenAPI Spec**: Available at `/api-docs`
- **Swagger UI**: Accessible at `/swagger-ui/index.html` (when enabled)

## License

Apache License 2.0
