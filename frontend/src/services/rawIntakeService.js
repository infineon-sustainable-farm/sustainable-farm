import apiClient from './api';

const rawIntakeApi = {
  getAll: () => apiClient.get('/api/raw-intakes'),
  getById: (id) => apiClient.get(`/api/raw-intakes/${id}`),
  create: (data) => apiClient.post('/api/raw-intakes', data),
  update: (id, data) => apiClient.put(`/api/raw-intakes/${id}`, data),
  delete: (id) => apiClient.delete(`/api/raw-intakes/${id}`),
  getByBatchId: (batchId) => apiClient.get(`/api/raw-intakes/batch/${batchId}`),
  getByDateRange: (startDate, endDate) => apiClient.get('/api/raw-intakes/date-range', { params: { startDate, endDate } }),
  getBySourceFarm: (sourceFarm) => apiClient.get(`/api/raw-intakes/farm/${sourceFarm}`),
  getByVariety: (variety) => apiClient.get(`/api/raw-intakes/variety/${variety}`),
  getByGrade: (grade) => apiClient.get(`/api/raw-intakes/grade/${grade}`),
  initializeBatchFromIntake: (intakeId) => apiClient.post(`/api/raw-intakes/${intakeId}/initialize-batch`)
};

export { rawIntakeApi };