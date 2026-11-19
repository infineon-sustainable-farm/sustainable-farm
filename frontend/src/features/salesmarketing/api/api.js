import { apiClient } from '../../../shared/api/client.js';

export const api = {
  // Demand Forecasts
  getDemandForecasts: () => apiClient.get('/api/sales-marketing/demand-forecasts'),
  getDemandForecastById: (id) => apiClient.get(`/api/sales-marketing/demand-forecasts/${id}`),
  createDemandForecast: (data) => apiClient.post('/api/sales-marketing/demand-forecasts', data),
  updateDemandForecast: (id, data) => apiClient.put(`/api/sales-marketing/demand-forecasts/${id}`, data),
  deleteDemandForecast: (id) => apiClient.delete(`/api/sales-marketing/demand-forecasts/${id}`),

  // Forecast Model Runs
  getForecastModelRuns: () => apiClient.get('/api/sales-marketing/forecast-model-runs'),
  getForecastModelRunById: (id) => apiClient.get(`/api/sales-marketing/forecast-model-runs/${id}`),
  createForecastModelRun: (data) => apiClient.post('/api/sales-marketing/forecast-model-runs', data),
  updateForecastModelRun: (id, data) => apiClient.put(`/api/sales-marketing/forecast-model-runs/${id}`, data),
  deleteForecastModelRun: (id) => apiClient.delete(`/api/sales-marketing/forecast-model-runs/${id}`),

  // Alerts
  getAlerts: () => apiClient.get('/api/sales-marketing/alerts'),
  getAlertById: (id) => apiClient.get(`/api/sales-marketing/alerts/${id}`),
  createAlert: (data) => apiClient.post('/api/sales-marketing/alerts', data),
  updateAlert: (id, data) => apiClient.put(`/api/sales-marketing/alerts/${id}`, data),
  deleteAlert: (id) => apiClient.delete(`/api/sales-marketing/alerts/${id}`),

  // Product Batches
  getProductBatches: () => apiClient.get('/api/sales-marketing/product-batches'),
  getProductBatchById: (id) => apiClient.get(`/api/sales-marketing/product-batches/${id}`),
  createProductBatch: (data) => apiClient.post('/api/sales-marketing/product-batches', data),
  updateProductBatch: (id, data) => apiClient.put(`/api/sales-marketing/product-batches/${id}`, data),
  deleteProductBatch: (id) => apiClient.delete(`/api/sales-marketing/product-batches/${id}`),

  // Products
  getProducts: () => apiClient.get('/api/sales-marketing/products'),

  // Demand Reports
  getDemandReports: () => apiClient.get('/api/sales-marketing/demand-reports'),
  getDemandReportById: (id) => apiClient.get(`/api/sales-marketing/demand-reports/${id}`),
  createDemandReport: (data) => apiClient.post('/api/sales-marketing/demand-reports', data),
  updateDemandReport: (id, data) => apiClient.put(`/api/sales-marketing/demand-reports/${id}`, data),
  deleteDemandReport: (id) => apiClient.delete(`/api/sales-marketing/demand-reports/${id}`),

  // Report Files
  getReportFiles: () => apiClient.get('/api/sales-marketing/report-files'),
  getReportFileById: (id) => apiClient.get(`/api/sales-marketing/report-files/${id}`),
  createReportFile: (data) => apiClient.post('/api/sales-marketing/report-files', data),
  updateReportFile: (id, data) => apiClient.put(`/api/sales-marketing/report-files/${id}`, data),
  deleteReportFile: (id) => apiClient.delete(`/api/sales-marketing/report-files/${id}`),

  // Customers
  getCustomers: (status) =>
    apiClient.get('/api/sales-marketing/customers', status ? { params: { status } } : undefined),
  getCustomerById: (id) => apiClient.get(`/api/sales-marketing/customers/${id}`),
  createCustomer: (data) => apiClient.post('/api/sales-marketing/customers', data),
  updateCustomer: (id, data) => apiClient.put(`/api/sales-marketing/customers/${id}`, data),
  deleteCustomer: (id) => apiClient.delete(`/api/sales-marketing/customers/${id}`),
    // Orders
  getOrders: () => apiClient.get('/api/sales-marketing/orders'),
  getOrderById: (id) => apiClient.get(`/api/sales-marketing/orders/${id}`),
  getOrderItems: () => apiClient.get('/api/sales-marketing/order-items'),
  // Customer Contacts
  getCustomerContacts: () => apiClient.get('/api/sales-marketing/customer-contacts'),
  getCustomerContactById: (id) => apiClient.get(`/api/sales-marketing/customer-contacts/${id}`),
  createCustomerContact: (data) => apiClient.post('/api/sales-marketing/customer-contacts', data),
  updateCustomerContact: (id, data) => apiClient.put(`/api/sales-marketing/customer-contacts/${id}`, data),
  deleteCustomerContact: (id) => apiClient.delete(`/api/sales-marketing/customer-contacts/${id}`),

  // Certifications
  getCertifications: () => apiClient.get('/api/sales-marketing/certifications'),
  getCustomerCertifications: (customerId) => apiClient.get(`/api/sales-marketing/customers/${customerId}/certifications`),
  linkCustomerCertification: (customerId, certificationId) =>
    apiClient.post(`/api/sales-marketing/customers/${customerId}/certifications/${certificationId}`),
  unlinkCustomerCertification: (customerId, certificationId) =>
    apiClient.delete(`/api/sales-marketing/customers/${customerId}/certifications/${certificationId}`),

  // Sales Channels
  getSalesChannels: () => apiClient.get('/api/sales-marketing/sales-channels'),
  getSalesChannelById: (id) => apiClient.get(`/api/sales-marketing/sales-channels/${id}`),
  createSalesChannel: (data) => apiClient.post('/api/sales-marketing/sales-channels', data),
  updateSalesChannel: (id, data) => apiClient.put(`/api/sales-marketing/sales-channels/${id}`, data),
  deleteSalesChannel: (id) => apiClient.delete(`/api/sales-marketing/sales-channels/${id}`),
  getSalesChannelTargets: () => apiClient.get('/api/sales-marketing/sales-channel-targets'),
  getSalesChannelTargetById: (id) => apiClient.get(`/api/sales-marketing/sales-channel-targets/${id}`),
  createSalesChannelTarget: (data) => apiClient.post('/api/sales-marketing/sales-channel-targets', data),
  updateSalesChannelTarget: (id, data) => apiClient.put(`/api/sales-marketing/sales-channel-targets/${id}`, data),
  deleteSalesChannelTarget: (id) => apiClient.delete(`/api/sales-marketing/sales-channel-targets/${id}`),

  // Pricing
  getPricingHistory: () => apiClient.get('/api/sales-marketing/pricing-history'),
  getPricingHistoryById: (id) => apiClient.get(`/api/sales-marketing/pricing-history/${id}`),
  createPricingHistory: (data) => apiClient.post('/api/sales-marketing/pricing-history', data),
  updatePricingHistory: (id, data) => apiClient.put(`/api/sales-marketing/pricing-history/${id}`, data),
  deletePricingHistory: (id) => apiClient.delete(`/api/sales-marketing/pricing-history/${id}`),
  getCompetitorPrices: () => apiClient.get('/api/sales-marketing/competitor-prices'),
  getCompetitorPriceById: (id) => apiClient.get(`/api/sales-marketing/competitor-prices/${id}`),
  createCompetitorPrice: (data) => apiClient.post('/api/sales-marketing/competitor-prices', data),
  updateCompetitorPrice: (id, data) => apiClient.put(`/api/sales-marketing/competitor-prices/${id}`, data),
  deleteCompetitorPrice: (id) => apiClient.delete(`/api/sales-marketing/competitor-prices/${id}`),
  getPricingRecommendations: () => apiClient.get('/api/sales-marketing/pricing-recommendations'),
  getPricingRecommendationById: (id) => apiClient.get(`/api/sales-marketing/pricing-recommendations/${id}`),
  createPricingRecommendation: (data) => apiClient.post('/api/sales-marketing/pricing-recommendations', data),
  updatePricingRecommendation: (id, data) => apiClient.put(`/api/sales-marketing/pricing-recommendations/${id}`, data),
  deletePricingRecommendation: (id) => apiClient.delete(`/api/sales-marketing/pricing-recommendations/${id}`),

  // Campaigns
  getCampaigns: () => apiClient.get('/api/sales-marketing/campaigns'),
  getCampaignById: (id) => apiClient.get(`/api/sales-marketing/campaigns/${id}`),
  createCampaign: (data) => apiClient.post('/api/sales-marketing/campaigns', data),
  updateCampaign: (id, data) => apiClient.put(`/api/sales-marketing/campaigns/${id}`, data),
  deleteCampaign: (id) => apiClient.delete(`/api/sales-marketing/campaigns/${id}`),
  getCampaignMetrics: () => apiClient.get('/api/sales-marketing/campaign-metrics'),
  getCampaignMetricById: (id) => apiClient.get(`/api/sales-marketing/campaign-metrics/${id}`),
  createCampaignMetric: (data) => apiClient.post('/api/sales-marketing/campaign-metrics', data),
  updateCampaignMetric: (id, data) => apiClient.put(`/api/sales-marketing/campaign-metrics/${id}`, data),
  deleteCampaignMetric: (id) => apiClient.delete(`/api/sales-marketing/campaign-metrics/${id}`),

  // Delivery / Shipments
  getShipments: () => apiClient.get('/api/sales-marketing/shipments'),
  getShipmentEvents: () => apiClient.get('/api/sales-marketing/shipment-events'),
};
