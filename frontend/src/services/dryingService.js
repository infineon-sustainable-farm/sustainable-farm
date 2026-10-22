import apiClient from './api';

const dryingApi = {
  getAll: () => apiClient.get('/api/drying-runs'),
  getById: (id) => apiClient.get(`/api/drying-runs/${id}`),
  create: (data) => apiClient.post('/api/drying-runs', data),
  update: (id, data) => apiClient.put(`/api/drying-runs/${id}`, data),
  delete: (id) => apiClient.delete(`/api/drying-runs/${id}`),
  getByBatchId: (batchId) => apiClient.get(`/api/drying-runs/batch/${batchId}`),
  getByDateRange: (startDate, endDate) => apiClient.get('/api/drying-runs/date-range', { params: { startDate, endDate } }),
  getByEquipmentId: (equipmentId) => apiClient.get(`/api/drying-runs/equipment/${equipmentId}`),
  getByOperatorId: (operatorId) => apiClient.get(`/api/drying-runs/operator/${operatorId}`)
};

export { dryingApi };