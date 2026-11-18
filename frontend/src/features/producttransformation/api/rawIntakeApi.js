/**
 * Product Transformation - Raw Intake API
 * API client for raw intake (reception) operations.
 *
 * Data contract: /api/product-transformation/raw-intakes (see RawIntakeController).
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

export const rawIntakeApi = {
  /**
   * Get all raw intakes
   */
  getAllRawIntakes: () => apiClient.get('/api/product-transformation/raw-intakes'),

  /**
   * Get a raw intake by ID
   */
  getRawIntakeById: (intakeId) => apiClient.get(`/api/product-transformation/raw-intakes/${intakeId}`),

  /**
   * Get raw intakes for a batch
   */
  getRawIntakesByBatch: (batchId) => apiClient.get(`/api/product-transformation/raw-intakes/batch/${batchId}`),

  /**
   * Create a raw intake
   */
  createRawIntake: (intakeData) => apiClient.post('/api/product-transformation/raw-intakes', intakeData),

  /**
   * Update a raw intake
   */
  updateRawIntake: (intakeId, intakeData) =>
    apiClient.put(`/api/product-transformation/raw-intakes/${intakeId}`, intakeData),

  /**
   * Delete a raw intake
   */
  deleteRawIntake: (intakeId) => apiClient.delete(`/api/product-transformation/raw-intakes/${intakeId}`),

  /**
   * Initialize a batch from an intake
   */
  initializeBatch: (intakeId) => apiClient.post(`/api/product-transformation/raw-intakes/${intakeId}/initialize-batch`),
};

export default rawIntakeApi;
