import apiClient from './api';

const dashboardApi = {
  getDashboardKPIs: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/dashboard/kpis', { params });
  },
  
  getHarvestQuantityKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/dashboard/kpi/harvest-quantity', { params });
  },
  
  getActiveBatchesKPI: () => {
    return apiClient.get('/api/dashboard/kpi/active-batches');
  },
  
  getQualityPassRateKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/dashboard/kpi/quality-pass-rate', { params });
  },
  
  getEnergyConsumptionKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return apiClient.get('/api/dashboard/kpi/energy-consumption', { params });
  },
  
  getEquipmentUtilizationKPI: () => {
    return apiClient.get('/api/dashboard/kpi/equipment-utilization');
  }
};

export { dashboardApi };