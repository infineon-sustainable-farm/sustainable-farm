import { apiClient } from './api';

export const auditTrailApi = {
  getBatchAuditTrail: (batchId) => {
    return apiClient.get(`/api/audit-trail/batch/${batchId}`);
  },
  getOperatorAuditTrail: (operatorId) => {
    return apiClient.get(`/api/audit-trail/operator/${operatorId}`);
  }
};