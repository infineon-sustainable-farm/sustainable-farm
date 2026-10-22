/**
 * Product Transformation - Operator API
 * API client for operator management operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/apiClient';

/**
 * Operator API endpoints
 */
export const operatorApi = {
  /**
   * Get all operators
   */
  getAllOperators: () => apiClient.get('/api/operators'),

  /**
   * Get operator by ID
   */
  getOperatorById: (operatorId) => apiClient.get(`/api/operators/${operatorId}`),

  /**
   * Create a new operator
   */
  createOperator: (operatorData) => apiClient.post('/api/operators', operatorData),

  /**
   * Update an operator
   */
  updateOperator: (operatorId, operatorData) => apiClient.put(`/api/operators/${operatorId}`, operatorData),

  /**
   * Delete an operator
   */
  deleteOperator: (operatorId) => apiClient.delete(`/api/operators/${operatorId}`),

  /**
   * Get operators by role
   */
  getOperatorsByRole: (role) => apiClient.get(`/api/operators/role/${role}`),

  /**
   * Get active operators
   */
  getActiveOperators: () => apiClient.get('/api/operators/active'),
};

export default operatorApi;
