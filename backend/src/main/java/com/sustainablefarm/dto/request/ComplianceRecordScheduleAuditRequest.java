package com.sustainablefarm.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for scheduling the next compliance audit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecordScheduleAuditRequest {

    @NotNull(message = "Next audit date is required")
    private LocalDate nextAuditDate;
}
