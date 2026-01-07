# Feature Usage Metrics

Unchain provides a built-in mechanism to track how often your feature flags are being evaluated in your applications. This data is used to provide insights into feature usage and to automatically identify "stale" features that may be ready for removal from the codebase.

## How it Works

Metrics collection is a collaborative process between the **Unchain Client SDK** and the **Unchain Admin API**.

### 1. Client-Side Tracking
When your application calls `isEnabled()` or `getVariant()`, the SDK performs the following:
- It checks if the feature has the `impressionData` flag enabled.
- If enabled, it increments an in-memory counter for that specific feature within the current environment and project context.
- To minimize network overhead, hits are aggregated locally.

### 2. Periodic Reporting
The Client SDK runs a background task (defaulting to every 5 minutes) that:
- Collects the aggregated hit counts since the last report.
- Sends a `POST /metrics` request to the Admin API.
- Clears the local counters upon successful transmission.

### 3. Backend Persistence
The Admin API receives the metrics report and updates the `feature_metrics` table. It tracks the total usage count and the timestamp of the last report for each feature/environment combination.

### 4. Staleness Detection
A scheduled task in the Admin API evaluates features periodically. If a feature has not been evaluated in any production-like environment for a specific duration, it is marked as **Stale**. This serves as a trigger for developers to clean up the code.

---

## Configuration

### Admin API Settings
You can customize the staleness detection logic in your `application.yaml`:

```yaml
unchain:
  features:
    # Number of days of inactivity before a feature is considered stale
    stale-after-days: 3
    # Cron expression for when to run the staleness check
    stale-check-cron: "0 0 3 * * *"
```

### SDK Settings
- **Impression Data toggle**: Metrics are only collected for features where **Impression Data** is toggled **ON** in the Admin UI. This allows you to control the volume of data generated and focus on important features.
- **Reporting Interval**: Currently, the Java SDK defaults to a 5-minute reporting interval to balance data freshness and network efficiency.

---

## Considerations & Best Practices

### Performance
- **Low Overhead**: Because the SDK aggregates hits in memory and uses a background thread for reporting, metrics collection has negligible impact on the performance of your application's hot path (the `isEnabled` call).
- **Network Traffic**: Reporting occurs periodically, not per evaluation. This ensures your application doesn't flood the Admin API with requests.

### Data Privacy
- Unchain metrics only track **counts**. No user-specific data or context values (like IDs or emails) are sent to the Admin API as part of standard usage metrics. 

### Feature Lifecycle Management
- **Manage the Stale Flag**: You can now manually control the staleness of a feature in the **Metadata** section of the feature detail view. 
  - **Auto-Detection**: The system will automatically mark features as stale based on inactivity (see configuration).
  - **Manual Override**: Use the toggle in the UI to mark a feature as stale early if you know it's no longer needed, or to clear the stale status after your cleanup is complete.
- **Visual Indicators**: Stale features are marked with a prominent amber **Stale** badge and an alert icon in the features list and detail views.
- **Cleanup**: Once a feature is marked stale and you've confirmed it's no longer needed, use the **Archive** function to remove it from the active list.

### Impression Data Control
- **Manual Toggle**: The **Impression Data** switch in the feature detail view controls whether SDKs should report matches for this feature.
- **Performance Optimization**: Disable impression data for features that don't require tracking to reduce network overhead and storage usage.

### Precision
- Metrics are "best-effort". If an application crashes before the 5-minute reporting window, the hits for that window may be lost. For strictly critical business analytics, we recommend using dedicated analytics tools in tandem with feature flag evaluations.
