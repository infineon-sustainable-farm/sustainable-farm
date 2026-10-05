import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchUsageLogs(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.USAGE_LOGS, { params: { page, size } });
}

export function createUsageLog(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.USAGE_LOGS, data);
}

export function updateUsageLog(id, data) {
  return apiClient.put(`${MACHINERY_ENDPOINTS.USAGE_LOGS}/${id}`, data);
}

export function deleteUsageLog(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.USAGE_LOGS}/${id}`);
}
