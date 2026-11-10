/**
 * Product Transformation - Dashboard API
 * API client for dashboard statistics and metrics
 *
 * STATUS (J3): All endpoints implemented and mapped to Product Transformation backend routes.
 * See api-client.ts dashboardApis for concrete URL mapping.
 * Do NOT redirect to Visitor Management (different module, different data).
 */

import { apiClient } from '../../../shared/api/client';

// apiClient is fetch-based: it does not understand axios-style `params`.
// Build the query string explicitly so date filters actually reach the backend.
const withQuery = (endpoint, startDate, endDate) => {
  const params = new URLSearchParams();
  if (startDate) params.set('startDate', startDate);
  if (endDate) params.set('endDate', endDate);
  const query = params.toString();
  return query ? `${endpoint}?${query}` : endpoint;
};

export const dashboardApi = {
  getDashboardStats: async () => {
    return await apiClient.get('/api/product-transformation/dashboard/kpis');
  },
  getHarvestTrendData: async (startDate, endDate) => {
    return await apiClient.get(
      withQuery('/api/product-transformation/dashboard/charts/harvest-trend', startDate, endDate)
    );
  },
  getProductionOutputData: async (startDate, endDate) => {
    return await apiClient.get(
      withQuery('/api/product-transformation/dashboard/charts/production-output', startDate, endDate)
    );
  },
  getEnergyBreakdownData: async () => {
    return await apiClient.get('/api/product-transformation/dashboard/charts/energy-breakdown');
  },
  getEquipmentUtilizationData: async () => {
    return await apiClient.get('/api/product-transformation/dashboard/charts/equipment-utilization');
  },
  getQualityTrendData: async (startDate, endDate) => {
    return await apiClient.get(
      withQuery('/api/product-transformation/dashboard/charts/quality-trend', startDate, endDate)
    );
  }
};

export default dashboardApi;
