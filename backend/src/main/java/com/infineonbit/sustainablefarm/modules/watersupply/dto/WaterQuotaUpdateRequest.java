package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Mise a jour partielle d'un quota : seuls les champs fournis sont modifies.
 */
public record WaterQuotaUpdateRequest(
        LocalDate quotaMonth,
        @Positive Double quotaLiters,
        String label) {
}
