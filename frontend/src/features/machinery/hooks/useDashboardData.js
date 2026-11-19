import { useQueries } from "@tanstack/react-query";
import { fetchEquipments } from "../api/equipmentApi";
import { fetchSpareParts, fetchLowStockSpareParts } from "../api/sparePartApi";
import { fetchMaintenanceSchedules } from "../api/maintenanceScheduleApi";
import { fetchUsageLogs } from "../api/usageLogApi";
import { fetchFuelLogs } from "../api/fuelLogApi";
import { fetchRepairLogs } from "../api/repairLogApi";

const DASHBOARD_PAGE_SIZE = 100;

export function useDashboardData() {
  const results = useQueries({
    queries: [
      {
        queryKey: ["equipments", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchEquipments(0, DASHBOARD_PAGE_SIZE),
      },
      {
        queryKey: ["spare-parts", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchSpareParts(0, DASHBOARD_PAGE_SIZE),
      },
      {
        queryKey: ["spare-parts", "low-stock"],
        queryFn: fetchLowStockSpareParts,
      },
      {
        queryKey: ["maintenance-schedules", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchMaintenanceSchedules(0, DASHBOARD_PAGE_SIZE),
      },
      {
        queryKey: ["usage-logs", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchUsageLogs(0, DASHBOARD_PAGE_SIZE),
      },
      {
        queryKey: ["fuel-logs", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchFuelLogs(0, DASHBOARD_PAGE_SIZE),
      },
      {
        queryKey: ["repair-logs", 0, DASHBOARD_PAGE_SIZE],
        queryFn: () => fetchRepairLogs(0, DASHBOARD_PAGE_SIZE),
      },
    ],
  });

  const [
    equipments,
    spareParts,
    lowStockSpareParts,
    maintenanceSchedules,
    usageLogs,
    fuelLogs,
    repairLogs,
  ] = results;

  return {
    isLoading: results.some((result) => result.isLoading),
    isError: results.some((result) => result.isError),
    refetchAll: () => Promise.all(results.map((result) => result.refetch())),
    equipments: equipments.data,
    spareParts: spareParts.data,
    lowStockSpareParts: lowStockSpareParts.data,
    maintenanceSchedules: maintenanceSchedules.data,
    usageLogs: usageLogs.data,
    fuelLogs: fuelLogs.data,
    repairLogs: repairLogs.data,
  };
}
