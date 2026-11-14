export const WRONG_ORIGIN_MESSAGE = "Open Site Security from its usual address to sign in.";
export const TOO_MANY_ATTEMPTS_MESSAGE = "Too many attempts from this network. Wait a minute, then try again.";
export const WRONG_PASSWORD_MESSAGE = "Email or password is incorrect.";
export const INVALID_LINK_MESSAGE = "This link is invalid, has expired or was already used. Ask an administrator for a new one.";

/** Error text for the public sign-in forms; `unauthorized` is the 401 text of that form. */
export function authErrorMessage(err, unauthorized, fallback) {
    if (err?.status === 401) return unauthorized;
    if (err?.status === 403) return WRONG_ORIGIN_MESSAGE;
    /* The backend sign-in limits answer JSON with their own advice; nginx answers HTML. */
    if (err?.status === 429) return typeof err?.data?.message === "string" ? err.data.message : TOO_MANY_ATTEMPTS_MESSAGE;
    return err?.message || fallback;
}

/** 400 field error for the password, when the server sent one. */
export function passwordFieldError(err) {
    return err?.data?.fieldErrors?.password ?? null;
}
