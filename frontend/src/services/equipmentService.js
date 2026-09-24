import apiClient from './api';

const equipmentApi = {
  getAll: () => apiClient.get('/api/equipment'),
  getById: (id) => apiClient.get(`/api/equipment/${id}`),
  create: (data) => apiClient.post('/api/equipment', data),
  update: (id, data) => apiClient.put(`/api/equipment/${id}`, data),
  delete: (id) => apiClient.delete(`/api/equipment/${id}`),
  getByType: (equipmentType) => apiClient.get(`/api/equipment/type/${equipmentType}`),
  getByStatus: (maintenanceStatus) => apiClient.get(`/api/equipment/status/${maintenanceStatus}`),
  getAvailable: () => apiClient.get('/api/equipment/available')
};

export { equipmentApi };