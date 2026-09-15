import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

export function fetchEquipments(page, size = 20) {
  return apiClient.get(ENDPOINTS.EQUIPMENTS, { params: { page, size } });
}

export function createEquipment(data) {
  return apiClient.post(ENDPOINTS.EQUIPMENTS, data);
}

export function updateEquipmentStatus(id, status) {
  return apiClient.patch(`${ENDPOINTS.EQUIPMENTS}/${id}`, { status });
}

export function deleteEquipment(id) {
  return apiClient.delete(`${ENDPOINTS.EQUIPMENTS}/${id}`);
}