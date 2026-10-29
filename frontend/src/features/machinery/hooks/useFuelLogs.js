import { useQuery } from "@tanstack/react-query";
import { fetchFuelLogs } from "../api/fuelLogApi";

export function useFuelLogs(page, size = 10) {
  return useQuery({
    queryKey: ["fuel-logs", page, size],
    queryFn: () => fetchFuelLogs(page, size),
  });
}
