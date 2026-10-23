package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creation of a monthly water quota for a farm or a zone.
 * The month is conventionally represented by the first day of the month (YYYY-MM-01).
 */
public record WaterQuotaCreateRequest(
        @NotBlank String targetType,
        @NotNull UUID targetId,
        @NotNull LocalDate quotaMonth,
        @NotNull @Positive Double quotaLiters,
        String label) {
}
