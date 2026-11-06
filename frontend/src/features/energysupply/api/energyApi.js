import { apiClient } from "../../../shared/api/client";

const BASE = "/api/energy";

// Dashboard
export const getDashboardSummary = () => apiClient.get(`${BASE}/dashboard/summary`);

// Components (used for the "System Status Overview" table)
export const getComponents = () => apiClient.get(`${BASE}/components`);
export const createComponent = (component) =>
  apiClient.post(`${BASE}/components`, component);
export const updateComponent = (id, component) =>
  apiClient.put(`${BASE}/components/${id}`, component);
export const deleteComponent = (id) =>
  apiClient.delete(`${BASE}/components/${id}`);

// Solar & Generation
export const getSolarLast7Days = () => apiClient.get(`${BASE}/solar/last-7-days`);
export const getAllSolarLogs = () => apiClient.get(`${BASE}/solar`);
export const createSolarLog = (log) => apiClient.post(`${BASE}/solar`, log);
export const updateSolarLog = (id, log) => apiClient.put(`${BASE}/solar/${id}`, log);
export const deleteSolarLog = (id) => apiClient.delete(`${BASE}/solar/${id}`);

// Consumption
export const getConsumptionToday = () => apiClient.get(`${BASE}/consumption/today`);
export const getAllConsumption = () => apiClient.get(`${BASE}/consumption`);
export const getConsumptionByCategory = (category) =>
  apiClient.get(`${BASE}/consumption/breakdown/${category}`);
export const createConsumptionLog = (log) => apiClient.post(`${BASE}/consumption`, log);
export const updateConsumptionLog = (id, log) =>
  apiClient.put(`${BASE}/consumption/${id}`, log);
export const deleteConsumptionLog = (id) =>
  apiClient.delete(`${BASE}/consumption/${id}`);

// Generator
export const getGeneratorEvents = () => apiClient.get(`${BASE}/generator-events`);
export const createGeneratorEvent = (event) =>
  apiClient.post(`${BASE}/generator-events`, event);
export const updateGeneratorEvent = (id, event) =>
  apiClient.put(`${BASE}/generator-events/${id}`, event);
export const deleteGeneratorEvent = (id) =>
  apiClient.delete(`${BASE}/generator-events/${id}`);

// Carbon
export const getCarbonMetrics = () => apiClient.get(`${BASE}/carbon`);

// Alerts
export const getAllAlerts = () => apiClient.get(`${BASE}/alerts`);
export const getOpenAlerts = () => apiClient.get(`${BASE}/alerts/open`);
export const updateAlertStatus = (id, status) =>
  apiClient.patch(`${BASE}/alerts/${id}/status`, { status });

// What-If simulation
export const simulateWhatIf = ({ additionalPanels, additionalBatteryKwh, productionScaleKgPerDay }) =>
  apiClient.post(`${BASE}/whatif/simulate`, {
    additionalPanels,
    additionalBatteryKwh,
    productionScaleKgPerDay,
  });