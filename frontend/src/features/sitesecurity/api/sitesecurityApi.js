import { apiClient } from "../../../shared/api/client";
import { SITESECURITY_ENDPOINTS as ENDPOINTS } from "./endpoints";
import { endSessionIfUnauthorized, startSession, withAuth } from "./sitesecurityAuth";

function authorizedGet(endpoint, config) {
    return apiClient.get(endpoint, withAuth(config)).catch(endSessionIfUnauthorized);
}

export async function openSession(token) {
    const { role } = await apiClient.get(ENDPOINTS.SESSION, {
        headers: { Authorization: `Bearer ${token}` },
    });
    startSession(token, role);
    return role;
}

export function fetchSecurityOverview() {
    return authorizedGet(ENDPOINTS.OVERVIEW);
}

export function fetchZones() {
    return authorizedGet(ENDPOINTS.ZONES);
}

export function fetchCredentials() {
    return authorizedGet(ENDPOINTS.CREDENTIALS);
}

export function createCredentialApi(credential) {
    return apiClient.post(ENDPOINTS.CREDENTIALS, credential, withAuth()).catch(endSessionIfUnauthorized);
}

export function revokeCredentialApi(id) {
    return apiClient
        .post(`${ENDPOINTS.CREDENTIALS}/${encodeURIComponent(id)}/revoke`, null, withAuth())
        .catch(endSessionIfUnauthorized);
}

export function fetchAccessLogs(filter) {
    return authorizedGet(ENDPOINTS.LOGS, { params: { filter } });
}
