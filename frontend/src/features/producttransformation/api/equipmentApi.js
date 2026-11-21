/**
 * Product Transformation - Equipment API
 * API client for Product Transformation equipment operations.
 *
 * Single equipment register (ADR 0004): `equipmentId` is the business code of
 * a processing or drying machine of the farm register owned by Machinery
 * (`equipment`); its PT attributes live in `pt_equipment_spec`. Machinery alone creates,
 * renames, changes the status of and deletes a machine: this client only reads the machines
 * and updates their PT attributes.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const equipmentApi = {
  /**
   * Get all PT equipment
   */
  getAllEquipment: () => apiClient.get('/api/product-transformation/equipment'),

  /**
   * Get PT equipment by ID
   */
  getEquipmentById: (equipmentId) => apiClient.get(`/api/product-transformation/equipment/${equipmentId}`),

  /**
   * Get available PT equipment
   */
  getAvailableEquipment: () => apiClient.get('/api/product-transformation/equipment/available'),

  /**
   * Get PT equipment by type
   */
  getEquipmentByType: (equipmentType) =>
    apiClient.get(`/api/product-transformation/equipment/type/${equipmentType}`),

  /**
   * Get PT equipment by maintenance status
   */
  getEquipmentByStatus: (maintenanceStatus) =>
    apiClient.get(`/api/product-transformation/equipment/status/${maintenanceStatus}`),

  /**
   * Update the PT attributes of a machine (type, capacity, energy, location, last maintenance)
   */
  updateEquipment: (equipmentId, equipmentData) =>
    apiClient.put(`/api/product-transformation/equipment/${equipmentId}`, equipmentData),
};

export default equipmentApi;
