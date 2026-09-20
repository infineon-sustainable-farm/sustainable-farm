import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchUsageLogs(page, size = 10) {
  return apiClient.get(ENDPOINTS.USAGE_LOGS, { params: { page, size } });
}

export function createUsageLog(data) {
  return apiClient.post(ENDPOINTS.USAGE_LOGS, data);
}

export function updateUsageLog(id, data) {
  return apiClient.put(`${ENDPOINTS.USAGE_LOGS}/${id}`, data);
}

export function deleteUsageLog(id) {
  return apiClient.delete(`${ENDPOINTS.USAGE_LOGS}/${id}`);
}
