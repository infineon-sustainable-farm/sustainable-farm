import { apiClient } from '../../../shared/api/client.js'

/**
 * API client for the watersupply module - typed wrapper around apiClient.
 * Every route matches a Spring Boot backend endpoint.
 */

// --- Auth ---
// No authentication client in this module: the backend exposes no /api/auth endpoint
// (checked server side) and login will be provided by the global platform. An authApi
// block used to live here and called routes that did not exist, which made it look wired.

// --- Farms ---
export const farmApi = {
  getFarms: () => apiClient.get('/api/farms'),
  createFarm: (farm) => apiClient.post('/api/farms', farm),
  getFarm: (farmId) => apiClient.get(`/api/farms/${farmId}`),
  updateFarm: (farmId, payload) => apiClient.put(`/api/farms/${farmId}`, payload),
  deleteFarm: (farmId) => apiClient.delete(`/api/farms/${farmId}`),
  getFarmFields: (farmId) => apiClient.get(`/api/farms/${farmId}/fields`),
}

// --- Fields ---
export const fieldApi = {
  getFields: () => apiClient.get('/api/fields'),
  createField: (field) => apiClient.post('/api/fields', field),
  getField: (fieldId) => apiClient.get(`/api/fields/${fieldId}`),
  updateField: (fieldId, payload) => apiClient.put(`/api/fields/${fieldId}`, payload),
  deleteField: (fieldId) => apiClient.delete(`/api/fields/${fieldId}`),
  getFieldZones: (fieldId) => apiClient.get(`/api/fields/${fieldId}/zones`),
}

// --- Zones ---
export const zoneApi = {
  getZones: () => apiClient.get('/api/zones'),
  createZone: (zone) => apiClient.post('/api/zones', zone),
  getZone: (zoneId) => apiClient.get(`/api/zones/${zoneId}`),
  updateZone: (zoneId, payload) => apiClient.put(`/api/zones/${zoneId}`, payload),
  deleteZone: (zoneId) => apiClient.delete(`/api/zones/${zoneId}`),
}

// --- Water Sources ---
export const waterSourceApi = {
  getSources: () => apiClient.get('/api/water/sources'),
  createSource: (source) => apiClient.post('/api/water/sources', source),
  getSource: (sourceId) => apiClient.get(`/api/water/sources/${sourceId}`),
  updateSource: (sourceId, payload) => apiClient.put(`/api/water/sources/${sourceId}`, payload),
  deleteSource: (sourceId) => apiClient.delete(`/api/water/sources/${sourceId}`),
}

// --- Water Consumption ---
export const waterConsumptionApi = {
  getConsumptions: () => apiClient.get('/api/water/consumption'),
  createConsumption: (consumption) => apiClient.post('/api/water/consumption', consumption),
  getConsumption: (consumptionId) => apiClient.get(`/api/water/consumption/${consumptionId}`),
  updateConsumption: (consumptionId, payload) => apiClient.put(`/api/water/consumption/${consumptionId}`, payload),
  deleteConsumption: (consumptionId) => apiClient.delete(`/api/water/consumption/${consumptionId}`),
}

// --- Water Quotas (P8) ---
export const waterQuotaApi = {
  getQuotas: (params = '') => apiClient.get(`/api/water/quotas${params}`),
  createQuota: (quota) => apiClient.post('/api/water/quotas', quota),
  getQuota: (quotaId) => apiClient.get(`/api/water/quotas/${quotaId}`),
  updateQuota: (quotaId, payload) => apiClient.put(`/api/water/quotas/${quotaId}`, payload),
  deleteQuota: (quotaId) => apiClient.delete(`/api/water/quotas/${quotaId}`),
  // Current month tracking (or a past month): cumulative usage + status per quota.
  getUsage: (month) => apiClient.get(`/api/water/quotas/usage${month ? `?month=${month}` : ''}`),
}

// --- Water Quality Tests ---
export const waterQualityApi = {
  getTests: () => apiClient.get('/api/water/quality'),
  createTest: (test) => apiClient.post('/api/water/quality', test),
  getTest: (testId) => apiClient.get(`/api/water/quality/${testId}`),
  updateTest: (testId, payload) => apiClient.put(`/api/water/quality/${testId}`, payload),
  deleteTest: (testId) => apiClient.delete(`/api/water/quality/${testId}`),
}

// --- Irrigation ---
export const irrigationApi = {
  getSchedules: () => apiClient.get('/api/irrigations'),
  createSchedule: (schedule) => apiClient.post('/api/irrigations', schedule),
  getSchedule: (scheduleId) => apiClient.get(`/api/irrigations/${scheduleId}`),
  updateSchedule: (scheduleId, payload) => apiClient.put(`/api/irrigations/${scheduleId}`, payload),
  deleteSchedule: (scheduleId) => apiClient.delete(`/api/irrigations/${scheduleId}`),
  getLogs: () => apiClient.get('/api/irrigation-logs'),
  createLog: (log) => apiClient.post('/api/irrigation-logs', log),
  updateLog: (logId, payload) => apiClient.put(`/api/irrigation-logs/${logId}`, payload),
  deleteLog: (logId) => apiClient.delete(`/api/irrigation-logs/${logId}`),
  startIrrigation: (scheduleId) => apiClient.post(`/api/irrigations/${scheduleId}/start`),
  stopIrrigation: (scheduleId) => apiClient.post(`/api/irrigations/${scheduleId}/stop`),
  // Postpone because of rain: suggestion based on the weather, the decision stays human.
  getSuggestions: () => apiClient.get('/api/irrigation/suggestions'),
  postpone: (scheduleId, reason) => apiClient.post(`/api/irrigations/${scheduleId}/postpone`, { reason }),
}

