# Unchain - Feature Flag Management

Unchain is a feature flag management platform that allows you to manage your feature flags in a centralized and secure way. It provides a web UI and an API to manage your feature flags. It comes with a client sdk for java and a sample app to show how to use the API.



# Documentation
See the [admin-api](admin-api/README.MD) and [client-sdk](client-sdk/README.MD) for more information how to adapt to your needs.
There are screenshots [screenshots](documentation/screenshots) and documentation on the underlying concepts used by unchain in the [documentation](documentation) folder.


# API
OpenAPI specification: [openapi.yaml](admin-api/src/main/resources/openapi.yaml)

# Sample

There is a sample app in the [sample-app](sample-app) folder that shows how to use the API.

# UI

Welcome to Unchain, a powerful and flexible feature flag management platform. This documentation provides an overview of the key functionalities available in the Unchain UI.

## 1. Dashboard

The **Dashboard** serves as the central hub for monitoring your feature flag ecosystem.

-   **Summary Statistics**: View high-level metrics such as the total number of projects, feature flags, active flags, and stale flags.
-   **Project Overview**: Get a quick glimpse of your projects and their health status.
-   **Recent Activity**: specific audit logs or recent changes are displayed here to help you stay updated on the latest modifications.

## 2. Features

The **Features** section is where you manage the lifecycle of your feature flags (toggles).

-   **Create Flags**: Create new feature flags with specific types (e.g., Release, Experiment, Operational).
-   **Toggle Status**: Enable or disable flags across different environments.
-   **Strategy Configuration**: Assign rollout strategies to your flags (see 'Strategies' section for more details).
-   **Search & Filter**: Quickly find flags by name, project, or status.
-   **Archiving**: Archive flags that are no longer in use.

## 3. Metrics

The **Metrics** page provides insights into how your feature flags are being evaluated.

-   **Usage Data**: Visualize evaluation counts over time to understand feature adoption and usage patterns.
-   **Client Versions**: See which SDK versions your applications are using.
-   **Stale Flags**: Identify flags that haven't been evaluated recently, helping you keep your codebase clean.

## 4. Change Requests

**Change Requests** add a governance layer to your feature flag management, especially for protected environments.

-   **Approval Workflow**: Changes to flags in protected environments (like Production) require a Change Request.
-   **Draft & Review**: Create drafts of your changes and submit them for review.
-   **Approvals**: Authorized users can review and approve change requests. *Note: You cannot approve your own change requests unless in a specific demo mode.*
-   **Scheduling**: Schedule approved changes to be applied at a specific future time.
-   **Application**: Once approved (and fast-forwarded if scheduled), changes can be applied to the target environment.

## 5. Projects

Organize your feature flags into **Projects**.

-   **Isolation**: Projects provide a way to group related flags (e.g., by team or application).
-   **Management**: Create, update, and delete projects.
-   **Project-Specific Settings**: Configure project-level defaults and permissions.

## 6. Strategies

**Strategies** define *how* a feature flag is released to your users.

-   **Standard Strategies**:
    -   **Standard**: The feature is enabled for everyone.
    -   **UserIDs**: Enable for specific user IDs.
    -   **Gradual Rollout**: Roll out to a percentage of users (randomly or sticky by session/user ID).
    -   **IP Addresses**: Enable for specific IP ranges.
-   **Custom Strategies**: Define custom strategies with specific parameters to fit your unique rollout requirements.

## 7. Environments

Manage the different stages of your deployment pipeline in the **Environments** section.

-   **Setup**: Define environments like Development, Staging, and Production.
-   **Protection**: Mark environments as "Protected" to enforce Change Request workflows and prevent accidental changes.

## 8. Contexts

**Context Fields** allow you to define the variables available for your targeting strategies.

-   **Standard Fields**: Common fields like `userId`, `appName`, and `environment`.
-   **Custom Fields**: Define custom context fields (e.g., `tenantId`, `region`, `planType`) that you can use in your feature flag constraints.
-   **Legal Values**: Restrict context fields to a specific set of allowed values.

## 9. History

The **History** log provides a distinct audit trail of changes in the system.

-   **Audit Logs**: View a chronological record of who changed what and when.
-   **Filtering**: Filter logs by project, feature, or user to investigate specific events.

## 10. Settings

Configure global settings for your Unchain instance.

-   **User Profile**: Manage your user account details.
-   **System Configuration**: Configure global system preferences and integrations.
    -   **Webhooks**: Set up webhook URLs to receive real-time events for feature changes and change requests. This allows integration with external tools like MS Teams for notifications or other automation pipelines.
    -   **Retention Policies**: Configure housekeeping rules for audit logs and stale feature flags.


