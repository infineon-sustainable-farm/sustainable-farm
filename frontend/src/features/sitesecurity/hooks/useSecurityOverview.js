import { useEffect, useState } from "react";
import { fetchSecurityOverview } from "../api/sitesecurityApi";

export function useSecurityOverview() {
    const [gates, setGates] = useState([]);
    const [stats, setStats] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let isMounted = true;
        fetchSecurityOverview()
            .then((res) => {
                if (isMounted) {
                    setGates(res.gates);
                    setStats(res.stats);
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

    return { gates, stats, isLoading, error };
}
