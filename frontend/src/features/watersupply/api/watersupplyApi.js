import { apiClient } from '../../../shared/api/client.js'

/**
 * API client pour le module watersupply - wrapper typé autour de apiClient.
 * Toutes les routes correspondent aux endpoints du backend Spring Boot.
 */

// --- Auth ---
export const authApi = {
  login: (email, password) => apiClient.post('/auth/login', { email, password }),
  register: (firstName, lastName, email, password) =>
    apiClient.post('/auth/register', { firstName, lastName, email, password }),
  logout: () => apiClient.post('/auth/logout'),
  forgotPassword: () => apiClient.post('/auth/forgot-password'),
  resetPassword: () => apiClient.post('/auth/reset-password'),
  refresh: () => apiClient.post('/auth/refresh'),
  getUsers: () => apiClient.get('/users'),
  getUser: (userId) => apiClient.get(`/users/${userId}`),
  updateUser: (userId, payload) => apiClient.put(`/users/${userId}`, payload),
  deleteUser: (userId) => apiClient.delete(`/users/${userId}`),
}

// --- Farms ---
export const farmApi = {
  getFarms: () => apiClient.get('/farms'),
  createFarm: (farm) => apiClient.post('/farms', farm),
  getFarm: (farmId) => apiClient.get(`/farms/${farmId}`),
  updateFarm: (farmId, payload) => apiClient.put(`/farms/${farmId}`, payload),
  deleteFarm: (farmId) => apiClient.delete(`/farms/${farmId}`),
  getFarmFields: (farmId) => apiClient.get(`/farms/${farmId}/fields`),
}

// --- Fields ---
export const fieldApi = {
  getFields: () => apiClient.get('/fields'),
  createField: (field) => apiClient.post('/fields', field),
  getField: (fieldId) => apiClient.get(`/fields/${fieldId}`),
  updateField: (fieldId, payload) => apiClient.put(`/fields/${fieldId}`, payload),
  deleteField: (fieldId) => apiClient.delete(`/fields/${fieldId}`),
  getFieldZones: (fieldId) => apiClient.get(`/fields/${fieldId}/zones`),
}

// --- Zones ---
export const zoneApi = {
  getZones: () => apiClient.get('/zones'),
  createZone: (zone) => apiClient.post('/zones', zone),
  getZone: (zoneId) => apiClient.get(`/zones/${zoneId}`),
  updateZone: (zoneId, payload) => apiClient.put(`/zones/${zoneId}`, payload),
  deleteZone: (zoneId) => apiClient.delete(`/zones/${zoneId}`),
}

// --- Water Sources ---
export const waterSourceApi = {
  getSources: () => apiClient.get('/water/sources'),
  createSource: (source) => apiClient.post('/water/sources', source),
  getSource: (sourceId) => apiClient.get(`/water/sources/${sourceId}`),
  updateSource: (sourceId, payload) => apiClient.put(`/water/sources/${sourceId}`, payload),
  deleteSource: (sourceId) => apiClient.delete(`/water/sources/${sourceId}`),
}

// --- Water Consumption ---
export const waterConsumptionApi = {
  getConsumptions: () => apiClient.get('/water/consumption'),
  createConsumption: (consumption) => apiClient.post('/water/consumption', consumption),
  getConsumption: (consumptionId) => apiClient.get(`/water/consumption/${consumptionId}`),
  updateConsumption: (consumptionId, payload) => apiClient.put(`/water/consumption/${consumptionId}`, payload),
  deleteConsumption: (consumptionId) => apiClient.delete(`/water/consumption/${consumptionId}`),
}

// --- Water Quality Tests ---
export const waterQualityApi = {
  getTests: () => apiClient.get('/water/quality'),
  createTest: (test) => apiClient.post('/water/quality', test),
  getTest: (testId) => apiClient.get(`/water/quality/${testId}`),
  updateTest: (testId, payload) => apiClient.put(`/water/quality/${testId}`, payload),
  deleteTest: (testId) => apiClient.delete(`/water/quality/${testId}`),
}

