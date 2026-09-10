const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

async function request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;

    const config = {
        headers: {
            "Content-Type": "application/json",
            ...options.headers,
        },
        ...options,
    };
    const response = await fetch(url, config);

    if (!response.ok) {
        const errorBody = await response.text().catch(() => "");
        throw new Error(
            `API Error ${response.status}: ${errorBody || response.statusText}`
        );
    }

    if (response.status === 204) return null;

    return response.json();
}
export const apiClient = {
    get: (endpoint) => request(endpoint),
    post: (endpoint, data) =>
        request(endpoint, { method: "POST", body: JSON.stringify(data) }),
    patch: (endpoint, data) =>
        request(endpoint, { method: "PATCH", body: JSON.stringify(data) }),
    delete: (endpoint) => request(endpoint, { method: "DELETE" }),
};     