export interface Project {
    id: string;
    name: string;
    description?: string;
    health?: number;
    updatedAt?: string;
}

export interface Feature {
    name: string;
    description?: string;
    type: string;
    project: string;
    stale: boolean;
    createdAt: string;
    lastSeenAt?: string;
    impressionData: boolean;
    environments?: FeatureEnvironment[];
    variants?: Variant[];
}

export interface FeatureEnvironment {
    name: string;
    enabled: boolean;
    strategies: Strategy[];
}

export interface Constraint {
    contextName: string;
    operator: 'IN' | 'NOT_IN' | 'STR_ENDS_WITH' | 'STR_STARTS_WITH' | 'STR_CONTAINS' | 'NUM_EQ' | 'NUM_GT' | 'NUM_LT' | 'DATE_AFTER' | 'DATE_BEFORE' | 'SEMVER_EQ' | 'SEMVER_GT' | 'SEMVER_LT';
    values?: string[];
    caseInsensitive?: boolean;
    inverted?: boolean;
}

export interface Strategy {
    id?: string;
    name: string;
    description?: string;
    title?: string;
    parameters?: StrategyParameter[];
    constraints?: Constraint[];
    segments?: number[];
    sortOrder?: number;
    disabled?: boolean;
    variants?: Variant[];
}

export interface StrategyParameter {
    name: string;
    type: string;
    description?: string;
    required: boolean;
    value?: string;
}

export interface StrategyDefinition {
    name: string;
    description?: string;
    editable: boolean;
    parameters: StrategyParameterDefinition[];
}

export interface StrategyParameterDefinition {
    name: string;
    type: 'string' | 'percentage' | 'list' | 'number' | 'boolean';
    description?: string;
    required: boolean;
}

export interface Segment {
    id: number;
    name: string;
    description?: string;
    constraints: Constraint[];
}

export interface Environment {
    name: string;
    type: string;
    enabled: boolean;
    protected: boolean;
    sortOrder?: number;
    projectCount?: number;
    enabledToggleCount?: number;
    requiredApprovals?: number;
}

export interface ChangeRequest {
    id: number;
    title: string;
    state: 'Draft' | 'In review' | 'Approved' | 'Applied' | 'Cancelled' | 'Rejected';
    environment: string;
    project: string;
    minApprovals: number;
    createdBy: {
        username: string;
    };
    features: ChangeRequestFeature[];
    scheduledAt?: string;
}

export interface ChangeRequestFeature {
    name: string;
    changes: ChangeRequestChange[];
}

export interface ChangeRequestChange {
    action: string;
    payload: unknown;
}

export interface ContextField {
    name: string;
    description?: string;
    stickiness?: boolean;
    sortOrder?: number;
    legalValues?: LegalValue[];
}

export interface LegalValue {
    value: string;
    description?: string;
}

export interface Variant {
    name: string;
    weight: number;
    stickiness?: string;
    payload?: VariantPayload;
}

export interface VariantPayload {
    type: 'string' | 'json' | 'number';
    value: string;
}

export interface ProjectMetrics {
    featureActivity: FeatureActivity[];
    clientVersions: ClientVersionUsage[];
    staleFeatures: StaleFeature[];
}

export interface FeatureActivity {
    name: string;
    count: number;
    lastUsage: string;
}

export interface ClientVersionUsage {
    version: string;
    count: number;
}

export interface StaleFeature {
    name: string;
    lastUsage: string | null;
}

export interface DashboardSummary {
    projectCount: number;
    featureCount: number;
    activeFeatureCount: number;
    staleFeatureCount: number;
    projects: ProjectDashboardItem[];
    recentChanges: AuditLogItem[];
}

export interface ProjectDashboardItem {
    id: string;
    name: string;
    featureCount: number;
    health: number;
}

export interface AuditLogItem {
    id: number;
    entityType: string;
    entityId: string;
    action: string;
    changedBy: string;
    changedAt: string;
    data?: string;
}
