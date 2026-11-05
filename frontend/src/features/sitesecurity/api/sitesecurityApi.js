import { apiClient } from "../../../shared/api/client";
import { SITESECURITY_ENDPOINTS as ENDPOINTS } from "./endpoints";

export function fetchSecurityOverview() {
    return apiClient.get(ENDPOINTS.OVERVIEW);
}

export function fetchZones() {
    return apiClient.get(ENDPOINTS.ZONES);
}

export function fetchCredentials() {
    return apiClient.get(ENDPOINTS.CREDENTIALS);
}

export function createCredentialApi(credential) {
    return apiClient
        .post(ENDPOINTS.CREDENTIALS, credential)
        .catch((err) => {
            throw new Error(err.message || "Failed to create credential. Please try again.");
        });
}

export function fetchAccessLogs(filter) {
    return apiClient.get(ENDPOINTS.LOGS, { params: { filter } });
}
