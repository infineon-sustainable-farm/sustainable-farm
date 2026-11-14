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

/* The shared client has no put; the server accepts PATCH /users/{id} with the PUT body. */
function authorizedPatch(endpoint, body) {
    return apiClient.patch(endpoint, body, WITH_COOKIE).catch(endSessionIfUnauthorized);
}

/* Public auth calls: a 401 there means "wrong credentials", never "session expired". */
function publicPost(endpoint, body) {
    return apiClient.post(endpoint, body, WITH_COOKIE);
}

function userPath(id, suffix = "") {
    return `${ENDPOINTS.USERS}/${encodeURIComponent(id)}${suffix}`;
}

/* ------------------------------ sign-in ------------------------------ */

/** Email + password; the server sets the cookie and answers the SessionDto. */
export async function signIn(email, password) {
    return startSession(await publicPost(ENDPOINTS.LOGIN, { email, password }));
}

/** Always answers the same way, whether the email belongs to an operator or not. */
export function requestSignInCode(email) {
    return publicPost(ENDPOINTS.REQUEST_CODE, { email });
}

/** Legacy path: a right code opens the session; the server sets the cookie. */
export async function verifySignInCode(email, code) {
    return startSession(await publicPost(ENDPOINTS.VERIFY_CODE, { email, code }));
}

/** Picks up a session still open in this browser, after a reload for example. */
export async function restoreSession() {
    return startSession(await apiClient.get(ENDPOINTS.SESSION, WITH_COOKIE));
}

/** Re-reads the session of a signed-in user, after a password change for example. */
export async function refreshSession() {
    return startSession(await authorizedGet(ENDPOINTS.SESSION));
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

/** Always answers 202, whether the account exists or not. */
export function requestPasswordReset(email) {
    return publicPost(ENDPOINTS.FORGOT_PASSWORD, { email });
}

/* --------------------------- emailed links --------------------------- */

export function inspectToken(token) {
    return publicPost(ENDPOINTS.INSPECT_TOKEN, { token });
}

export function confirmInvitation(token) {
    return publicPost(ENDPOINTS.CONFIRM_INVITATION, { token });
}

/** A successful activation signs the new user in. */
export async function activateAccount(token, password) {
    return startSession(await publicPost(ENDPOINTS.ACTIVATE, { token, password }));
}

/** Does not sign in: the user signs in with the new password afterwards. */
export function resetPassword(token, password) {
    return publicPost(ENDPOINTS.RESET_PASSWORD, { token, password });
}

/* ------------------------- signed-in account ------------------------- */

/*
 * A 401 here means "current password is wrong" (contract), so it must not lock
 * the screen. If the session really expired, the next authorized call locks it.
 */
export function changePassword(currentPassword, newPassword) {
    return apiClient.post(ENDPOINTS.ACCOUNT_PASSWORD, { currentPassword, newPassword }, WITH_COOKIE);
}

export function setupPassword(code, password) {
    return authorizedPost(ENDPOINTS.ACCOUNT_PASSWORD_SETUP, { code, password });
}

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

/* ------------------------------- admin ------------------------------- */

export function fetchUsers() {
    return authorizedGet(ENDPOINTS.USERS);
}

export function fetchUsersSummary() {
    return authorizedGet(ENDPOINTS.USERS_SUMMARY);
}

export function inviteUser(invitation) {
    return authorizedPost(ENDPOINTS.USERS, invitation);
}

export function updateUser(id, changes) {
    return authorizedPatch(userPath(id), changes);
}

/** Also sends the password setup link to an active account without a password. */
export function resendInvitation(id) {
    return authorizedPost(userPath(id, "/invitation"), null);
}

export function revokeInvitation(id) {
    return authorizedPost(userPath(id, "/invitation/revoke"), null);
}

/** Fallback when email is down: a new link, shown once; the previous link stops working. */
export function createActivationLink(id) {
    return authorizedPost(userPath(id, "/invitation/link"), null);
}

export function disableUser(id) {
    return authorizedPost(userPath(id, "/disable"), null);
}

export function enableUser(id) {
    return authorizedPost(userPath(id, "/enable"), null);
}

export function fetchUserHistory(id) {
    return authorizedGet(userPath(id, "/history"));
}

/** Every email about the account (invitation, setup, reset), newest first. */
export function fetchUserEmails(id) {
    return authorizedGet(userPath(id, "/emails"));
}

export function fetchCodeSignInSettings() {
    return authorizedGet(ENDPOINTS.CODE_SIGN_IN_SETTINGS);
}

export function retireCodeSignIn(confirmation) {
    return authorizedPost(`${ENDPOINTS.CODE_SIGN_IN_SETTINGS}/retire`, { confirmation });
}

export function restoreCodeSignIn() {
    return authorizedPost(`${ENDPOINTS.CODE_SIGN_IN_SETTINGS}/restore`, {});
}

export function confirmEmailDelivery(deliveryId, evidence) {
    return authorizedPost(`${ENDPOINTS.EMAIL_DELIVERIES}/${encodeURIComponent(deliveryId)}/confirm`, { evidence });
}
