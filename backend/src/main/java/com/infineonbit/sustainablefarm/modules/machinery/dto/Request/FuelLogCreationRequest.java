package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelLogCreationRequest(
        @NotNull(message = "Equipment is required")
        Long equipmentId,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Liters is required")
        @DecimalMin(value = "0.0", message = "Liters must be 0 or more")
        BigDecimal liters,

        @NotNull(message = "Cost is required")
        @DecimalMin(value = "0.0", message = "Cost must be 0 or more")
        BigDecimal cost
) {}
