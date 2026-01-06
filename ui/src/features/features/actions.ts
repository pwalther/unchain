"use client"

import { apiFetch } from "@/lib/api"
import { Feature } from "@/types"

export async function getFeatures(projectId: string = "default") {
    const data = await apiFetch<{ features: Feature[] }>(`/projects/${projectId}/features`)
    return data.features
}

export async function getFeature(projectId: string, featureName: string) {
    return await apiFetch<Feature>(`/projects/${projectId}/features/${featureName}`)
}


export async function createFeature(projectId: string, data: { name: string; type: string; description?: string }) {
    await apiFetch(`/projects/${projectId}/features`, {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function toggleFeature(projectId: string, featureName: string, environment: string, enabled: boolean) {
    const endpoint = enabled ? "on" : "off"
    await apiFetch(`/projects/${projectId}/features/${featureName}/environments/${environment}/${endpoint}`, {
        method: "POST",
    })
}

export async function deleteFeature(projectId: string, featureName: string) {
    await apiFetch(`/projects/${projectId}/features/${featureName}`, {
        method: "DELETE",
    })
}
