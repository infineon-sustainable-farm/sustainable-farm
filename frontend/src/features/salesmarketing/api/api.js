import { apiClient } from '../../../shared/api/client.js';

export const api = {
  // Demand Forecasts
  getDemandForecasts: () => apiClient.get('/api/demand-forecasts'),
  getDemandForecastById: (id) => apiClient.get(`/api/demand-forecasts/${id}`),
  createDemandForecast: (data) => apiClient.post('/api/demand-forecasts', data),
  updateDemandForecast: (id, data) => apiClient.put(`/api/demand-forecasts/${id}`, data),
  deleteDemandForecast: (id) => apiClient.delete(`/api/demand-forecasts/${id}`),

  // Forecast Model Runs
  getForecastModelRuns: () => apiClient.get('/api/forecast-model-runs'),
  getForecastModelRunById: (id) => apiClient.get(`/api/forecast-model-runs/${id}`),
  createForecastModelRun: (data) => apiClient.post('/api/forecast-model-runs', data),
  updateForecastModelRun: (id, data) => apiClient.put(`/api/forecast-model-runs/${id}`, data),
  deleteForecastModelRun: (id) => apiClient.delete(`/api/forecast-model-runs/${id}`),

  // Alerts
  getAlerts: () => apiClient.get('/api/alerts'),
  getAlertById: (id) => apiClient.get(`/api/alerts/${id}`),
  createAlert: (data) => apiClient.post('/api/alerts', data),
  updateAlert: (id, data) => apiClient.put(`/api/alerts/${id}`, data),
  deleteAlert: (id) => apiClient.delete(`/api/alerts/${id}`),

  // Product Batches
  getProductBatches: () => apiClient.get('/api/product-batches'),
  getProductBatchById: (id) => apiClient.get(`/api/product-batches/${id}`),
  createProductBatch: (data) => apiClient.post('/api/product-batches', data),
  updateProductBatch: (id, data) => apiClient.put(`/api/product-batches/${id}`, data),
  deleteProductBatch: (id) => apiClient.delete(`/api/product-batches/${id}`),

  // Products
  getProducts: () => apiClient.get('/api/products'),

  // Demand Reports
  getDemandReports: () => apiClient.get('/api/demand-reports'),
  getDemandReportById: (id) => apiClient.get(`/api/demand-reports/${id}`),
  createDemandReport: (data) => apiClient.post('/api/demand-reports', data),
  updateDemandReport: (id, data) => apiClient.put(`/api/demand-reports/${id}`, data),
  deleteDemandReport: (id) => apiClient.delete(`/api/demand-reports/${id}`),

  // Report Files
  getReportFiles: () => apiClient.get('/api/report-files'),
  getReportFileById: (id) => apiClient.get(`/api/report-files/${id}`),
  createReportFile: (data) => apiClient.post('/api/report-files', data),
  updateReportFile: (id, data) => apiClient.put(`/api/report-files/${id}`, data),
  deleteReportFile: (id) => apiClient.delete(`/api/report-files/${id}`),

  // Customers
  getCustomers: (status) =>
    apiClient.get('/api/customers', status ? { params: { status } } : undefined),
  getCustomerById: (id) => apiClient.get(`/api/customers/${id}`),
  createCustomer: (data) => apiClient.post('/api/customers', data),
  updateCustomer: (id, data) => apiClient.put(`/api/customers/${id}`, data),
  deleteCustomer: (id) => apiClient.delete(`/api/customers/${id}`),
    // Orders
  getOrders: () => apiClient.get('/api/orders'),
  getOrderById: (id) => apiClient.get(`/api/orders/${id}`),
  getOrderItems: () => apiClient.get('/api/order-items'),
  // Customer Contacts
  getCustomerContacts: () => apiClient.get('/api/customer-contacts'),
  getCustomerContactById: (id) => apiClient.get(`/api/customer-contacts/${id}`),
  createCustomerContact: (data) => apiClient.post('/api/customer-contacts', data),
  updateCustomerContact: (id, data) => apiClient.put(`/api/customer-contacts/${id}`, data),
  deleteCustomerContact: (id) => apiClient.delete(`/api/customer-contacts/${id}`),

  // Certifications
  getCertifications: () => apiClient.get('/api/certifications'),
  getCustomerCertifications: (customerId) => apiClient.get(`/api/customers/${customerId}/certifications`),
  linkCustomerCertification: (customerId, certificationId) =>
    apiClient.post(`/api/customers/${customerId}/certifications/${certificationId}`),
  unlinkCustomerCertification: (customerId, certificationId) =>
    apiClient.delete(`/api/customers/${customerId}/certifications/${certificationId}`),

  // Sales Channels
  getSalesChannels: () => apiClient.get('/api/sales-channels'),
  getSalesChannelById: (id) => apiClient.get(`/api/sales-channels/${id}`),
  createSalesChannel: (data) => apiClient.post('/api/sales-channels', data),
  updateSalesChannel: (id, data) => apiClient.put(`/api/sales-channels/${id}`, data),
  deleteSalesChannel: (id) => apiClient.delete(`/api/sales-channels/${id}`),
  getSalesChannelTargets: () => apiClient.get('/api/sales-channel-targets'),
  getSalesChannelTargetById: (id) => apiClient.get(`/api/sales-channel-targets/${id}`),
  createSalesChannelTarget: (data) => apiClient.post('/api/sales-channel-targets', data),
  updateSalesChannelTarget: (id, data) => apiClient.put(`/api/sales-channel-targets/${id}`, data),
  deleteSalesChannelTarget: (id) => apiClient.delete(`/api/sales-channel-targets/${id}`),

  // Pricing
  getPricingHistory: () => apiClient.get('/api/pricing-history'),
  getPricingHistoryById: (id) => apiClient.get(`/api/pricing-history/${id}`),
  createPricingHistory: (data) => apiClient.post('/api/pricing-history', data),
  updatePricingHistory: (id, data) => apiClient.put(`/api/pricing-history/${id}`, data),
  deletePricingHistory: (id) => apiClient.delete(`/api/pricing-history/${id}`),
  getCompetitorPrices: () => apiClient.get('/api/competitor-prices'),
  getCompetitorPriceById: (id) => apiClient.get(`/api/competitor-prices/${id}`),
  createCompetitorPrice: (data) => apiClient.post('/api/competitor-prices', data),
  updateCompetitorPrice: (id, data) => apiClient.put(`/api/competitor-prices/${id}`, data),
  deleteCompetitorPrice: (id) => apiClient.delete(`/api/competitor-prices/${id}`),
  getPricingRecommendations: () => apiClient.get('/api/pricing-recommendations'),
  getPricingRecommendationById: (id) => apiClient.get(`/api/pricing-recommendations/${id}`),
  createPricingRecommendation: (data) => apiClient.post('/api/pricing-recommendations', data),
  updatePricingRecommendation: (id, data) => apiClient.put(`/api/pricing-recommendations/${id}`, data),
  deletePricingRecommendation: (id) => apiClient.delete(`/api/pricing-recommendations/${id}`),

  // Campaigns
  getCampaigns: () => apiClient.get('/api/campaigns'),
  getCampaignById: (id) => apiClient.get(`/api/campaigns/${id}`),
  createCampaign: (data) => apiClient.post('/api/campaigns', data),
  updateCampaign: (id, data) => apiClient.put(`/api/campaigns/${id}`, data),
  deleteCampaign: (id) => apiClient.delete(`/api/campaigns/${id}`),
  getCampaignMetrics: () => apiClient.get('/api/campaign-metrics'),
  getCampaignMetricById: (id) => apiClient.get(`/api/campaign-metrics/${id}`),
  createCampaignMetric: (data) => apiClient.post('/api/campaign-metrics', data),
  updateCampaignMetric: (id, data) => apiClient.put(`/api/campaign-metrics/${id}`, data),
  deleteCampaignMetric: (id) => apiClient.delete(`/api/campaign-metrics/${id}`),

  // Delivery / Shipments
  getShipments: () => apiClient.get('/api/shipments'),
  getShipmentEvents: () => apiClient.get('/api/shipment-events'),
};
