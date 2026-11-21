/**
 * Product Transformation - Operator API
 * API client for operator management operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

/**
 * Operator API endpoints
 */
export const operatorApi = {
  /**
   * Get all operators
   */
  getAllOperators: () => apiClient.get('/api/product-transformation/operators'),

  /**
   * Get operator by ID
   */
  getOperatorById: (operatorId) => apiClient.get(`/api/product-transformation/operators/${operatorId}`),

  /**
   * Create the operator role of an existing person of the people directory:
   * { operatorId, personId, role, certifications, activeStatus, hireDate }. The name is the person's.
   */
  createOperator: (operatorData) => apiClient.post('/api/product-transformation/operators', operatorData),

  /**
   * Update an operator
   */
  updateOperator: (operatorId, operatorData) => apiClient.put(`/api/product-transformation/operators/${operatorId}`, operatorData),

  /**
   * Delete an operator
   */
  deleteOperator: (operatorId) => apiClient.delete(`/api/product-transformation/operators/${operatorId}`),

  /**
   * Get operators by role
   */
  getOperatorsByRole: (role) => apiClient.get(`/api/product-transformation/operators/role/${role}`),

  /**
   * Get active operators
   */
  getActiveOperators: () => apiClient.get('/api/product-transformation/operators/active'),
};

export default operatorApi;
