package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageLogCreationRequest(
        @NotNull(message = "Equipment is required")
        Long equipmentId,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Hours used is required")
        @DecimalMin(value = "0.0", message = "Hours used must be 0 or more")
        BigDecimal hoursUsed,

        @Size(max = 60, message = "Operator must be 60 characters or fewer")
        String operator,

        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes
) {}
