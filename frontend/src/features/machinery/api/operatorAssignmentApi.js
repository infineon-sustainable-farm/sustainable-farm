import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchOperatorAssignments(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.OPERATOR_ASSIGNMENTS, { params: { page, size } });
}

export function createOperatorAssignment(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.OPERATOR_ASSIGNMENTS, data);
}

export function updateOperatorAssignment(id, data) {
  return apiClient.patch(`${MACHINERY_ENDPOINTS.OPERATOR_ASSIGNMENTS}/${id}`, data);
}

export function deleteOperatorAssignment(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.OPERATOR_ASSIGNMENTS}/${id}`);
}
