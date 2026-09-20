import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchRepairLogs(page, size = 10) {
  return apiClient.get(ENDPOINTS.REPAIR_LOGS, { params: { page, size } });
}

export function createRepairLog(data) {
  return apiClient.post(ENDPOINTS.REPAIR_LOGS, data);
}

export function updateRepairLog(id, data) {
  return apiClient.put(`${ENDPOINTS.REPAIR_LOGS}/${id}`, data);
}

export function deleteRepairLog(id) {
  return apiClient.delete(`${ENDPOINTS.REPAIR_LOGS}/${id}`);
}
