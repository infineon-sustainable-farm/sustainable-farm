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
        apiError.data = body;
        throw apiError;
    }
);

export const apiClient = {
    get: (endpoint, config) =>
        instance.get(endpoint, config).then((r) => r.data),
    post: (endpoint, data, config) =>
        instance.post(endpoint, data, config).then((r) => r.data),
    put: (endpoint, data, config) =>
        instance.put(endpoint, data, config).then((r) => r.data),
    patch: (endpoint, data, config) =>
        instance.patch(endpoint, data, config).then((r) => r.data),
    delete: (endpoint, config) =>
        instance.delete(endpoint, config).then((r) => r.data),
};
