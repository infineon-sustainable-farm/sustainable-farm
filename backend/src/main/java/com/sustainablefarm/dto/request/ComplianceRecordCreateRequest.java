package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.model.ComplianceRecord.ComplianceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new ComplianceRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecordCreateRequest {

    @NotBlank(message = "Record ID is required")
    private String recordId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Compliance type is required")
    private ComplianceType complianceType;

    @NotBlank(message = "Requirement is required")
    private String requirement;

    @NotNull(message = "Result is required")
    private ComplianceResult result;

    private String evidence;

    private String auditorId;

    @NotNull(message = "Audit date is required")
    private LocalDate auditDate;

    private LocalDate nextAuditDate;
}
