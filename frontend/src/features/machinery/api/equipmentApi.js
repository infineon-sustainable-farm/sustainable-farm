import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";

export function fetchEquipments(page, size = 10) {
  return apiClient.get(MACHINERY_ENDPOINTS.EQUIPMENTS, { params: { page, size } });
}

export function createEquipment(data) {
  return apiClient.post(MACHINERY_ENDPOINTS.EQUIPMENTS, data);
}

export function updateEquipmentStatus(id, status) {
  return apiClient.patch(`${MACHINERY_ENDPOINTS.EQUIPMENTS}/${id}`, { status });
}

export function deleteEquipment(id) {
  return apiClient.delete(`${MACHINERY_ENDPOINTS.EQUIPMENTS}/${id}`);
}