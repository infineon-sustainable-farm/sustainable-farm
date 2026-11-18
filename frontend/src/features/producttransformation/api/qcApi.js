/**
 * Product Transformation - QC Checkpoint API
 * API client for quality control checkpoint operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

/**
 * QC Checkpoint API endpoints
 */
export const qcApi = {
  /**
   * Get all QC checkpoints
   */
  getAllQcCheckpoints: () => apiClient.get('/api/product-transformation/qc-checkpoints'),

  /**
   * Get QC checkpoint by ID
   */
  getQcCheckpointById: (checkpointId) => apiClient.get(`/api/product-transformation/qc-checkpoints/${checkpointId}`),

  /**
   * Create a new QC checkpoint
   */
  createQcCheckpoint: (qcData) => apiClient.post('/api/product-transformation/qc-checkpoints', qcData),

  /**
   * Update a QC checkpoint
   */
  updateQcCheckpoint: (checkpointId, qcData) => apiClient.put(`/api/product-transformation/qc-checkpoints/${checkpointId}`, qcData),

  /**
   * Delete a QC checkpoint
   */
  deleteQcCheckpoint: (checkpointId) => apiClient.delete(`/api/product-transformation/qc-checkpoints/${checkpointId}`),

  /**
   * Get QC checkpoints by batch
   */
  getQcCheckpointsByBatch: (batchId) => apiClient.get(`/api/product-transformation/qc-checkpoints/batch/${batchId}`),

  /**
   * Get QC checkpoints by inspector
   */
  getQcCheckpointsByInspector: (inspectorId) => apiClient.get(`/api/product-transformation/qc-checkpoints/inspector/${inspectorId}`),
};

export default qcApi;
