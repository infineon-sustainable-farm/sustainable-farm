/**
 * Product Transformation - Dashboard API
 * API client for dashboard statistics and metrics
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/apiClient';

/**
 * Dashboard API endpoints
 */
export const dashboardApi = {
  /**
   * Get dashboard statistics
   */
  getDashboardStats: () => apiClient.get('/api/product-transformation/dashboard/stats'),

  /**
   * Get batch status distribution
   */
  getBatchStatusDistribution: () => apiClient.get('/api/product-transformation/dashboard/batch-status-distribution'),

  /**
   * Get harvest quantity by variety
   */
  getHarvestQuantityByVariety: () => apiClient.get('/api/product-transformation/dashboard/harvest-quantity-by-variety'),

  /**
   * Get processing efficiency metrics
   */
  getProcessingEfficiency: () => apiClient.get('/api/product-transformation/dashboard/processing-efficiency'),

  /**
   * Get QC pass rate
   */
  getQcPassRate: () => apiClient.get('/api/product-transformation/dashboard/qc-pass-rate'),

  /**
   * Get recent activity
   */
  getRecentActivity: () => apiClient.get('/api/product-transformation/dashboard/recent-activity'),
};

export default dashboardApi;
