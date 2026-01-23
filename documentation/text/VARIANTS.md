# Feature Variants in Unchain

Feature Variants allow you to define multiple versions of a feature flag beyond a simple "on/off" toggle. This enables advanced use cases such as A/B testing, multivariate testing, and gradual rollouts of new configurations.

## Core Concepts

### 1. Variants
A variant is a specific version of a feature. Each variant consists of:
- **Name:** A unique identifier (e.g., `control`, `test-a`, `test-b`).
- **Weight:** An integer defining the rollout percentage.
- **Payload:** Optional data (String, JSON, or Number) passed to the application.
- **Stickiness:** Defines which context property is used to ensure a user always sees the same variant.

### 2. Selection Logic
When an application requests a variant, the Unchain SDK performs a deterministic calculation:
1. **Identify the Stickiness Key:** Defaults to `userId`.
2. **Hash the Key:** The SDK hashes the combination of the feature name and the stickiness value.
3. **Calculate Bucket:** The hash is mapped to one of **1,000 discrete buckets** (internally represented as 0 to 999).
4. **Assign Variant:** The value is checked against the cumulative weights of the defined variants.

## The Logic of Weights (Scale 0-1000)

In Unchain, the total weight for all variants of a feature or strategy should sum up to **1000**. While many systems use a base of 100 (percentage), Unchain uses 1000 for three reasons:

### A. High Precision (The 0.1% Increment)
A scale of 100 only allows for 1% increments. For high-traffic applications, a 1% change might affect hundreds of thousands of users.
- **0-100 Scale:** Minimum rollout is 1%.
- **0-1000 Scale:** Minimum rollout is **0.1%**.
This allows for much "safer" initial rollouts of experimental features.

### B. Fair Multi-Variant Distribution
When splitting traffic between multiple variants, a base of 100 often leads to rounding errors and "leftover" percentages.
- **Scenario:** Split traffic equally between 3 variants.
    - **Base 100:** 33 + 33 + 33 = 99%. (1% of traffic is "lost" or goes to a default).
    - **Base 1000:** 333 + 333 + 334 = 1000. (The distribution is significantly more accurate).

### C. Industry Alignment
Large-scale feature management platforms (such as Unleash, Optimizely, and LaunchDarkly) frequently use a 1,000 or 10,000 base for their bucketing logic to cater to enterprise precision requirements.

## Configuration Levels

Variants can be defined at two levels in Unchain:

### 1. Feature Level (Default)
Defined on the feature flag itself. These are the "fallback" variants used if no specific environment-level strategy overrides them.

### 2. Strategy Level (Overrides)
Defined within a specific strategy assignment (e.g., "Rollout Strategy" in Production). If a strategy matches and has variants defined, those variants **completely override** the feature-level variants for that specific request. 

## Payloads
Payloads allow you to decouple your logic from the flag state. Instead of hardcoding values in your code, you can serve different configurations:
- **String:** e.g., `button-color: "blue"` vs `button-color: "red"`.
- **JSON:** e.g., `{"discount": 20, "onItems": ["shoes", "hats"]}`.
- **Number:** e.g., `max_timeout: 500`.

This makes the platform a dynamic configuration engine, not just a toggle switch.
