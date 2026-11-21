import { apiClient } from "../../../shared/api/client";
import { SITESECURITY_ENDPOINTS as ENDPOINTS } from "./endpoints";

/* The caller's Site Security role, permissions and zones. Site Security signs nobody in. */
export function fetchAuthorization() {
  return apiClient.get(ENDPOINTS.AUTHORIZATION);
}

export function fetchSecurityOverview() {
  return apiClient.get(ENDPOINTS.OVERVIEW);
}

export function fetchZones() {
  return apiClient.get(ENDPOINTS.ZONES);
}

export function fetchCredentials() {
  return apiClient.get(ENDPOINTS.CREDENTIALS);
}

/* The holder is a person of the shared directory: the payload carries its id, never its name. */
export function createCredentialApi(credential) {
  return apiClient.post(ENDPOINTS.CREDENTIALS, credential);
}

export function revokeCredentialApi(id) {
  return apiClient.post(`${ENDPOINTS.CREDENTIALS}/${encodeURIComponent(id)}/revoke`, null);
}

export function fetchAccessLogs(filter) {
  return apiClient.get(ENDPOINTS.LOGS, { params: { filter } });
}
