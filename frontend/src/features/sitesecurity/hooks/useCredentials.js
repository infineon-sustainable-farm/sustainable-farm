import { useEffect, useState } from "react";
import { createCredentialApi, fetchCredentials } from "../api/sitesecurityApi";

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

    const addCredential = async (u) => {
        try {
            const created = await createCredentialApi(u);
            setUsers((prev) => [created, ...prev]);
            setFreshId(created.id);
            setError(null);
        } catch (err) {
            setError(err.message);
            throw err;
        }
    };

    return { users, freshId, addCredential, isLoading, error };
}
