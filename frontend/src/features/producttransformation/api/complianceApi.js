/**
 * Product Transformation - Compliance Record API
 * API client for HACCP compliance record operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { apiClient } from '../../../shared/api/client';

/**
 * Compliance record API endpoints
 */
export const complianceApi = {
  /**
   * Get all compliance records
   */
  getAllComplianceRecords: () => apiClient.get('/api/compliance-records'),

  /**
   * Get compliance record by ID
   */
  getComplianceRecordById: (recordId) => apiClient.get(`/api/compliance-records/${recordId}`),

  /**
   * Create a new compliance record
   */
  createComplianceRecord: (recordData) => apiClient.post('/api/compliance-records', recordData),

  /**
   * Update a compliance record
   */
  updateComplianceRecord: (recordId, recordData) =>
    apiClient.put(`/api/compliance-records/${recordId}`, recordData),

  /**
   * Delete a compliance record
   */
  deleteComplianceRecord: (recordId) => apiClient.delete(`/api/compliance-records/${recordId}`),

  /**
   * Get compliance records by batch
   */
  getComplianceRecordsByBatch: (batchId) => apiClient.get(`/api/compliance-records/batch/${batchId}`),

  /**
   * Get compliance records by type
   */
  getComplianceRecordsByType: (complianceType) =>
    apiClient.get(`/api/compliance-records/type/${complianceType}`),

  /**
   * Get compliance records by result
   */
  getComplianceRecordsByResult: (result) => apiClient.get(`/api/compliance-records/result/${result}`),

  /**
   * Get compliance records by auditor
   */
  getComplianceRecordsByAuditor: (auditorId) =>
    apiClient.get(`/api/compliance-records/auditor/${auditorId}`),

  /**
   * Get overdue compliance records
   */
  getOverdueComplianceRecords: () => apiClient.get('/api/compliance-records/overdue'),

  /**
   * Get compliance records by audit date range
   */
  getComplianceRecordsByDateRange: (startDate, endDate) =>
    apiClient.get(`/api/compliance-records/date-range?startDate=${startDate}&endDate=${endDate}`),
};

export default complianceApi;
