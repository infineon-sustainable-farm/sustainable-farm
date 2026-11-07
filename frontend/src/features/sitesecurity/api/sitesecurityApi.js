import { apiClient } from "../../../shared/api/client";
import { SITESECURITY_ENDPOINTS as ENDPOINTS } from "./endpoints";
import { endSession, endSessionIfUnauthorized, startSession } from "./sitesecurityAuth";

/* The session cookie is HttpOnly: the browser attaches it, even when VITE_API_URL points elsewhere. */
const WITH_COOKIE = { withCredentials: true };

function authorizedGet(endpoint, config = {}) {
    return apiClient.get(endpoint, { ...config, ...WITH_COOKIE }).catch(endSessionIfUnauthorized);
}

function authorizedPost(endpoint, body) {
    return apiClient.post(endpoint, body, WITH_COOKIE).catch(endSessionIfUnauthorized);
}

/** Always answers the same way, whether the email belongs to an operator or not. */
export function requestSignInCode(email) {
    return apiClient.post(ENDPOINTS.REQUEST_CODE, { email }, WITH_COOKIE);
}

/** A right code opens the session; the server sets the cookie. */
export async function verifySignInCode(email, code) {
    const { role, email: signedInEmail } = await apiClient.post(ENDPOINTS.VERIFY_CODE, { email, code }, WITH_COOKIE);
    startSession(role, signedInEmail);
    return role;
}

/** Picks up a session still open in this browser, after a reload for example. */
export async function restoreSession() {
    const { role, email } = await apiClient.get(ENDPOINTS.SESSION, WITH_COOKIE);
    startSession(role, email);
    return role;
}

/** Closes the session on the server; the screen locks even if the server cannot be reached. */
export async function signOut() {
    try {
        await apiClient.post(ENDPOINTS.LOGOUT, null, WITH_COOKIE);
    }
    finally {
        endSession();
    }
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
    return authorizedPost(ENDPOINTS.CREDENTIALS, credential);
}

export function revokeCredentialApi(id) {
    return authorizedPost(`${ENDPOINTS.CREDENTIALS}/${encodeURIComponent(id)}/revoke`, null);
}

export function fetchAccessLogs(filter) {
    return authorizedGet(ENDPOINTS.LOGS, { params: { filter } });
}
