"use client"

import { apiFetch } from "@/lib/api"
import { Environment } from "@/types"

export async function getEnvironments() {
    const data = await apiFetch<{ environments: Environment[] }>("/environments")
    return data.environments
}


export async function createEnvironment(data: { name: string; type: string }) {
    await apiFetch("/environments", {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function getEnvironment(name: string) {
    return await apiFetch<Environment>(`/environments/${name}`)
}

export async function updateEnvironment(name: string, data: Partial<Environment>) {
    await apiFetch<Environment>(`/environments/update/${name}`, {
        method: "PUT",
        body: JSON.stringify(data),
    })
}
