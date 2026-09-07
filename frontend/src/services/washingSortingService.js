import apiClient from './api';

const washingSortingApi = {
  getAll: () => apiClient.get('/api/wash-sort-records'),
  getById: (id) => apiClient.get(`/api/wash-sort-records/${id}`),
  create: (data) => apiClient.post('/api/wash-sort-records', data),
  update: (id, data) => apiClient.put(`/api/wash-sort-records/${id}`, data),
  delete: (id) => apiClient.delete(`/api/wash-sort-records/${id}`),
  complete: (id, data) => apiClient.post(`/api/wash-sort-records/${id}/complete`, data),
  getByBatchId: (batchId) => apiClient.get(`/api/wash-sort-records/batch/${batchId}`),
  getByDateRange: (startDate, endDate) => apiClient.get('/api/wash-sort-records/date-range', { params: { startDate, endDate } }),
  getByEquipmentId: (equipmentId) => apiClient.get(`/api/wash-sort-records/equipment/${equipmentId}`),
  getByOperatorId: (operatorId) => apiClient.get(`/api/wash-sort-records/operator/${operatorId}`)
};

export { washingSortingApi };