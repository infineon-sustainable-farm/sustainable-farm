package com.sustainablefarm.modules.producttransformation.resources.compliancerecord.dto.response;

import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for ComplianceRecord response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecordResponse {

    private String recordId;
    private String batchId;
    private ComplianceType complianceType;
    private String requirement;
    private ComplianceResult result;
    private String evidence;
    private String auditorId;
    private LocalDate auditDate;
    private LocalDate nextAuditDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Computed fields
    private Boolean compliant;
    private Boolean auditOverdue;
}
