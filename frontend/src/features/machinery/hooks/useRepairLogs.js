import { useQuery } from "@tanstack/react-query";
import { fetchRepairLogs } from "../api/repairLogApi";

export function useRepairLogs(page, size = 10) {
  return useQuery({
    queryKey: ["repair-logs", page, size],
    queryFn: () => fetchRepairLogs(page, size),
  });
}
