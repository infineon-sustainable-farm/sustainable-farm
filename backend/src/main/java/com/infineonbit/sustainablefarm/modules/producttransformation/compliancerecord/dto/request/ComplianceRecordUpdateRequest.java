package com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceRecord.ComplianceResult;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceRecord.ComplianceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating an existing ComplianceRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecordUpdateRequest {

    private ComplianceType complianceType;

    private String requirement;

    private ComplianceResult result;

    private String evidence;

    private String auditorId;

    private LocalDate auditDate;

    private LocalDate nextAuditDate;
}
