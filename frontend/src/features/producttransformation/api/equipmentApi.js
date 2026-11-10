/**
 * Product Transformation - Equipment API
 * API client for Product Transformation equipment operations.
 *
 * Equipment collision rule (Option A): this client targets the PT-only
 * `pt_equipment` entity, not the Machinery module's `equipment`.
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
