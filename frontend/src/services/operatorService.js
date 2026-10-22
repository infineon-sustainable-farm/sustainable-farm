import apiClient from './api';

const operatorApi = {
  getAll: () => apiClient.get('/api/operators'),
  getById: (id) => apiClient.get(`/api/operators/${id}`),
  create: (data) => apiClient.post('/api/operators', data),
  update: (id, data) => apiClient.put(`/api/operators/${id}`, data),
  delete: (id) => apiClient.delete(`/api/operators/${id}`),
  getByRole: (role) => apiClient.get(`/api/operators/role/${role}`),
  getByStatus: (activeStatus) => apiClient.get(`/api/operators/status/${activeStatus}`),
  getActive: () => apiClient.get('/api/operators/active'),
  getCertifiedByRole: (role) => apiClient.get(`/api/operators/certified/${role}`)
};

export { operatorApi };