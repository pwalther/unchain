"use client"

import { apiFetch } from "@/lib/api"
import { Segment } from "@/types"

export async function getSegments() {
    const data = await apiFetch<{ segments: Segment[] }>("/segments")
    return data.segments
}

export async function createSegment(data: Partial<Segment>) {
    await apiFetch("/segments", {
        method: "POST",
        body: JSON.stringify(data),
    })
}

export async function deleteSegment(segmentId: number) {
    await apiFetch(`/segments/${segmentId}`, {
        method: "DELETE",
    })
}
