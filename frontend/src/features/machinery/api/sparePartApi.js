import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchSpareParts(page, size = 10) {
  return apiClient.get(ENDPOINTS.SPARE_PARTS, { params: { page, size } });
}

export function fetchLowStockSpareParts() {
  return apiClient.get(`${ENDPOINTS.SPARE_PARTS}/low-stock`);
}

export function createSparePart(data) {
  return apiClient.post(ENDPOINTS.SPARE_PARTS, data);
}

export function updateSparePart(id, data) {
  return apiClient.put(`${ENDPOINTS.SPARE_PARTS}/${id}`, data);
}

export function deleteSparePart(id) {
  return apiClient.delete(`${ENDPOINTS.SPARE_PARTS}/${id}`);
}
