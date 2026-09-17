package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creation d'un quota mensuel d'eau pour une ferme ou une zone.
 * Le mois est conventionnellement represente par le premier jour du mois (YYYY-MM-01).
 */
public record WaterQuotaCreateRequest(
        @NotBlank String targetType,
        @NotNull UUID targetId,
        @NotNull LocalDate quotaMonth,
        @NotNull @Positive Double quotaLiters,
        String label) {
}
