import { apiClient } from "../../../shared/api/client";

const BASE = "/api/energy";

// Dashboard
export const getDashboardSummary = () => apiClient.get(`${BASE}/dashboard/summary`);

// Components (used for the "System Status Overview" table)
export const getComponents = () => apiClient.get(`${BASE}/components`);

// Solar & Generation
export const getSolarLast7Days = () => apiClient.get(`${BASE}/solar/last-7-days`);
export const getAllSolarLogs = () => apiClient.get(`${BASE}/solar`);

// Consumption
export const getConsumptionToday = () => apiClient.get(`${BASE}/consumption/today`);
export const getAllConsumption = () => apiClient.get(`${BASE}/consumption`);
export const getConsumptionByCategory = (category) =>
  apiClient.get(`${BASE}/consumption/breakdown/${category}`);

// Generator
export const getGeneratorEvents = () => apiClient.get(`${BASE}/generator/events`);
export const createGeneratorEvent = (event) =>
  apiClient.post(`${BASE}/generator/events`, event);

// Carbon
export const getCarbonMetrics = () => apiClient.get(`${BASE}/carbon`);

// Alerts
export const getAllAlerts = () => apiClient.get(`${BASE}/alerts`);
export const getOpenAlerts = () => apiClient.get(`${BASE}/alerts/open`);

// What-If simulation
export const simulateWhatIf = ({ additionalPanels, additionalBatteryKwh, productionScaleKgPerDay }) =>
  apiClient.post(`${BASE}/whatif/simulate`, {
    additionalPanels,
    additionalBatteryKwh,
    productionScaleKgPerDay,
  });