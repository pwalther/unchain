"use client"

import { apiFetch } from "@/lib/api"
import { Strategy, StrategyDefinition, StrategyParameterDefinition } from "@/types"

export async function getStrategies() {
    const data = await apiFetch<{ strategies: StrategyDefinition[] }>("/strategies")
    return data.strategies
}

export async function createStrategyDefinition(data: {
    name: string;
    description?: string;
    parameters?: StrategyParameterDefinition[];
}) {
    await apiFetch("/strategies", {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function updateStrategyDefinition(strategyName: string, data: {
    parameters?: StrategyParameterDefinition[];
}) {
    await apiFetch(`/strategies/${strategyName}`, {
        method: "PUT",
        body: JSON.stringify({ ...data, name: strategyName }),
    })
}

export async function createStrategyForFeature(
    projectId: string,
    featureName: string,
    environment: string,
    strategy: Partial<Strategy>
) {
    await apiFetch(`/projects/${projectId}/features/${featureName}/environments/${environment}/strategies`, {
        method: "POST",
        body: JSON.stringify(strategy),
    })
}

export async function getStrategyForFeature(
    projectId: string,
    featureName: string,
    environment: string,
    strategyId: string
) {
    return await apiFetch<Strategy>(`/projects/${projectId}/features/${featureName}/environments/${environment}/strategies/${strategyId}`)
}

export async function updateStrategyForFeature(
    projectId: string,
    featureName: string,
    environment: string,
    strategyId: string,
    strategy: Partial<Strategy>
) {
    await apiFetch(`/projects/${projectId}/features/${featureName}/environments/${environment}/strategies/${strategyId}`, {
        method: "PUT",
        body: JSON.stringify(strategy),
    })
}

export async function deleteStrategyFromFeature(
    projectId: string,
    featureName: string,
    environment: string,
    strategyId: string
) {
    await apiFetch(`/projects/${projectId}/features/${featureName}/environments/${environment}/strategies/${strategyId}`, {
        method: "DELETE",
    })
}
