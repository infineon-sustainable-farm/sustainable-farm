package com.sustainablefarm.modules.producttransformation.resources.audittrail.service;

import com.sustainablefarm.modules.producttransformation.resources.audittrail.dto.response.AuditTrailResponse;

import java.util.List;

/**
 * Service interface for Audit Trail operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface AuditTrailService {
    
    /**
     * Log a batch status change
     * 
     * @param batchId The batch ID
     * @param previousStatus Previous status
     * @param newStatus New status
     * @param operatorId Operator who made the change
     * @param reason Reason for the change
     */
    void logStatusChange(String batchId, String previousStatus, String newStatus, String operatorId, String reason);
    
    /**
     * Get audit trail for a batch
     * 
     * @param batchId The batch ID
     * @return List of audit trail entries
     */
    List<AuditTrailResponse> getBatchAuditTrail(String batchId);
    
    /**
     * Get audit trail for an operator
     * 
     * @param operatorId The operator ID
     * @return List of audit trail entries
     */
    List<AuditTrailResponse> getOperatorAuditTrail(String operatorId);
}