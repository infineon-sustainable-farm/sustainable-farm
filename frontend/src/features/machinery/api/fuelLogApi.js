import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchFuelLogs(page, size = 10) {
  return apiClient.get(ENDPOINTS.FUEL_LOGS, { params: { page, size } });
}

export function createFuelLog(data) {
  return apiClient.post(ENDPOINTS.FUEL_LOGS, data);
}

export function updateFuelLog(id, data) {
  return apiClient.put(`${ENDPOINTS.FUEL_LOGS}/${id}`, data);
}

export function deleteFuelLog(id) {
  return apiClient.delete(`${ENDPOINTS.FUEL_LOGS}/${id}`);
}
