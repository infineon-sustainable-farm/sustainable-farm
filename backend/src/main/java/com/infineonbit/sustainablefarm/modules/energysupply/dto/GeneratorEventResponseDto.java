package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GeneratorEventResponseDto(
        String eventId,
        LocalDate date,
        String triggerReason,
        BigDecimal durationHours,
        BigDecimal fuelLiters,
        BigDecimal costEur) {
}
