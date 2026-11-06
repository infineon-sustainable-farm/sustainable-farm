import apiClient from './api';

const dashboardApi = {
  getDashboardKPIs: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/kpis', { params });
  },
  
  getHarvestQuantityKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/kpi/harvest-quantity', { params });
  },
  
  getActiveBatchesKPI: () => {
    return apiClient.get('/api/product-transformation/dashboard/kpi/active-batches');
  },
  
  getQualityPassRateKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/kpi/quality-pass-rate', { params });
  },
  
  getEnergyConsumptionKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/kpi/energy-consumption', { params });
  },
  
  getEquipmentUtilizationKPI: () => {
    return apiClient.get('/api/product-transformation/dashboard/kpi/equipment-utilization');
  },
  
  // Chart data endpoints
  getHarvestTrendChart: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/charts/harvest-trend', { params });
  },
  
  getProductionOutputChart: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/charts/production-output', { params });
  },
  
  getEnergyBreakdownChart: () => {
    return apiClient.get('/api/product-transformation/dashboard/charts/energy-breakdown');
  },
  
  getEquipmentUtilizationChart: () => {
    return apiClient.get('/api/product-transformation/dashboard/charts/equipment-utilization');
  },
  
  getQualityTrendChart: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/product-transformation/dashboard/charts/quality-trend', { params });
  }
};

export { dashboardApi };