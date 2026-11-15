import { apiClient } from "../../../shared/api/client";
import { SITESECURITY_ENDPOINTS as ENDPOINTS } from "./endpoints";
const WITH_COOKIE = {};

function authorizedGet(endpoint, config = {}) {
    return apiClient.get(endpoint, { ...config, ...WITH_COOKIE });
}

function authorizedPost(endpoint, body) {
    return apiClient.post(endpoint, body, WITH_COOKIE);
}

export const restoreSession = () => authorizedGet(ENDPOINTS.SESSION);

/* ---------------------------- operations ----------------------------- */

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
    return authorizedPost(ENDPOINTS.CREDENTIALS, credential);
}

export function revokeCredentialApi(id) {
    return authorizedPost(`${ENDPOINTS.CREDENTIALS}/${encodeURIComponent(id)}/revoke`, null);
}

export function fetchAccessLogs(filter) {
    return authorizedGet(ENDPOINTS.LOGS, { params: { filter } });
}

export function fetchAuthorization() {
    return authorizedGet(ENDPOINTS.AUTHORIZATION);
}
