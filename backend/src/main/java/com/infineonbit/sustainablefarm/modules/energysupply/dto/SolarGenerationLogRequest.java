package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolarGenerationLogRequest(
        @NotBlank String componentId,
        @NotNull @PastOrPresent LocalDate date,
        @NotNull @PositiveOrZero BigDecimal kwhProduced,
        @PositiveOrZero @DecimalMax("100") BigDecimal efficiencyPct) {
}
