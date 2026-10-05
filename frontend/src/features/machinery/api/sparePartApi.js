import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchSpareParts(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.SPARE_PARTS, { params: { page, size } });
}

export function fetchLowStockSpareParts() {
  return apiClient.get(`${MACHINERY_ENDPOINTS.SPARE_PARTS}/low-stock`);
}

export function createSparePart(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.SPARE_PARTS, data);
}

export function updateSparePart(id, data) {
  return apiClient.put(`${MACHINERY_ENDPOINTS.SPARE_PARTS}/${id}`, data);
}

export function deleteSparePart(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.SPARE_PARTS}/${id}`);
}
