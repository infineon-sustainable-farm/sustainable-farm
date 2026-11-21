import { useEffect, useState } from "react";
import { fetchAccessLogs } from "../api/sitesecurityApi";
import { countLogs } from "../utils/accessLogs";

const ALL = "all";

export function useAccessLogs(initialFilter = ALL) {
    const [filter, setFilterValue] = useState(initialFilter);
    const [filteredLogs, setFilteredLogs] = useState([]);
    const [allLogs, setAllLogs] = useState([]);
    const [isLoadingAll, setIsLoadingAll] = useState(true);
    const [isLoadingFiltered, setIsLoadingFiltered] = useState(initialFilter !== ALL);
    const [error, setError] = useState(null);

    useEffect(() => {
        let isMounted = true;
        fetchAccessLogs(ALL)
            .then((res) => {
                if (isMounted) {
                    setAllLogs(res);
                    setIsLoadingAll(false);
                }
            })
            .catch((err) => {
                if (isMounted) {
                    setError(err.message);
                    setIsLoadingAll(false);
                }
            });
        return () => {
            isMounted = false;
        };
    }, []);

    useEffect(() => {
        if (filter === ALL) {
            return undefined;
        }
        let isMounted = true;
        fetchAccessLogs(filter)
            .then((res) => {
                if (isMounted) {
                    setFilteredLogs(res);
                    setIsLoadingFiltered(false);
                }
            })
            .catch((err) => {
                if (isMounted) {
                    setError(err.message);
                    setIsLoadingFiltered(false);
                }
            });
        return () => {
            isMounted = false;
        };
    }, [filter]);

    const setFilter = (next) => {
        if (next === filter) {
            return;
        }
        setFilteredLogs([]);
        setIsLoadingFiltered(next !== ALL);
        setError(null);
        setFilterValue(next);
    };

    return {
        filter,
        setFilter,
        logs: filter === ALL ? allLogs : filteredLogs,
        allLogs,
        counts: countLogs(allLogs),
        isLoading: filter === ALL ? isLoadingAll : isLoadingFiltered,
        error,
    };
}
