package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairLogCreationRequest(
        @NotNull(message = "Equipment is required")
        Long equipmentId,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotBlank(message = "Issue description is required")
        @Size(max = 500, message = "Issue description must be 500 characters or fewer")
        String issue,

        @DecimalMin(value = "0.0", message = "Downtime must be 0 or more")
        BigDecimal downtime,

        @DecimalMin(value = "0.0", message = "Cost must be 0 or more")
        BigDecimal cost,

        @Size(max = 60, message = "Technician must be 60 characters or fewer")
        String technician
) {}
