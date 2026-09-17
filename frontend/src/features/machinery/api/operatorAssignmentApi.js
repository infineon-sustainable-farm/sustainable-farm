import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchOperatorAssignments(page, size = 10) {
  return apiClient.get(ENDPOINTS.OPERATOR_ASSIGNMENTS, { params: { page, size } });
}

export function createOperatorAssignment(data) {
  return apiClient.post(ENDPOINTS.OPERATOR_ASSIGNMENTS, data);
}

export function updateOperatorAssignment(id, data) {
  return apiClient.patch(`${ENDPOINTS.OPERATOR_ASSIGNMENTS}/${id}`, data);
}

export function deleteOperatorAssignment(id) {
  return apiClient.delete(`${ENDPOINTS.OPERATOR_ASSIGNMENTS}/${id}`);
}
