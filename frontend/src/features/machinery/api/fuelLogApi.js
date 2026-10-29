import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchFuelLogs(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.FUEL_LOGS, { params: { page, size } });
}

export function createFuelLog(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.FUEL_LOGS, data);
}

export function updateFuelLog(id, data) {
  return apiClient.put(`${MACHINERY_ENDPOINTS.FUEL_LOGS}/${id}`, data);
}

export function deleteFuelLog(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.FUEL_LOGS}/${id}`);
}
