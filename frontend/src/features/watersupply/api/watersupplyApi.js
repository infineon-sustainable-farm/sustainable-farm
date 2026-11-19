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
  getFarms: () => apiClient.get('/api/farm/farms'),
  createFarm: (farm) => apiClient.post('/api/farm/farms', farm),
  getFarm: (farmId) => apiClient.get(`/api/farm/farms/${farmId}`),
  updateFarm: (farmId, payload) => apiClient.put(`/api/farm/farms/${farmId}`, payload),
  deleteFarm: (farmId) => apiClient.delete(`/api/farm/farms/${farmId}`),
  getFarmFields: (farmId) => apiClient.get(`/api/farm/farms/${farmId}/fields`),
}

// --- Fields ---
export const fieldApi = {
  getFields: () => apiClient.get('/api/farm/fields'),
  createField: (field) => apiClient.post('/api/farm/fields', field),
  getField: (fieldId) => apiClient.get(`/api/farm/fields/${fieldId}`),
  updateField: (fieldId, payload) => apiClient.put(`/api/farm/fields/${fieldId}`, payload),
  deleteField: (fieldId) => apiClient.delete(`/api/farm/fields/${fieldId}`),
  getFieldZones: (fieldId) => apiClient.get(`/api/farm/fields/${fieldId}/zones`),
}

// --- Zones ---
export const zoneApi = {
  getZones: () => apiClient.get('/api/farm/zones'),
  createZone: (zone) => apiClient.post('/api/farm/zones', zone),
  getZone: (zoneId) => apiClient.get(`/api/farm/zones/${zoneId}`),
  updateZone: (zoneId, payload) => apiClient.put(`/api/farm/zones/${zoneId}`, payload),
  deleteZone: (zoneId) => apiClient.delete(`/api/farm/zones/${zoneId}`),
}

// --- Water Sources ---
export const waterSourceApi = {
  getSources: () => apiClient.get('/api/water-supply/sources'),
  createSource: (source) => apiClient.post('/api/water-supply/sources', source),
  getSource: (sourceId) => apiClient.get(`/api/water-supply/sources/${sourceId}`),
  updateSource: (sourceId, payload) => apiClient.put(`/api/water-supply/sources/${sourceId}`, payload),
  deleteSource: (sourceId) => apiClient.delete(`/api/water-supply/sources/${sourceId}`),
  // Every sensor reading and manual level change, oldest first (the source only keeps the last one).
  getLevelHistory: (sourceId) => apiClient.get(`/api/water-supply/sources/${sourceId}/level-history`),
}

// --- Water Consumption ---
export const waterConsumptionApi = {
  getConsumptions: () => apiClient.get('/api/water-supply/consumption'),
  createConsumption: (consumption) => apiClient.post('/api/water-supply/consumption', consumption),
  getConsumption: (consumptionId) => apiClient.get(`/api/water-supply/consumption/${consumptionId}`),
  updateConsumption: (consumptionId, payload) => apiClient.put(`/api/water-supply/consumption/${consumptionId}`, payload),
  deleteConsumption: (consumptionId) => apiClient.delete(`/api/water-supply/consumption/${consumptionId}`),
}

// --- Water Quotas (P8) ---
export const waterQuotaApi = {
  getQuotas: (params = '') => apiClient.get(`/api/water-supply/quotas${params}`),
  createQuota: (quota) => apiClient.post('/api/water-supply/quotas', quota),
  getQuota: (quotaId) => apiClient.get(`/api/water-supply/quotas/${quotaId}`),
  updateQuota: (quotaId, payload) => apiClient.put(`/api/water-supply/quotas/${quotaId}`, payload),
  deleteQuota: (quotaId) => apiClient.delete(`/api/water-supply/quotas/${quotaId}`),
  // Current month tracking (or a past month): cumulative usage + status per quota.
  getUsage: (month) => apiClient.get(`/api/water-supply/quotas/usage${month ? `?month=${month}` : ''}`),
}

// --- Water Quality Tests ---
export const waterQualityApi = {
  getTests: () => apiClient.get('/api/water-supply/quality'),
  createTest: (test) => apiClient.post('/api/water-supply/quality', test),
  getTest: (testId) => apiClient.get(`/api/water-supply/quality/${testId}`),
  updateTest: (testId, payload) => apiClient.put(`/api/water-supply/quality/${testId}`, payload),
  deleteTest: (testId) => apiClient.delete(`/api/water-supply/quality/${testId}`),
}

