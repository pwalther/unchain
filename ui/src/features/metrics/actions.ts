"use client"

import { apiFetch } from "@/lib/api"
import { ProjectMetrics } from "@/types"

export async function getProjectMetrics(projectId: string) {
    return await apiFetch<ProjectMetrics>(`/projects/${projectId}/metrics`)
}
