package com.sustainablefarm.modules.producttransformation.resources.audittrail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Trail Response DTO
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrailResponse {
    
    private Long auditId;
    private String batchId;
    private String previousStatus;
    private String newStatus;
    private String operatorId;
    private String operatorName;
    private String changeReason;
    private LocalDateTime changeTimestamp;
    private String ipAddress;
    private String userAgent;
}