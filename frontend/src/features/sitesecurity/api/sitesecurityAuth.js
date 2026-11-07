/*
 * The session itself lives in an HttpOnly cookie that this code cannot read:
 * the browser sends it with every Site Security request. This module only
 * remembers who is signed in, to choose what the screens show.
 */
let session = null;
let onSessionEnded = null;

export function getSessionRole() {
    return session?.role ?? null;
}

export function getSessionEmail() {
    return session?.email ?? null;
}

export function startSession(role, email = null) {
    session = { role, email };
}

export function endSession() {
    session = null;
    onSessionEnded?.();
}

export function setSessionEndedHandler(handler) {
    onSessionEnded = handler;
}

export function endSessionIfUnauthorized(err) {
    if (err.status === 401) endSession();
    throw err;
}