// --- Irrigation ---
export const irrigationApi = {
  getSchedules: () => apiClient.get('/api/water-supply/irrigations'),
  createSchedule: (schedule) => apiClient.post('/api/water-supply/irrigations', schedule),
  getSchedule: (scheduleId) => apiClient.get(`/api/water-supply/irrigations/${scheduleId}`),
  updateSchedule: (scheduleId, payload) => apiClient.put(`/api/water-supply/irrigations/${scheduleId}`, payload),
  deleteSchedule: (scheduleId) => apiClient.delete(`/api/water-supply/irrigations/${scheduleId}`),
  getLogs: () => apiClient.get('/api/water-supply/irrigation-logs'),
  createLog: (log) => apiClient.post('/api/water-supply/irrigation-logs', log),
  updateLog: (logId, payload) => apiClient.put(`/api/water-supply/irrigation-logs/${logId}`, payload),
  deleteLog: (logId) => apiClient.delete(`/api/water-supply/irrigation-logs/${logId}`),
  startIrrigation: (scheduleId) => apiClient.post(`/api/water-supply/irrigations/${scheduleId}/start`),
  stopIrrigation: (scheduleId) => apiClient.post(`/api/water-supply/irrigations/${scheduleId}/stop`),
  // Postpone because of rain: suggestion based on the weather, the decision stays human.
  getSuggestions: () => apiClient.get('/api/water-supply/irrigation/suggestions'),
  postpone: (scheduleId, reason) => apiClient.post(`/api/water-supply/irrigations/${scheduleId}/postpone`, { reason }),
}

// --- Notifications ---
export const notificationApi = {
  getNotifications: () => apiClient.get('/api/water-supply/notifications'),
  createNotification: (notification) => apiClient.post('/api/water-supply/notifications', notification),
  getNotification: (notificationId) => apiClient.get(`/api/water-supply/notifications/${notificationId}`),
  markAsRead: (notificationId) => apiClient.patch(`/api/water-supply/notifications/${notificationId}/read`),
  markAllAsRead: () => apiClient.patch('/api/water-supply/notifications/read-all'),
  deleteNotification: (notificationId) => apiClient.delete(`/api/water-supply/notifications/${notificationId}`),
}

// --- Rainwater Harvest ---
export const rainwaterHarvestApi = {
  getHarvests: () => apiClient.get('/api/water-supply/rainwater-harvests'),
  createHarvest: (harvest) => apiClient.post('/api/water-supply/rainwater-harvests', harvest),
  getHarvest: (harvestId) => apiClient.get(`/api/water-supply/rainwater-harvests/${harvestId}`),
  updateHarvest: (harvestId, payload) => apiClient.put(`/api/water-supply/rainwater-harvests/${harvestId}`, payload),
  deleteHarvest: (harvestId) => apiClient.delete(`/api/water-supply/rainwater-harvests/${harvestId}`),
  getCoverage: (period = 'month') => apiClient.get(`/api/water-supply/rainwater-harvests/coverage?period=${period}`),
}

// --- Drip Maintenance ---
export const dripMaintenanceApi = {
  getLogs: () => apiClient.get('/api/water-supply/drip-maintenance-logs'),
  createLog: (log) => apiClient.post('/api/water-supply/drip-maintenance-logs', log),
  getLog: (logId) => apiClient.get(`/api/water-supply/drip-maintenance-logs/${logId}`),
  updateLog: (logId, payload) => apiClient.put(`/api/water-supply/drip-maintenance-logs/${logId}`, payload),
  deleteLog: (logId) => apiClient.delete(`/api/water-supply/drip-maintenance-logs/${logId}`),
  getSchedule: () => apiClient.get('/api/water-supply/drip-maintenance-logs/schedule'),
}

// --- Dashboard ---
export const dashboardApi = {
  getKpis: () => apiClient.get('/api/water-supply/dashboard/kpis'),
  getWaterSavings: (period = 'month') => apiClient.get(`/api/water-supply/dashboard/water-savings?period=${period}`),
  // Water savings over time: cumulative crop need vs cumulative consumption.
  getSavingsSeries: (days = 30) => apiClient.get(`/api/water-supply/dashboard/savings-series?days=${days}`),
  // Water balance: inputs (harvested rain) vs outputs (consumed water) and tank levels.
  getWaterBalance: (period = 'month') => apiClient.get(`/api/water-supply/dashboard/water-balance?period=${period}`),
  // Flow anomalies = probable leaks (diagnostic already computed backend side).
  getLeaks: () => apiClient.get('/api/water-supply/dashboard/leaks'),
  getActivities: () => apiClient.get('/api/water-supply/dashboard/activities'),
  getAlerts: () => apiClient.get('/api/water-supply/dashboard/alerts'),
}

// --- AI ---
export const aiApi = {
  getRecommendations: () => apiClient.get('/api/water-supply/ai/recommendations'),
  getDroughtPrediction: () => apiClient.get('/api/water-supply/ai/drought-prediction'),
  analyze: () => apiClient.post('/api/water-supply/ai/analyze'),
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
  getCurrent: (latitude, longitude) => apiClient.get(withCoordinates('/api/water-supply/weather/current', latitude, longitude)),
  getForecast: (latitude, longitude) => apiClient.get(withCoordinates('/api/water-supply/weather/forecast', latitude, longitude)),
}

