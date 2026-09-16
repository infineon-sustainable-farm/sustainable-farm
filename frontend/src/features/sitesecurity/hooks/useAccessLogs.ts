import { useEffect, useState } from "react";
import { fetchAccessLogs } from "../api/sitesecurityApi";
import type { LogEntry, LogFilter } from "../api/sitesecurityTypes";

export function useAccessLogs(initialFilter: LogFilter = "all") {
  const [filter, setFilter] = useState<LogFilter>(initialFilter);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [allLogs, setAllLogs] = useState<LogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    fetchAccessLogs("all").then((res) => {
      if (isMounted) {
        setAllLogs(res);
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    fetchAccessLogs(filter).then((res) => {
      if (isMounted) {
        setLogs(res);
        setIsLoading(false);
      }
    });
    return () => {
      isMounted = false;
    };
  }, [filter]);

  const counts: Record<LogFilter, number> = {
    all: allLogs.length,
    approved: allLogs.filter((e) => e.status === "Approved").length,
    denied: allLogs.filter((e) => e.status === "Denied").length,
    visitors: allLogs.filter((e) => e.type === "Visitor").length,
    staff: allLogs.filter((e) => e.type === "Staff").length,
    service: allLogs.filter((e) => e.type === "Service").length,
  };

  return { filter, setFilter, logs, allLogs, counts, isLoading };
}
