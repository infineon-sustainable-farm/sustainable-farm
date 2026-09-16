import { useEffect, useState } from "react";
import { fetchZones } from "../api/sitesecurityApi";
import type { LevelKey, ZoneDef } from "../api/sitesecurityTypes";

export function useZones(selectedKey: LevelKey) {
  const [zones, setZones] = useState<ZoneDef[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    fetchZones().then((res) => {
      if (isMounted) {
        setZones(res);
        setIsLoading(false);
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  const selectedZone = zones.find((z) => z.id === selectedKey) ?? zones[0];

  return { zones, selectedZone, isLoading };
}
