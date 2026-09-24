import { useEffect, useState } from "react";
import { fetchAccessLogs } from "../api/sitesecurityApi";

export function useAccessLogs(initialFilter = "all") {
    const [filter, setFilter] = useState(initialFilter);
    const [logs, setLogs] = useState([]);
    const [allLogs, setAllLogs] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let isMounted = true;
        fetchAccessLogs("all")
            .then((res) => {
                if (isMounted) {
                    setAllLogs(res);
                }
            })
            .catch((err) => {
                if (isMounted) {
                    setError(err.message);
                }
            });
        return () => {
            isMounted = false;
        };
    }, []);

    useEffect(() => {
        let isMounted = true;
        fetchAccessLogs(filter)
            .then((res) => {
                if (isMounted) {
                    setLogs(res);
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
    }, [filter]);

    const counts = {
        all: allLogs.length,
        approved: allLogs.filter((e) => e.status === "Approved").length,
        denied: allLogs.filter((e) => e.status === "Denied").length,
        visitors: allLogs.filter((e) => e.type === "Visitor").length,
        staff: allLogs.filter((e) => e.type === "Staff").length,
        service: allLogs.filter((e) => e.type === "Service").length,
    };
    return { filter, setFilter, logs, allLogs, counts, isLoading, error };
}
