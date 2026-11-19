/**
 * Product Transformation - Packaging Record API
 * API client for packaging record operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const packagingApi = {
  /**
   * Get all packaging records
   */
  getAllPackagingRecords: () => apiClient.get('/api/product-transformation/packaging-records'),

  /**
   * Get packaging record by ID
   */
  getPackagingRecordById: (recordId) => apiClient.get(`/api/product-transformation/packaging-records/${recordId}`),

  /**
   * Create a new packaging record
   */
  createPackagingRecord: (recordData) => apiClient.post('/api/product-transformation/packaging-records', recordData),

  /**
   * Update a packaging record
   */
  updatePackagingRecord: (recordId, recordData) =>
    apiClient.put(`/api/product-transformation/packaging-records/${recordId}`, recordData),

  /**
   * Delete a packaging record
   */
  deletePackagingRecord: (recordId) => apiClient.delete(`/api/product-transformation/packaging-records/${recordId}`),

  /**
   * Get packaging records by batch
   */
  getPackagingRecordsByBatch: (batchId) => apiClient.get(`/api/product-transformation/packaging-records/batch/${batchId}`),

  /**
   * Get export-ready packaging records
   */
  getExportReadyPackagingRecords: () => apiClient.get('/api/product-transformation/packaging-records/export-ready'),

  /**
   * Mark a packaging record as export ready
   */
  markExportReady: (recordId) =>
    apiClient.post(`/api/product-transformation/packaging-records/${recordId}/mark-export-ready`, {}),
};

export default packagingApi;
