import { useEffect, useState } from "react";
import { fetchSecurityOverview } from "../api/sitesecurityApi";
import type { GateDef, OverviewStats } from "../api/sitesecurityTypes";

export function useSecurityOverview() {
  const [gates, setGates] = useState<GateDef[]>([]);
  const [stats, setStats] = useState<OverviewStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    fetchSecurityOverview().then((res) => {
      if (isMounted) {
        setGates(res.gates);
        setStats(res.stats);
        setIsLoading(false);
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  return { gates, stats, isLoading };
}
