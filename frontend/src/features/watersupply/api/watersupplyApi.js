import { apiClient } from '../../../shared/api/client.js'

/**
 * API client pour le module watersupply - wrapper typé autour de apiClient.
 * Toutes les routes correspondent aux endpoints du backend Spring Boot.
 */

// --- Auth ---
export const authApi = {
  login: (email, password) => apiClient.post('/api/auth/login', { email, password }),
  register: (firstName, lastName, email, password) =>
    apiClient.post('/api/auth/register', { firstName, lastName, email, password }),
  logout: () => apiClient.post('/api/auth/logout'),
  forgotPassword: () => apiClient.post('/api/auth/forgot-password'),
  resetPassword: () => apiClient.post('/api/auth/reset-password'),
  refresh: () => apiClient.post('/api/auth/refresh'),
  getUsers: () => apiClient.get('/api/users'),
  getUser: (userId) => apiClient.get(`/api/users/${userId}`),
  updateUser: (userId, payload) => apiClient.put(`/api/users/${userId}`, payload),
  deleteUser: (userId) => apiClient.delete(`/api/users/${userId}`),
}

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
  // Suivi du mois courant (ou d'un mois passe) : consommation cumulee + statut par quota.
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
  // Report pour cause de pluie : proposition fondee sur la meteo, la decision reste humaine.
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
  // Economie d'eau vue dans le temps : besoin cumule des cultures vs consommation cumulee.
  getSavingsSeries: (days = 30) => apiClient.get(`/api/dashboard/savings-series?days=${days}`),
  // Bilan hydrique : entrees (pluie recuperee) vs sorties (eau consommee) et niveau des reservoirs.
  getWaterBalance: (period = 'month') => apiClient.get(`/api/dashboard/water-balance?period=${period}`),
  // Anomalies de debit = fuites probables (diagnostic deja calcule cote backend).
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
export const weatherApi = {
  getCurrent: (latitude = 10.5, longitude = -61.2) =>
    apiClient.get(`/api/weather/current?latitude=${latitude}&longitude=${longitude}`),
  getForecast: (latitude = 10.5, longitude = -61.2) =>
    apiClient.get(`/api/weather/forecast?latitude=${latitude}&longitude=${longitude}`),
}

// --- Reports ---
export const reportApi = {
  getConsumptionReport: (period = 'month') =>
    apiClient.get(`/api/reports/consumption?period=${period}`),
  getIrrigationReport: (period = 'month') =>
    apiClient.get(`/api/reports/irrigation?period=${period}`),
  getQualityReport: () => apiClient.get('/api/reports/quality'),
}

// --- Exports CSV (P10) ---
// Telechargement direct de fichiers : passe par une URL native (fetch + blob) car l'apiClient
// parse le JSON. Le token JWT est rejoue manuellement pour les memes droits que l'API.
export async function downloadCsv(kind, period = 'month') {
  const token = localStorage.getItem('access_token')
  const response = await fetch(`/api/reports/${kind}/csv?period=${encodeURIComponent(period)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
  if (!response.ok) {
    throw new Error(`Export impossible (HTTP ${response.status})`)
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
