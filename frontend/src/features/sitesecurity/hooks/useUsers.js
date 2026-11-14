import { useCallback, useEffect, useState } from "react";
import { fetchUsers, fetchUsersSummary } from "../api/sitesecurityApi";
import { hasEmailInFlight } from "../utils/userStatus";

const EMAIL_CHECK_INTERVAL_MS = 2000;
const MAX_EMAIL_CHECKS = 30;

export function useUsersSummary() {
    const [summary, setSummary] = useState(null);
    const [error, setError] = useState(null);
    const [seed, setSeed] = useState(0);

    useEffect(() => {
        let isMounted = true;
        fetchUsersSummary()
            .then((res) => isMounted && setSummary(res))
            .catch((err) => isMounted && setError(err.message));
        return () => {
            isMounted = false;
        };
    }, [seed]);

    const reload = useCallback(() => setSeed((n) => n + 1), []);
    return { summary, error, reload };
}

/** Users list + summary. Every action answers the updated UserDto, so rows are replaced in place. */
export function useUsers() {
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const { summary, error: summaryError, reload: reloadSummary } = useUsersSummary();

    useEffect(() => {
        let isMounted = true;
        fetchUsers()
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

    /* Emails go out after the API has answered: poll while one is still on its way. */
    const isEmailInFlight = hasEmailInFlight(users);
    useEffect(() => {
        if (!isEmailInFlight)
            return undefined;
        let checks = 0;
        const timer = setInterval(() => {
            checks += 1;
            if (checks >= MAX_EMAIL_CHECKS)
                clearInterval(timer);
            fetchUsers().then(setUsers).catch(() => { });
        }, EMAIL_CHECK_INTERVAL_MS);
        return () => clearInterval(timer);
    }, [isEmailInFlight]);

    const replaceUser = useCallback((updated) => {
        if (!updated?.id) return;
        setUsers((prev) => (prev.some((u) => u.id === updated.id)
            ? prev.map((u) => (u.id === updated.id ? updated : u))
            : [updated, ...prev]));
        reloadSummary();
    }, [reloadSummary]);

    const reloadUsers = useCallback(() => {
        fetchUsers().then(setUsers).catch((err) => setError(err.message));
        reloadSummary();
    }, [reloadSummary]);

    return { users, summary, isLoading, error: error ?? summaryError, replaceUser, reloadUsers };
}
