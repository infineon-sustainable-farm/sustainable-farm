/**
 * Product Transformation - Drying Run API
 * API client for drying run operations.
 *
 * Data contract: /api/product-transformation/drying-runs (see DryingRunController).
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const dryingApi = {
  /**
   * Get all drying runs
   */
  getAllDryingRuns: () => apiClient.get('/api/product-transformation/drying-runs'),

  /**
   * Get a drying run by ID
   */
  getDryingRunById: (runId) => apiClient.get(`/api/product-transformation/drying-runs/${runId}`),

  /**
   * Get drying runs for a batch
   */
  getDryingRunsByBatch: (batchId) => apiClient.get(`/api/product-transformation/drying-runs/batch/${batchId}`),

  /**
   * Create a drying run
   */
  createDryingRun: (runData) => apiClient.post('/api/product-transformation/drying-runs', runData),

  /**
   * Update a drying run
   */
  updateDryingRun: (runId, runData) => apiClient.put(`/api/product-transformation/drying-runs/${runId}`, runData),

  /**
   * Delete a drying run
   */
  deleteDryingRun: (runId) => apiClient.delete(`/api/product-transformation/drying-runs/${runId}`),

  /**
   * Get drying runs for an equipment
   */
  getDryingRunsByEquipment: (equipmentId) =>
    apiClient.get(`/api/product-transformation/drying-runs/equipment/${equipmentId}`),
};

export default dryingApi;
