package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GeneratorEventRequest(
        @NotNull @PastOrPresent(message = "must be today or in the past") LocalDate date,
        @PositiveOrZero BigDecimal durationHours,
        @PositiveOrZero BigDecimal fuelLiters,
        @PositiveOrZero BigDecimal costEur,
        @Size(max = 500) String triggerReason) {}