export const getApiUrl = () => process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
        super(message);
        this.status = status;
        this.name = "ApiError";
    }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const baseUrl = getApiUrl();

    const res = await fetch(`${baseUrl}${endpoint}`, {
        ...options,
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...options.headers,
        },
    });

    const text = await res.text();
    const data = text ? JSON.parse(text) : {};

    if (!res.ok) {
        if (res.status === 401) {
            // Redirect to OIDC login on backend
            window.location.href = `${baseUrl}/oauth2/authorization/oidc`;
            return {} as T;
        }
        throw new ApiError(data.message || res.statusText || "API request failed", res.status);
    }

    if (res.status === 204 || !text) {
        return {} as T;
    }

    return data;
}