// --- Irrigation ---
export const irrigationApi = {
  getSchedules: () => apiClient.get('/irrigations'),
  createSchedule: (schedule) => apiClient.post('/irrigations', schedule),
  getSchedule: (scheduleId) => apiClient.get(`/irrigations/${scheduleId}`),
  updateSchedule: (scheduleId, payload) => apiClient.put(`/irrigations/${scheduleId}`, payload),
  deleteSchedule: (scheduleId) => apiClient.delete(`/irrigations/${scheduleId}`),
  getLogs: () => apiClient.get('/irrigation-logs'),
  startIrrigation: (scheduleId) => apiClient.post(`/irrigations/${scheduleId}/start`),
  stopIrrigation: (scheduleId) => apiClient.post(`/irrigations/${scheduleId}/stop`),
}

// --- Notifications ---
export const notificationApi = {
  getNotifications: () => apiClient.get('/notifications'),
  createNotification: (notification) => apiClient.post('/notifications', notification),
  getNotification: (notificationId) => apiClient.get(`/notifications/${notificationId}`),
  markAsRead: (notificationId) => apiClient.patch(`/notifications/${notificationId}/read`),
  markAllAsRead: () => apiClient.patch('/notifications/read-all'),
  deleteNotification: (notificationId) => apiClient.delete(`/notifications/${notificationId}`),
}

// --- Rainwater Harvest ---
export const rainwaterHarvestApi = {
  getHarvests: () => apiClient.get('/rainwater-harvests'),
  createHarvest: (harvest) => apiClient.post('/rainwater-harvests', harvest),
  getHarvest: (harvestId) => apiClient.get(`/rainwater-harvests/${harvestId}`),
  updateHarvest: (harvestId, payload) => apiClient.put(`/rainwater-harvests/${harvestId}`, payload),
  deleteHarvest: (harvestId) => apiClient.delete(`/rainwater-harvests/${harvestId}`),
  getCoverage: (period = 'month') => apiClient.get(`/rainwater-harvests/coverage?period=${period}`),
}

// --- Drip Maintenance ---
export const dripMaintenanceApi = {
  getLogs: () => apiClient.get('/drip-maintenance-logs'),
  createLog: (log) => apiClient.post('/drip-maintenance-logs', log),
  getLog: (logId) => apiClient.get(`/drip-maintenance-logs/${logId}`),
  updateLog: (logId, payload) => apiClient.put(`/drip-maintenance-logs/${logId}`, payload),
  deleteLog: (logId) => apiClient.delete(`/drip-maintenance-logs/${logId}`),
  getSchedule: () => apiClient.get('/drip-maintenance-logs/schedule'),
}

// --- Dashboard ---
export const dashboardApi = {
  getKpis: () => apiClient.get('/dashboard/kpis'),
  getWaterSavings: (period = 'month') => apiClient.get(`/dashboard/water-savings?period=${period}`),
  getActivities: () => apiClient.get('/dashboard/activities'),
  getAlerts: () => apiClient.get('/dashboard/alerts'),
}

// --- AI ---
export const aiApi = {
  getRecommendations: () => apiClient.get('/ai/recommendations'),
  getDroughtPrediction: () => apiClient.get('/ai/drought-prediction'),
  analyze: () => apiClient.post('/ai/analyze'),
}

// --- Weather ---
export const weatherApi = {
  getCurrent: (latitude = 10.5, longitude = -61.2) =>
    apiClient.get(`/weather/current?latitude=${latitude}&longitude=${longitude}`),
  getForecast: (latitude = 10.5, longitude = -61.2) =>
    apiClient.get(`/weather/forecast?latitude=${latitude}&longitude=${longitude}`),
}

// --- Reports ---
export const reportApi = {
  getConsumptionReport: (period = 'month') =>
    apiClient.get(`/reports/consumption?period=${period}`),
  getIrrigationReport: (period = 'month') =>
    apiClient.get(`/reports/irrigation?period=${period}`),
  getQualityReport: () => apiClient.get('/reports/quality'),
}

// --- Health ---
export const healthApi = {
  check: () => apiClient.get('/health'),
}
