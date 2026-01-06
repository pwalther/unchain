"use client"

import { apiFetch } from "@/lib/api"
import { Project } from "@/types"

export async function getProjects() {
    const data = await apiFetch<{ projects: Project[] }>("/projects")
    return data.projects
}


export async function createProject(data: { id: string; name: string; description?: string }) {
    await apiFetch("/projects", {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function deleteProject(projectId: string) {
    await apiFetch(`/projects/${projectId}`, {
        method: "DELETE",
    })
}
