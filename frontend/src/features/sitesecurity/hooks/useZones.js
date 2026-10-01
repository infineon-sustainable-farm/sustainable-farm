import { useEffect, useState } from "react";
import { fetchZones } from "../api/sitesecurityApi";

export function useZones(selectedKey) {
    const [zones, setZones] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let isMounted = true;
        fetchZones()
            .then((res) => {
                if (isMounted) {
                    setZones(res);
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

    const selectedZone = zones.find((z) => z.id === selectedKey) ?? zones[0];
    return { zones, selectedZone, isLoading, error };
}
