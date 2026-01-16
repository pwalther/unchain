# Unchain - Feature Flag Management

## Server-Side Events (SSE) Support

Unchain supports streaming feature flag updates to clients using Server-Side Events (SSE). This allows for near real-time updates without polling.

### Enabling SSE in Client SDK

To enable SSE in the Java Client SDK, set the `sseEnabled` flag in `UnchainConfig`:

```java
UnchainConfig config = UnchainConfig.builder()
    .apiUrl("http://localhost:8080")
    .apiKey("your-api-key")
    .projectId("default")
    .environment("production")
    .sseEnabled(true) // Enable SSE
    .build();

UnchainClient client = new UnchainClient(config);
```

When enabled, the client will connect to `/projects/{projectId}/features/stream` and receive updates automatically. The polling mechanism remains active as a fallback and safety sync (default every 120s).

### Architecture

- **Backend**: Exposes `/projects/{projectId}/features/stream`.
- **Event Handling**:
  - Local updates (via API/UI) trigger SSE notifications immediately.
  - External updates (e.g. from other instances) can be ingested via `ActiveMQFeatureEventHandler` (requires ActiveMQ setup) or by implementing valid logic in `FeatureEventHandler`.
