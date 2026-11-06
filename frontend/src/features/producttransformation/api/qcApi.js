/**
 * Product Transformation - QC Checkpoint API
 * API client for quality control checkpoint operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/apiClient';

/**
 * QC Checkpoint API endpoints
 */
export const qcApi = {
  /**
   * Get all QC checkpoints
   */
  getAllQcCheckpoints: () => apiClient.get('/api/qc-checkpoints'),

  /**
   * Get QC checkpoint by ID
   */
  getQcCheckpointById: (checkpointId) => apiClient.get(`/api/qc-checkpoints/${checkpointId}`),

  /**
   * Create a new QC checkpoint
   */
  createQcCheckpoint: (qcData) => apiClient.post('/api/qc-checkpoints', qcData),

  /**
   * Update a QC checkpoint
   */
  updateQcCheckpoint: (checkpointId, qcData) => apiClient.put(`/api/qc-checkpoints/${checkpointId}`, qcData),

  /**
   * Delete a QC checkpoint
   */
  deleteQcCheckpoint: (checkpointId) => apiClient.delete(`/api/qc-checkpoints/${checkpointId}`),

  /**
   * Get QC checkpoints by batch
   */
  getQcCheckpointsByBatch: (batchId) => apiClient.get(`/api/qc-checkpoints/batch/${batchId}`),

  /**
   * Get QC checkpoints by stage
   */
  getQcCheckpointsByStage: (stage) => apiClient.get(`/api/qc-checkpoints/stage/${stage}`),

  /**
   * Get QC checkpoints by inspector
   */
  getQcCheckpointsByInspector: (inspectorId) => apiClient.get(`/api/qc-checkpoints/inspector/${inspectorId}`),
};

export default qcApi;
