import { useQuery } from "@tanstack/react-query"
import { apiFetch } from "@/lib/api"

export interface User {
    authenticated: boolean
    name?: string
    email?: string
    username?: string
    picture?: string
    authorities?: string[]
}

export function useAuth() {
    return useQuery<User>({
        queryKey: ["auth", "me"],
        queryFn: () => apiFetch<User>("/api/me"),
        retry: 3,
        retryDelay: 1000, // Retry every second instead of exponential backoff
    })
}
