package com.infineonbit.sustainablefarm.modules.machinery.dto.Request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelLogUpdateRequest(
        Long equipmentId,

        LocalDate date,

        @DecimalMin(value = "0.0", message = "Liters must be 0 or more")
        BigDecimal liters,

        @DecimalMin(value = "0.0", message = "Cost must be 0 or more")
        BigDecimal cost
) {}
