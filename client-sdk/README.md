# Unchain Java Client SDK

The Unchain Java Client SDK allows your application to interact with the Unchain Feature Flag platform.

## Features
- **Deterministic Evaluation:** Feature flags are evaluated locally after a periodic background refresh.
- **Rollout Strategies:** Supports default, gradual rollout, and user-ID based strategies.
- **Feature Variants:** Multivariate support with weighted distribution and stickiness.
- **Resiliency:** Background updates ensure the application stays fast even if the API is down.

## Error Handling

The SDK uses SLF4J for logging and follows a "fail-silent" approach for feature evaluations to ensure your application remains stable even if there are connectivity issues.

### Logging
You should provide an SLF4J implementation (like Logback or Log4j2) to see SDK logs.
- `ERROR`: Network failures, unauthorized access, or parsing errors during background refresh.
- `DEBUG`: Information about successful refreshes and cache updates.

### Cache Policy
If the background refresh fails:
1. The SDK continues to use the **last successfully fetched** state from the local cache.
2. Applications continue to work with stale data until the connection is restored.
3. If the cache is empty and the initial fetch fails, all `isEnabled` calls will return `false`.

### Exceptions
The SDK defines `ch.redmoon.unchain.client.exception.UnchainException` for critical initialization errors. However, common runtime operations (like `isEnabled`) are designed to be exception-free, returning safe defaults on failure.

## Configuration

```java
UnchainConfig config = UnchainConfig.builder()
    .apiUrl("http://your-unchain-instance.com/api/admin")
    .tokenSupplier(() -> "your-api-token")
    .environment("production")
    .projects(List.of("default"))
    .refreshIntervalSeconds(60)
    .build();

UnchainClient client = new UnchainClient(config);
```
