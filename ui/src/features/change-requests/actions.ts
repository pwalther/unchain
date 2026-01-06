import { apiFetch } from "@/lib/api"
import { ChangeRequest } from "@/types"

export async function getChangeRequests(projectId: string): Promise<ChangeRequest[]> {
    return await apiFetch<ChangeRequest[]>(`/projects/${projectId}/change-requests`)
}

export async function getChangeRequest(projectId: string, id: number): Promise<ChangeRequest> {
    return await apiFetch<ChangeRequest>(`/projects/${projectId}/change-requests/${id}`)
}

export async function createChangeRequest(projectId: string, data: {
    title: string
    environment: string
    changes: {
        feature: string
        action: string
        payload: any
    }[]
    scheduledAt?: string
}): Promise<ChangeRequest> {
    return await apiFetch<ChangeRequest>(`/projects/${projectId}/change-requests`, {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function approveChangeRequest(projectId: string, changeRequestId: number): Promise<void> {
    await apiFetch(`/projects/${projectId}/change-requests/${changeRequestId}/approvals`, {
        method: "POST",
    })
}

export async function updateChangeRequestState(projectId: string, changeRequestId: number, state: string): Promise<void> {
    await apiFetch(`/projects/${projectId}/change-requests/${changeRequestId}/state`, {
        method: "PUT",
        body: JSON.stringify({ state }),
    })
}

export async function addChangesToRequest(projectId: string, id: number, data: {
    changes: {
        feature: string
        action: string
        payload: any
    }[]
}): Promise<ChangeRequest> {
    return await apiFetch<ChangeRequest>(`/projects/${projectId}/change-requests/${id}`, {
        method: "PATCH",
        body: JSON.stringify(data),
    })
}
