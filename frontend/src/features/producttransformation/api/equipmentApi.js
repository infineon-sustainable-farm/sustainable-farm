/**
 * Product Transformation - Equipment API
 * API client for Product Transformation equipment operations.
 *
 * Single equipment register (ADR 0004): `equipmentId` is the business code of
 * a processing or drying machine of the farm register owned by Machinery
 * (`equipment`); its PT attributes live in `pt_equipment_spec`.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const equipmentApi = {
  /**
   * Get all PT equipment
   */
  getAllEquipment: () => apiClient.get('/api/equipment'),

  /**
   * Get PT equipment by ID
   */
  getEquipmentById: (equipmentId) => apiClient.get(`/api/equipment/${equipmentId}`),

  /**
   * Get available PT equipment
   */
  getAvailableEquipment: () => apiClient.get('/api/equipment/available'),

  /**
   * Get PT equipment by type
   */
  getEquipmentByType: (equipmentType) =>
    apiClient.get(`/api/equipment/type/${equipmentType}`),

  /**
   * Get PT equipment by maintenance status
   */
  getEquipmentByStatus: (maintenanceStatus) =>
    apiClient.get(`/api/equipment/status/${maintenanceStatus}`),

  /**
   * Create PT equipment
   */
  createEquipment: (equipmentData) => apiClient.post('/api/equipment', equipmentData),

  /**
   * Update PT equipment
   */
  updateEquipment: (equipmentId, equipmentData) =>
    apiClient.put(`/api/equipment/${equipmentId}`, equipmentData),

  /**
   * Delete PT equipment
   */
  deleteEquipment: (equipmentId) => apiClient.delete(`/api/equipment/${equipmentId}`),
};

export default equipmentApi;
