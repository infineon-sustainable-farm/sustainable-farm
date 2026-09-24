package com.sustainablefarm.modules.producttransformation.resources.audittrail.controller;

import com.sustainablefarm.modules.producttransformation.resources.audittrail.dto.response.AuditTrailResponse;
import com.sustainablefarm.modules.producttransformation.resources.audittrail.service.AuditTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Audit Trail operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/audit-trail")
@Tag(name = "Audit Trail", description = "APIs for batch status change audit trail")
public class AuditTrailController {

    private final AuditTrailService auditTrailService;

    @Autowired
    public AuditTrailController(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch audit trail", description = "Retrieves audit trail entries for a specific batch")
    public ResponseEntity<List<AuditTrailResponse>> getBatchAuditTrail(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<AuditTrailResponse> auditTrail = auditTrailService.getBatchAuditTrail(batchId);
        return ResponseEntity.ok(auditTrail);
    }

    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "Get operator audit trail", description = "Retrieves audit trail entries for a specific operator")
    public ResponseEntity<List<AuditTrailResponse>> getOperatorAuditTrail(
            @Parameter(description = "Operator ID") @PathVariable String operatorId) {
        List<AuditTrailResponse> auditTrail = auditTrailService.getOperatorAuditTrail(operatorId);
        return ResponseEntity.ok(auditTrail);
    }
}