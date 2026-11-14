/*
 * Activation and reset links carry their token in the URL fragment
 * (#token=...): the fragment never reaches a server log or a Referer header.
 * The page reads it once, then removes it from the address bar and history.
 */
function hashParams(hash) {
    return new URLSearchParams(String(hash ?? "").replace(/^#/, ""));
}

export function readTokenFromHash(hash) {
    const token = hashParams(hash).get("token")?.trim();
    return token ? token : null;
}

/** The fragment without the token: "" when nothing else was in it. */
export function hashWithoutToken(hash) {
    const params = hashParams(hash);
    params.delete("token");
    const rest = params.toString();
    return rest ? `#${rest}` : "";
}

/** Replaces the current history entry so the token cannot be found with Back or in the history. */
export function stripTokenFromUrl(location, history) {
    const nextHash = hashWithoutToken(location.hash);
    if (nextHash === location.hash) {
        return false;
    }
    history.replaceState(history.state, "", `${location.pathname}${location.search}${nextHash}`);
    return true;
}
