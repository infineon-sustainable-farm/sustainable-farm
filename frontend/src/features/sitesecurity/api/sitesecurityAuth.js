/*
 * The access token lives in this module's memory only: never in the bundle or
 * in browser storage. Reloading the page therefore asks for it again.
 */
let session = null;
let onSessionEnded = null;

export function getSessionRole() {
    return session?.role ?? null;
}

export function startSession(token, role) {
    session = { token, role };
}

export function endSession() {
    session = null;
    onSessionEnded?.();
}

export function setSessionEndedHandler(handler) {
    onSessionEnded = handler;
}

export function withAuth(config = {}) {
    if (!session) return config;
    return { ...config, headers: { ...config.headers, Authorization: `Bearer ${session.token}` } };
}

export function endSessionIfUnauthorized(err) {
    if (err.status === 401) endSession();
    throw err;
}
