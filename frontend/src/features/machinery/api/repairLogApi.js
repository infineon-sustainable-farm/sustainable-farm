import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchRepairLogs(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.REPAIR_LOGS, { params: { page, size } });
}

export function createRepairLog(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.REPAIR_LOGS, data);
}

export function updateRepairLog(id, data) {
  return apiClient.put(`${MACHINERY_ENDPOINTS.REPAIR_LOGS}/${id}`, data);
}

export function deleteRepairLog(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.REPAIR_LOGS}/${id}`);
}