// --- Notifications ---
export const notificationApi = {
  getNotifications: () => apiClient.get('/api/notifications'),
  createNotification: (notification) => apiClient.post('/api/notifications', notification),
  getNotification: (notificationId) => apiClient.get(`/api/notifications/${notificationId}`),
  markAsRead: (notificationId) => apiClient.patch(`/api/notifications/${notificationId}/read`),
  markAllAsRead: () => apiClient.patch('/api/notifications/read-all'),
  deleteNotification: (notificationId) => apiClient.delete(`/api/notifications/${notificationId}`),
}

// --- Rainwater Harvest ---
export const rainwaterHarvestApi = {
  getHarvests: () => apiClient.get('/api/rainwater-harvests'),
  createHarvest: (harvest) => apiClient.post('/api/rainwater-harvests', harvest),
  getHarvest: (harvestId) => apiClient.get(`/api/rainwater-harvests/${harvestId}`),
  updateHarvest: (harvestId, payload) => apiClient.put(`/api/rainwater-harvests/${harvestId}`, payload),
  deleteHarvest: (harvestId) => apiClient.delete(`/api/rainwater-harvests/${harvestId}`),
  getCoverage: (period = 'month') => apiClient.get(`/api/rainwater-harvests/coverage?period=${period}`),
}

// --- Drip Maintenance ---
export const dripMaintenanceApi = {
  getLogs: () => apiClient.get('/api/drip-maintenance-logs'),
  createLog: (log) => apiClient.post('/api/drip-maintenance-logs', log),
  getLog: (logId) => apiClient.get(`/api/drip-maintenance-logs/${logId}`),
  updateLog: (logId, payload) => apiClient.put(`/api/drip-maintenance-logs/${logId}`, payload),
  deleteLog: (logId) => apiClient.delete(`/api/drip-maintenance-logs/${logId}`),
  getSchedule: () => apiClient.get('/api/drip-maintenance-logs/schedule'),
}

// --- Dashboard ---
export const dashboardApi = {
  getKpis: () => apiClient.get('/api/dashboard/kpis'),
  getWaterSavings: (period = 'month') => apiClient.get(`/api/dashboard/water-savings?period=${period}`),
  // Water savings over time: cumulative crop need vs cumulative consumption.
  getSavingsSeries: (days = 30) => apiClient.get(`/api/dashboard/savings-series?days=${days}`),
  // Water balance: inputs (harvested rain) vs outputs (consumed water) and tank levels.
  getWaterBalance: (period = 'month') => apiClient.get(`/api/dashboard/water-balance?period=${period}`),
  // Flow anomalies = probable leaks (diagnostic already computed backend side).
  getLeaks: () => apiClient.get('/api/dashboard/leaks'),
  getActivities: () => apiClient.get('/api/dashboard/activities'),
  getAlerts: () => apiClient.get('/api/dashboard/alerts'),
}

// --- AI ---
export const aiApi = {
  getRecommendations: () => apiClient.get('/api/ai/recommendations'),
  getDroughtPrediction: () => apiClient.get('/api/ai/drought-prediction'),
  analyze: () => apiClient.post('/api/ai/analyze'),
}

// --- Weather ---
// Default coordinates come from the backend (app.weather.latitude / app.weather.longitude,
// Banfora site): the front only sends them when a screen provides them explicitly. A screen
// without parameters can therefore no longer query another location's weather by mistake.
function withCoordinates(path, latitude, longitude) {
  if (latitude == null || longitude == null) return path
  return `${path}?latitude=${latitude}&longitude=${longitude}`
}

export const weatherApi = {
  getCurrent: (latitude, longitude) => apiClient.get(withCoordinates('/api/weather/current', latitude, longitude)),
  getForecast: (latitude, longitude) => apiClient.get(withCoordinates('/api/weather/forecast', latitude, longitude)),
}

// --- Reports ---
export const reportApi = {
  getConsumptionReport: (period = 'month') =>
    apiClient.get(`/api/reports/consumption?period=${period}`),
  getIrrigationReport: (period = 'month') =>
    apiClient.get(`/api/reports/irrigation?period=${period}`),
  getQualityReport: () => apiClient.get('/api/reports/quality'),
}

// --- CSV exports (P10) ---
// Direct file download: goes through a native URL (fetch + blob) because apiClient
// parses JSON. No authentication header: the module has none (see the Auth section).
export async function downloadCsv(kind, period = 'month') {
  const response = await fetch(`/api/reports/${kind}/csv?period=${encodeURIComponent(period)}`)
  if (!response.ok) {
    throw new Error(`Export failed (HTTP ${response.status})`)
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${kind}-${period}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

// --- Health ---
export const healthApi = {
  check: () => apiClient.get('/api/health'),
}
