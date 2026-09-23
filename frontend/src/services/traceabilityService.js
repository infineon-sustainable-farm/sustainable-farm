import { apiClient } from './api';

export const traceabilityApi = {
  getBatchTraceability: (batchId) => {
    return apiClient.get(`/api/traceability/batch/${batchId}`);
  }
};