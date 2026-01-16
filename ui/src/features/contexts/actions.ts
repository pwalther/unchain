import { apiFetch } from "@/lib/api"
import { ContextField } from "@/types"

export async function getContexts() {
    return apiFetch<ContextField[]>("/contexts")
}

export async function createContext(data: ContextField) {
    return apiFetch<void>("/contexts", {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function deleteContext(name: string) {
    return apiFetch<void>(`/contexts/${name}`, {
        method: "DELETE",
    })
}
