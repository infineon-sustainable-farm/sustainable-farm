import apiClient from './api';

const harvestApi = {
  getAll: () => apiClient.get('/api/harvest-events'),
  getById: (id) => apiClient.get(`/api/harvest-events/${id}`),
  create: (data) => apiClient.post('/api/harvest-events', data),
  update: (id, data) => apiClient.put(`/api/harvest-events/${id}`, data),
  delete: (id) => apiClient.delete(`/api/harvest-events/${id}`),
  getByDateRange: (startDate, endDate) => apiClient.get('/api/harvest-events/date-range', { params: { startDate, endDate } }),
  getByVariety: (variety) => apiClient.get(`/api/harvest-events/variety/${variety}`),
  getByGrade: (grade) => apiClient.get(`/api/harvest-events/grade/${grade}`),
  getByFarm: (farmId) => apiClient.get(`/api/harvest-events/farm/${farmId}`)
};

export { harvestApi };