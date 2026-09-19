import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "";

const instance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
    },
});

instance.interceptors.response.use(
    (response) => {
        if (response.status === 204) return { ...response, data: null };
        return response;
    },
    (error) => {
        const status = error.response?.status;
        const body = error.response?.data;
        const apiError = new Error(
            typeof body === "string" ? body : body?.message || error.message
        );
        apiError.status = status;
        // Readable machine code (NOT_FOUND, METHOD_NOT_ALLOWED, ...) for fine-grained handling.
        apiError.code = body?.code;
        apiError.data = body;
        throw apiError;
    }
);

// JWT token: kept for the useAuth hook and future protected routes.
const TOKEN_KEY = "access_token";

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
}

export const apiClient = {
    get: (endpoint, config) =>
        instance.get(endpoint, config).then((r) => r.data),
    post: (endpoint, data, config) =>
        instance.post(endpoint, data, config).then((r) => r.data),
    // Used by the watersupply module updates (PUT /api/.../{id}).
    put: (endpoint, data, config) =>
        instance.put(endpoint, data, config).then((r) => r.data),
    patch: (endpoint, data, config) =>
        instance.patch(endpoint, data, config).then((r) => r.data),
    delete: (endpoint, config) =>
        instance.delete(endpoint, config).then((r) => r.data),
};
