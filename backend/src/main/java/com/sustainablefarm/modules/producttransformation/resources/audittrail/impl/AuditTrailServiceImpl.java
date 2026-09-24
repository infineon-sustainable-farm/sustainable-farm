package com.sustainablefarm.modules.producttransformation.resources.audittrail.impl;

import com.sustainablefarm.core.exception.ResourceNotFoundException;
import com.sustainablefarm.modules.producttransformation.resources.audittrail.dto.response.AuditTrailResponse;
import com.sustainablefarm.modules.producttransformation.resources.audittrail.model.AuditTrail;
import com.sustainablefarm.modules.producttransformation.resources.audittrail.repository.AuditTrailRepository;
import com.sustainablefarm.modules.producttransformation.resources.audittrail.service.AuditTrailService;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Audit Trail Service
 * Tracks batch status transitions with operator attribution
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Slf4j
public class AuditTrailServiceImpl implements AuditTrailService {

    @Autowired
    private AuditTrailRepository auditTrailRepository;
    
    @Autowired
    private BatchRepository batchRepository;
    
    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    public void logStatusChange(String batchId, String previousStatus, String newStatus, String operatorId, String reason) {
        log.info("Logging status change for batch {}: {} -> {} by operator {}", batchId, previousStatus, newStatus, operatorId);
        
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with ID: " + batchId));
        
        Operator operator = null;
        if (operatorId != null && !operatorId.isEmpty()) {
            operator = operatorRepository.findById(operatorId).orElse(null);
        }
        
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setBatch(batch);
        auditTrail.setPreviousStatus(previousStatus);
        auditTrail.setNewStatus(newStatus);
        auditTrail.setOperator(operator);
        auditTrail.setChangeReason(reason);
        
        auditTrailRepository.save(auditTrail);
        log.info("Audit trail entry created for batch status change");
    }

    @Override
    public List<AuditTrailResponse> getBatchAuditTrail(String batchId) {
        log.info("Getting audit trail for batch: {}", batchId);
        
        List<AuditTrail> auditTrails = auditTrailRepository.findByBatchBatchIdOrderByChangeTimestampDesc(batchId);
        
        return auditTrails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditTrailResponse> getOperatorAuditTrail(String operatorId) {
        log.info("Getting audit trail for operator: {}", operatorId);
        
        List<AuditTrail> auditTrails = auditTrailRepository.findByOperatorOperatorIdOrderByChangeTimestampDesc(operatorId);
        
        return auditTrails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private AuditTrailResponse mapToResponse(AuditTrail auditTrail) {
        return AuditTrailResponse.builder()
                .auditId(auditTrail.getAuditId())
                .batchId(auditTrail.getBatch() != null ? auditTrail.getBatch().getBatchId() : null)
                .previousStatus(auditTrail.getPreviousStatus())
                .newStatus(auditTrail.getNewStatus())
                .operatorId(auditTrail.getOperator() != null ? auditTrail.getOperator().getOperatorId() : null)
                .operatorName(auditTrail.getOperator() != null ? auditTrail.getOperator().getOperatorName() : null)
                .changeReason(auditTrail.getChangeReason())
                .changeTimestamp(auditTrail.getChangeTimestamp())
                .ipAddress(auditTrail.getIpAddress())
                .userAgent(auditTrail.getUserAgent())
                .build();
    }
}