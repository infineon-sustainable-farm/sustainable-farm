package com.sustainablefarm.modules.producttransformation.resources.compliancerecord.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for completing a compliance audit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecordCompleteAuditRequest {

    @NotNull(message = "Compliance result is required")
    private ComplianceResult result;

    private String evidence;
}
