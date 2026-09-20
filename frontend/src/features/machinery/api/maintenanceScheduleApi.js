import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchMaintenanceSchedules(page, size = 10) {
  return apiClient.get(ENDPOINTS.MAINTENANCE_SCHEDULES, { params: { page, size } });
}

export function createMaintenanceSchedule(data) {
  return apiClient.post(ENDPOINTS.MAINTENANCE_SCHEDULES, data);
}

export function updateMaintenanceSchedule(id, data) {
  return apiClient.put(`${ENDPOINTS.MAINTENANCE_SCHEDULES}/${id}`, data);
}

export function deleteMaintenanceSchedule(id) {
  return apiClient.delete(`${ENDPOINTS.MAINTENANCE_SCHEDULES}/${id}`);
}
