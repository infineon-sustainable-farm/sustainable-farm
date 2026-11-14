/*
 * The session itself lives in an HttpOnly cookie that this code cannot read:
 * the browser sends it with every Site Security request. This module only
 * remembers who is signed in (the server's SessionDto), to choose what the
 * screens show. The server stays the authority on every permission.
 */
let session = null;
let onSessionEnded = null;

/* Missing or odd fields from the server must not break the screens. */
export function normalizeSession(dto) {
    return {
        role: dto?.role ?? null,
        email: dto?.email ?? null,
        fullName: dto?.fullName ?? null,
        permissions: Array.isArray(dto?.permissions) ? dto.permissions : [],
        passwordSet: Boolean(dto?.passwordSet),
        codeSignInRetired: Boolean(dto?.codeSignInRetired),
    };
}

export function getSession() {
    return session;
}

export function getSessionRole() {
    return session?.role ?? null;
}

export function getSessionEmail() {
    return session?.email ?? null;
}

/** Stores the SessionDto answered by sign-in, activation or GET /session. */
export function startSession(dto) {
    session = normalizeSession(dto);
    return session;
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
