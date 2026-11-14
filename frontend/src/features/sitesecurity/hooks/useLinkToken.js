import { useEffect, useState } from "react";
import { inspectToken } from "../api/sitesecurityApi";
import { readTokenFromHash, stripTokenFromUrl } from "../utils/urlToken";

/*
 * Reads the token of an emailed link once (rendering only reads the hash),
 * removes it from the address bar and history, then asks the server what it is.
 * The token then lives only in this component's state.
 */
export function useLinkToken(expectedPurpose) {
    const [token] = useState(() => readTokenFromHash(window.location.hash));
    const [result, setResult] = useState(() => ({ state: token ? "loading" : "invalid", info: null }));

    useEffect(() => {
        stripTokenFromUrl(window.location, window.history);
        if (!token) return undefined;
        let active = true;
        inspectToken(token)
            .then((info) => {
                if (!active) return;
                setResult(info?.purpose === expectedPurpose ? { state: "ready", info } : { state: "invalid", info: null });
            })
            .catch((err) => {
                if (active) setResult({ state: err.status === 400 ? "invalid" : "error", info: null, error: err });
            });
        return () => {
            active = false;
        };
    }, [token, expectedPurpose]);

    const markInvalid = () => setResult({ state: "invalid", info: null });
    return { token, ...result, markInvalid };
}
