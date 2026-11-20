/**
 * Product Transformation - Batch API
 * API client for batch management operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

/**
 * Batch API endpoints
 */
export const batchApi = {
  /**
   * Get all batches
   */
  getAllBatches: () => apiClient.get('/api/product-transformation/batches'),

  /**
   * Get batch by ID
   */
  getBatchById: (batchId) => apiClient.get(`/api/product-transformation/batches/${batchId}`),

  /**
   * Create a new batch
   */
  createBatch: (batchData) => apiClient.post('/api/product-transformation/batches', batchData),

  /**
   * Update a batch
   */
  updateBatch: (batchId, batchData) => apiClient.put(`/api/product-transformation/batches/${batchId}`, batchData),

  /**
   * Delete a batch
   */
  deleteBatch: (batchId) => apiClient.delete(`/api/product-transformation/batches/${batchId}`),

  /**
   * Advance batch status to next stage
   */
  advanceBatchStatus: (batchId) => apiClient.post(`/api/product-transformation/batches/${batchId}/advance-status`),

  /**
   * Set batch status manually (for operations like REJECTED)
   */
  setBatchStatus: (batchId, status) => apiClient.put(`/api/product-transformation/batches/${batchId}/status`, null, {
    params: { status },
  }),

  /**
   * Get batches by status
   */
  getBatchesByStatus: (status) => apiClient.get(`/api/product-transformation/batches/status/${status}`),

  /**
   * Get batches by farm
   */
  getBatchesByFarm: (farmId) => apiClient.get(`/api/product-transformation/batches/farm/${farmId}`),

  /**
   * Get batches by variety
   */
  getBatchesByVariety: (variety) => apiClient.get(`/api/product-transformation/batches/variety/${variety}`),

  /**
   * Get batches ready for shipping (status COMPLETED)
   */
  getBatchesReadyForShipping: () => apiClient.get('/api/product-transformation/batches/ready-for-shipping'),

  /**
   * Get batches by farm and block
   */
  getBatchesByFarmAndBlock: (farmId, blockId) =>
    apiClient.get(`/api/product-transformation/batches/farm/${farmId}/block/${blockId}`),

  /**
   * Get batches by harvest date range
   */
  getBatchesByHarvestDateRange: (startDate, endDate) =>
    apiClient.get('/api/product-transformation/batches/date-range', {
      params: { startDate, endDate },
    }),
};

export default batchApi;
