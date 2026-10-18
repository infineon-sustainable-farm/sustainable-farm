import { useQuery } from "@tanstack/react-query";
import { fetchUsageLogs } from "../api/usageLogApi";

export function useUsageLogs(page, size = 10) {
  return useQuery({
    queryKey: ["usage-logs", page, size],
    queryFn: () => fetchUsageLogs(page, size),
  });
}
