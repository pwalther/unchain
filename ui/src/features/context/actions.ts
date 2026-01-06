"use client"

import { apiFetch } from "@/lib/api"
import { ContextField } from "@/types"

export async function getContextFields() {
    return await apiFetch<ContextField[]>("/context")
}
