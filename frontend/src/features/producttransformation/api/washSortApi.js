/**
 * Product Transformation - Wash & Sort Record API
 * API client for washing and sorting operations.
 *
 * Data contract: /api/product-transformation/wash-sort-records (see WashSortRecordController).
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const washSortApi = {
  /**
   * Get all wash & sort records
   */
  getAllWashSortRecords: () => apiClient.get('/api/product-transformation/wash-sort-records'),

  /**
   * Get a wash & sort record by ID
   */
  getWashSortRecordById: (recordId) => apiClient.get(`/api/product-transformation/wash-sort-records/${recordId}`),

  /**
   * Get wash & sort records for a batch
   */
  getWashSortRecordsByBatch: (batchId) =>
    apiClient.get(`/api/product-transformation/wash-sort-records/batch/${batchId}`),

  /**
   * Create a wash & sort record
   */
  createWashSortRecord: (recordData) => apiClient.post('/api/product-transformation/wash-sort-records', recordData),

  /**
   * Update a wash & sort record
   */
  updateWashSortRecord: (recordId, recordData) =>
    apiClient.put(`/api/product-transformation/wash-sort-records/${recordId}`, recordData),

  /**
   * Delete a wash & sort record
   */
  deleteWashSortRecord: (recordId) => apiClient.delete(`/api/product-transformation/wash-sort-records/${recordId}`),

  /**
   * Complete a wash & sort record
   */
  completeWashSortRecord: (recordId, completionData) =>
    apiClient.post(`/api/product-transformation/wash-sort-records/${recordId}/complete`, completionData),
};

export default washSortApi;
