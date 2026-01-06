export const getApiUrl = () => process.env.NEXT_PUBLIC_API_URL || "https://192.168.1.36:8888";

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
        const error = new Error(data.message || res.statusText || "API request failed") as any;
        error.status = res.status;
        throw error;
    }

    if (res.status === 204 || !text) {
        return {} as T;
    }

    return data;
}

