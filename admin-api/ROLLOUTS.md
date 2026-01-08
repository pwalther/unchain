# Gradual Rollouts & Hashing Strategy

This document outlines how the **Unchain** platform and its SDKs implement gradual rollouts (percentage-based releases) and stickiness.

## 1. Overview

Gradual rollouts allow you to release a feature to a specific percentage of your user base (e.g., 10%, 50%, 100%). A critical requirement for this is **Stickiness**: if a user falls into the "10% enabled" bucket today, they must remain in that bucket tomorrow, ensuring a consistent user experience.

We achieve this using deterministic hashing. We do not store "state" for every user; instead, we compute their status on-the-fly using a hashing algorithm.

## 2. The Hashing Algorithm: MurmurHash3

We use **MurmurHash3 (32-bit, x86 variant)** as our hashing function.

### Why MurmurHash?
1.  **Speed**: It is extremely fast to compute, adding negligible overhead to flag evaluation.
2.  **Deterministic**: The same input always yields the exact same hash.
3.  **Distribution**: It has excellent avalanche properties, meaning small changes in input result in vastly different hash values. This ensures users are evenly distributed across the 0-100 range.
4.  **Standard**: It is widely available in all major languages (Java, Node, Python, Go, etc.), ensuring server-side and client-side evaluations match perfectly.

## 3. Implementation Details

The evaluation process follows these steps:

### Step 1: Inputs
To evaluate a rollout, we need:
-   **Stickiness Key (`stickinessId`)**: The unique identifier for the subject (e.g., User ID, Session ID, IP Address).
-   **Group ID (`groupId`)**: Typically the **Feature Name** or a specific ID assigned to the rollout strategy. This ensures that a user who is in the "10%" bucket for *Feature A* is not arguably correlated to be in the "10%" bucket for *Feature B*.
-   **Rollout Percentage (`p`)**: An integer from 0 to 100.

### Step 2: Construct the Hashing Key
We concatenate the Group ID and the Stickiness Key with a colon separator:

```text
data = groupId + ":" + stickinessId
```

*Example: "new-login-flow:user-12345"*

### Step 3: Compute Hash
We calculate the Murmur3 (32-bit x86) hash of the UTF-8 bytes of this string.

```java
long hash = Murmur3.hash_x86_32(data.getBytes("UTF-8"));
```

### Step 4: Normalization (The "Bucket")
We map the potentially large hash integer to a value between 1 and 100.

```java
// Modulo 100 gives 0-99. Add 1 to get 1-100.
int normalized = (hash % 100) + 1;
```

### Step 5: Evaluation
We compare the normalized bucket value against the configured rollout percentage.

```java
isEnabled = normalized <= rolloutPercentage;
```

-   If the normalized value is **35** and the rollout is set to **50%**, the feature is **ENABLED**.
-   If the normalized value is **60** and the rollout is set to **50%**, the feature is **DISABLED**.

## 4. Stickiness Explained

**Stickiness** is the property that ensures the same user always gets the same result.

Because the calculation relies *only* on the `stickinessId` (which shouldn't change for a user) and the `groupId` (which is constant for the feature), the `Murmur3` hash will always return the exact same number.

-   **User A (`user-12345`)** hashes to **Bucket 42**.
-   As long as the rollout percentage is **42% or higher**, User A sees the feature.
-   If you increase rollout from 10% -> 20% -> 50%, User A will eventually be included.
-   If you decrease rollout, User A might lose access, but the process remains deterministic.

### Context Fields
Stickiness can be based on any field in the context:
-   **`userId`**: Stable across devices and sessions (recommended).
-   **`sessionId`**: Stable only for the current browser session.
-   **`remoteAddress`**: Stable for the IP (useful for anonymous traffic).
