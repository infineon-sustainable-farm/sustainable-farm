import apiClient from './api';

const qcApi = {
  getAll: () => apiClient.get('/api/qc-checkpoints'),
  getById: (id) => apiClient.get(`/api/qc-checkpoints/${id}`),
  create: (data) => apiClient.post('/api/qc-checkpoints', data),
  update: (id, data) => apiClient.put(`/api/qc-checkpoints/${id}`, data),
  delete: (id) => apiClient.delete(`/api/qc-checkpoints/${id}`),
  getByBatch: (batchId) => apiClient.get(`/api/qc-checkpoints/batch/${batchId}`),
  getByStage: (stage) => apiClient.get(`/api/qc-checkpoints/stage/${stage}`),
  getByInspector: (inspectorId) => apiClient.get(`/api/qc-checkpoints/inspector/${inspectorId}`)
};

export { qcApi };
