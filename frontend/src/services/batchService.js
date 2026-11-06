import apiClient from './api';

const batchApi = {
  getAll: () => apiClient.get('/api/batches'),
  getById: (id) => apiClient.get(`/api/batches/${id}`),
  create: (data) => apiClient.post('/api/batches', data),
  update: (id, data) => apiClient.put(`/api/batches/${id}`, data),
  delete: (id) => apiClient.delete(`/api/batches/${id}`),
  advanceStatus: (id, data) => apiClient.post(`/api/batches/${id}/advance-status`, data),
  setStatus: (id, status) => apiClient.put(`/api/batches/${id}/status`, { status }),
  getByStatus: (status) => apiClient.get(`/api/batches/status/${status}`),
  getByFarm: (farmId) => apiClient.get(`/api/batches/farm/${farmId}`),
  getByFarmAndBlock: (farmId, blockId) => apiClient.get(`/api/batches/farm/${farmId}/block/${blockId}`),
  getByVariety: (variety) => apiClient.get(`/api/batches/variety/${variety}`),
  getReadyForShipping: () => apiClient.get('/api/batches/ready-for-shipping'),
  getByHarvestDateRange: (startDate, endDate) => apiClient.get('/api/batches/date-range', { params: { startDate, endDate } })
};

export { batchApi };