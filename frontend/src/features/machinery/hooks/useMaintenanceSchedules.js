import { useQuery } from "@tanstack/react-query";
import { fetchMaintenanceSchedules } from "../api/maintenanceScheduleApi";

export function useMaintenanceSchedules(page, size = 10) {
  return useQuery({
    queryKey: ["maintenance-schedules", page, size],
    queryFn: () => fetchMaintenanceSchedules(page, size),
  });
}