// --- Reports ---
export const reportApi = {
  getConsumptionReport: (period = 'month') =>
    apiClient.get(`/api/water-supply/reports/consumption?period=${period}`),
  getIrrigationReport: (period = 'month') =>
    apiClient.get(`/api/water-supply/reports/irrigation?period=${period}`),
  getQualityReport: () => apiClient.get('/api/water-supply/reports/quality'),
}

// --- CSV exports (P10) ---
// Direct file download: goes through a native URL (fetch + blob) because apiClient
// parses JSON. No authentication header: the module has none (see the Auth section).
// Only the exports the backend serves: the kind is part of the path, so nothing else reaches fetch.
const CSV_KINDS = ['consumption', 'irrigation', 'quality']

export async function downloadCsv(kind, period = 'month') {
  if (!CSV_KINDS.includes(kind)) {
    throw new Error(`Unknown export: ${kind}`)
  }
  const response = await fetch(`/api/water-supply/reports/${kind}/csv?period=${encodeURIComponent(period)}`)
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

// --- Inter-module integrations (contracts: docs/integrations/) ---
// Query strings are built by axios from `params`: undefined values are dropped, so optional
// filters can be passed as-is.

// Plants blocks <-> Water Supply zones.
export const zoneBlockMappingApi = {
  getMappings: (params) => apiClient.get('/api/farm/zone-block-mappings', { params }),
  createMapping: (mapping) => apiClient.post('/api/farm/zone-block-mappings', mapping),
  updateMapping: (mappingId, payload) => apiClient.put(`/api/farm/zone-block-mappings/${mappingId}`, payload),
  deleteMapping: (mappingId) => apiClient.delete(`/api/farm/zone-block-mappings/${mappingId}`),
}

export const plantsIntegrationApi = {
  // from / to: days (yyyy-MM-dd), both included.
  getRainfall: (blockCode, from, to) => apiClient.get('/api/water-supply/plants/rainfall', { params: { blockCode, from, to } }),
  getIrrigationNeed: (request) => apiClient.post('/api/water-supply/plants/irrigation-need', request),
}

// Planned water unavailability (outage / maintenance) on a farm, a source or a zone.
export const outageApi = {
  getOutages: (params) => apiClient.get('/api/water-supply/outages', { params }),
  createOutage: (outage) => apiClient.post('/api/water-supply/outages', outage),
  updateOutage: (outageId, payload) => apiClient.put(`/api/water-supply/outages/${outageId}`, payload),
  deleteOutage: (outageId) => apiClient.delete(`/api/water-supply/outages/${outageId}`),
}

// What Visitor Management reads, previewed by the water team.
export const visitorApi = {
  getSummary: (params) => apiClient.get('/api/water-supply/visitor/summary', { params }),
  getSafetyBriefing: () => apiClient.get('/api/water-supply/visitor/safety-briefing'),
  publishSafetyBriefing: (briefing) => apiClient.patch('/api/water-supply/visitor/safety-briefing', briefing),
}

export const productTransformationApi = {
  getConsumptionByLot: (lotId) => apiClient.get('/api/water-supply/consumption/by-lot', { params: { lotId } }),
  getQualityCompliance: (lotId) => apiClient.get('/api/water-supply/quality/compliance', { params: { lotId } }),
  attachLotUsage: (request) => apiClient.post('/api/water-supply/lot-usage', request),
  forecastAvailability: (request) => apiClient.post('/api/water-supply/product-transformation/availability-forecast', request),
}

// The processing batches belong to Product Transformation (OWNER_LINKS.processingBatch): a lot is
// picked in its list, never typed.
export const processingBatchApi = {
  getBatches: () => apiClient.get('/api/product-transformation/batches'),
}

// The variety catalogue belongs to Plants (OWNER_LINKS.variety): the crop type of a field is picked
// in it, by key.
export const varietyCatalogApi = {
  getVarieties: () => apiClient.get('/api/plants/variety-references'),
}

// Outbound valve commands: 503 VALVE_GATEWAY_UNAVAILABLE until a device gateway is configured.
export const iotCommandApi = {
  sendCommand: (deviceId, command) => apiClient.post(`/api/water-supply/iot/devices/${encodeURIComponent(deviceId)}/commands`, command),
  getCommands: (deviceId) => apiClient.get(`/api/water-supply/iot/devices/${encodeURIComponent(deviceId)}/commands`),
}

// --- Health ---
export const healthApi = {
  check: () => apiClient.get('/api/water-supply/health'),
}
