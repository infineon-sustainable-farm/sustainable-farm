import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Create axios instance with default configuration
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add any auth tokens or other headers here if needed
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Handle common error scenarios
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', error.response.data);
    } else if (error.request) {
      // Request made but no response received
      console.error('Network Error:', error.message);
    } else {
      // Error in request configuration
      console.error('Request Error:', error.message);
    }
    return Promise.reject(error);
  }
);

/**
 * Dashboard API Service
 * Methods for fetching dashboard KPI data
 */
export const dashboardApi = {
  /**
   * Get comprehensive dashboard KPIs
   * @param {string} startDate - Start date (yyyy-MM-dd)
   * @param {string} endDate - End date (yyyy-MM-dd)
   * @returns {Promise} DashboardKPIResponse
   */
  getDashboardKPIs: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    return apiClient.get('/api/dashboard/kpis', { params });
  },

  /**
   * Get Harvest Quantity KPI
   * @param {string} startDate - Start date (yyyy-MM-dd)
   * @param {string} endDate - End date (yyyy-MM-dd)
   * @returns {Promise} HarvestQuantityKPI
   */
  getHarvestQuantityKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    return apiClient.get('/api/dashboard/kpi/harvest-quantity', { params });
  },

  /**
   * Get Active Batches KPI
   * @returns {Promise} ActiveBatchesKPI
   */
  getActiveBatchesKPI: () => {
    return apiClient.get('/api/dashboard/kpi/active-batches');
  },

  /**
   * Get Quality Pass Rate KPI
   * @param {string} startDate - Start date (yyyy-MM-dd)
   * @param {string} endDate - End date (yyyy-MM-dd)
   * @returns {Promise} QualityPassRateKPI
   */
  getQualityPassRateKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    return apiClient.get('/api/dashboard/kpi/quality-pass-rate', { params });
  },

  /**
   * Get Energy Consumption KPI
   * @param {string} startDate - Start date (yyyy-MM-dd)
   * @param {string} endDate - End date (yyyy-MM-dd)
   * @returns {Promise} EnergyConsumptionKPI
   */
  getEnergyConsumptionKPI: (startDate = null, endDate = null) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    return apiClient.get('/api/dashboard/kpi/energy-consumption', { params });
  },

  /**
   * Get Equipment Utilization KPI
   * @returns {Promise} EquipmentUtilizationKPI
   */
  getEquipmentUtilizationKPI: () => {
    return apiClient.get('/api/dashboard/kpi/equipment-utilization');
  },
};

export default apiClient;