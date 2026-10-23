import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

export const api = {
  // Demand Forecasts
  getDemandForecasts: () => apiClient.get('/demand-forecasts'),
  getDemandForecastById: (id) => apiClient.get(`/demand-forecasts/${id}`),
  createDemandForecast: (data) => apiClient.post('/demand-forecasts', data),
  updateDemandForecast: (id, data) => apiClient.put(`/demand-forecasts/${id}`, data),
  deleteDemandForecast: (id) => apiClient.delete(`/demand-forecasts/${id}`),

  // Forecast Model Runs
  getForecastModelRuns: () => apiClient.get('/forecast-model-runs'),
  getForecastModelRunById: (id) => apiClient.get(`/forecast-model-runs/${id}`),
  createForecastModelRun: (data) => apiClient.post('/forecast-model-runs', data),
  updateForecastModelRun: (id, data) => apiClient.put(`/forecast-model-runs/${id}`, data),
  deleteForecastModelRun: (id) => apiClient.delete(`/forecast-model-runs/${id}`),

  // Alerts
  getAlerts: () => apiClient.get('/alerts'),
  getAlertById: (id) => apiClient.get(`/alerts/${id}`),
  createAlert: (data) => apiClient.post('/alerts', data),
  updateAlert: (id, data) => apiClient.put(`/alerts/${id}`, data),
  deleteAlert: (id) => apiClient.delete(`/alerts/${id}`),

  // Product Batches
  getProductBatches: () => apiClient.get('/product-batches'),
  getProductBatchById: (id) => apiClient.get(`/product-batches/${id}`),
  createProductBatch: (data) => apiClient.post('/product-batches', data),
  updateProductBatch: (id, data) => apiClient.put(`/product-batches/${id}`, data),
  deleteProductBatch: (id) => apiClient.delete(`/product-batches/${id}`),

  // Products
  getProducts: () => apiClient.get('/products'),

  // Demand Reports
  getDemandReports: () => apiClient.get('/demand-reports'),
  getDemandReportById: (id) => apiClient.get(`/demand-reports/${id}`),
  createDemandReport: (data) => apiClient.post('/demand-reports', data),
  updateDemandReport: (id, data) => apiClient.put(`/demand-reports/${id}`, data),
  deleteDemandReport: (id) => apiClient.delete(`/demand-reports/${id}`),

  // Report Files
  getReportFiles: () => apiClient.get('/report-files'),
  getReportFileById: (id) => apiClient.get(`/report-files/${id}`),
  createReportFile: (data) => apiClient.post('/report-files', data),
  updateReportFile: (id, data) => apiClient.put(`/report-files/${id}`, data),
  deleteReportFile: (id) => apiClient.delete(`/report-files/${id}`),

  // Customers
  getCustomers: (status) =>
    apiClient.get('/customers', status ? { params: { status } } : undefined),
  getCustomerById: (id) => apiClient.get(`/customers/${id}`),
  createCustomer: (data) => apiClient.post('/customers', data),
  updateCustomer: (id, data) => apiClient.put(`/customers/${id}`, data),
  deleteCustomer: (id) => apiClient.delete(`/customers/${id}`),
    // Orders
  getOrders: () => apiClient.get('/orders'),
  getOrderById: (id) => apiClient.get(`/orders/${id}`),
getOrderItems: () => apiClient.get('/order-items'),
  // Customer Contacts
  getCustomerContacts: () => apiClient.get('/customer-contacts'),
  getCustomerContactById: (id) => apiClient.get(`/customer-contacts/${id}`),
  createCustomerContact: (data) => apiClient.post('/customer-contacts', data),
  updateCustomerContact: (id, data) => apiClient.put(`/customer-contacts/${id}`, data),
  deleteCustomerContact: (id) => apiClient.delete(`/customer-contacts/${id}`),

  // Certifications
  getCertifications: () => apiClient.get('/certifications'),
  getCustomerCertifications: (customerId) => apiClient.get(`/customers/${customerId}/certifications`),
  linkCustomerCertification: (customerId, certificationId) =>
    apiClient.post(`/customers/${customerId}/certifications/${certificationId}`),
  unlinkCustomerCertification: (customerId, certificationId) =>
    apiClient.delete(`/customers/${customerId}/certifications/${certificationId}`),

  // Sales Channels
  getSalesChannels: () => apiClient.get('/sales-channels'),
  getSalesChannelById: (id) => apiClient.get(`/sales-channels/${id}`),
  createSalesChannel: (data) => apiClient.post('/sales-channels', data),
  updateSalesChannel: (id, data) => apiClient.put(`/sales-channels/${id}`, data),
  deleteSalesChannel: (id) => apiClient.delete(`/sales-channels/${id}`),
  getSalesChannelTargets: () => apiClient.get('/sales-channel-targets'),
  getSalesChannelTargetById: (id) => apiClient.get(`/sales-channel-targets/${id}`),
  createSalesChannelTarget: (data) => apiClient.post('/sales-channel-targets', data),
  updateSalesChannelTarget: (id, data) => apiClient.put(`/sales-channel-targets/${id}`, data),
  deleteSalesChannelTarget: (id) => apiClient.delete(`/sales-channel-targets/${id}`),

  // Pricing
  getPricingHistory: () => apiClient.get('/pricing-history'),
  getPricingHistoryById: (id) => apiClient.get(`/pricing-history/${id}`),
  createPricingHistory: (data) => apiClient.post('/pricing-history', data),
  updatePricingHistory: (id, data) => apiClient.put(`/pricing-history/${id}`, data),
  deletePricingHistory: (id) => apiClient.delete(`/pricing-history/${id}`),
  getCompetitorPrices: () => apiClient.get('/competitor-prices'),
  getCompetitorPriceById: (id) => apiClient.get(`/competitor-prices/${id}`),
  createCompetitorPrice: (data) => apiClient.post('/competitor-prices', data),
  updateCompetitorPrice: (id, data) => apiClient.put(`/competitor-prices/${id}`, data),
  deleteCompetitorPrice: (id) => apiClient.delete(`/competitor-prices/${id}`),
  getPricingRecommendations: () => apiClient.get('/pricing-recommendations'),
  getPricingRecommendationById: (id) => apiClient.get(`/pricing-recommendations/${id}`),
  createPricingRecommendation: (data) => apiClient.post('/pricing-recommendations', data),
  updatePricingRecommendation: (id, data) => apiClient.put(`/pricing-recommendations/${id}`, data),
  deletePricingRecommendation: (id) => apiClient.delete(`/pricing-recommendations/${id}`),

  // Campaigns
  getCampaigns: () => apiClient.get('/campaigns'),
  getCampaignById: (id) => apiClient.get(`/campaigns/${id}`),
  createCampaign: (data) => apiClient.post('/campaigns', data),
  updateCampaign: (id, data) => apiClient.put(`/campaigns/${id}`, data),
  deleteCampaign: (id) => apiClient.delete(`/campaigns/${id}`),
  getCampaignMetrics: () => apiClient.get('/campaign-metrics'),
  getCampaignMetricById: (id) => apiClient.get(`/campaign-metrics/${id}`),
  createCampaignMetric: (data) => apiClient.post('/campaign-metrics', data),
  updateCampaignMetric: (id, data) => apiClient.put(`/campaign-metrics/${id}`, data),
  deleteCampaignMetric: (id) => apiClient.delete(`/campaign-metrics/${id}`),

  // Delivery / Shipments
  getShipments: () => apiClient.get('/shipments'),
  getShipmentEvents: () => apiClient.get('/shipment-events'),
};

export default apiClient;
