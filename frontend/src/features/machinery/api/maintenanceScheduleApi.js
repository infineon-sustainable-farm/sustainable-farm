import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchMaintenanceSchedules(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.MAINTENANCE_SCHEDULES, { params: { page, size } });
}

export function createMaintenanceSchedule(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.MAINTENANCE_SCHEDULES, data);
}

export function updateMaintenanceSchedule(id, data) {
  return apiClient.put(`${MACHINERY_ENDPOINTS.MAINTENANCE_SCHEDULES}/${id}`, data);
}

export function deleteMaintenanceSchedule(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.MAINTENANCE_SCHEDULES}/${id}`);
}
