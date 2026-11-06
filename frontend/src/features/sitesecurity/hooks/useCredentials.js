import { useEffect, useState } from "react";
import { createCredentialApi, fetchCredentials, revokeCredentialApi } from "../api/sitesecurityApi";
import { hasPendingEmail, mergeCredentials } from "../utils/credentialForm";

const EMAIL_CHECK_INTERVAL_MS = 1500;
const MAX_EMAIL_CHECKS = 40;

export function useCredentials() {
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [freshId, setFreshId] = useState(null);

    useEffect(() => {
        let isMounted = true;
        fetchCredentials()
            .then((res) => {
                if (isMounted) {
                    setUsers(res);
                    setIsLoading(false);
                }
            })
            .catch((err) => {
                if (isMounted) {
                    setError(err.message);
                    setIsLoading(false);
                }
            });
        return () => {
            isMounted = false;
        };
    }, []);

    /* Emails go out after the API has answered, so their result arrives later. */
    const isEmailPending = hasPendingEmail(users);
    useEffect(() => {
        if (!isEmailPending)
            return undefined;
        let checks = 0;
        const timer = setInterval(() => {
            checks += 1;
            if (checks >= MAX_EMAIL_CHECKS)
                clearInterval(timer);
            fetchCredentials()
                .then((latest) => setUsers((current) => mergeCredentials(current, latest)))
                .catch(() => { });
        }, EMAIL_CHECK_INTERVAL_MS);
        return () => clearInterval(timer);
    }, [isEmailPending]);

    /* Rejects on failure: the form that called it shows the error and keeps its input. */
    const addCredential = async (u) => {
        const created = await createCredentialApi(u);
        setUsers((prev) => [created, ...prev]);
        setFreshId(created.id);
        setError(null);
    };

    const revokeCredential = async (id) => {
        try {
            const revoked = await revokeCredentialApi(id);
            setUsers((prev) => prev.map((u) => (u.id === revoked.id ? revoked : u)));
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    };

    return { users, freshId, addCredential, revokeCredential, isLoading, error };
}
