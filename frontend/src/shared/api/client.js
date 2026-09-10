import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

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
        throw new Error(
            `API Error ${status}: ${body || error.message}`
        );
    }
);

export const apiClient = {
    get: (endpoint, config) =>
        instance.get(endpoint, config).then((r) => r.data),
    post: (endpoint, data, config) =>
        instance.post(endpoint, data, config).then((r) => r.data),
    patch: (endpoint, data, config) =>
        instance.patch(endpoint, data, config).then((r) => r.data),
    delete: (endpoint, config) =>
        instance.delete(endpoint, config).then((r) => r.data),
};
