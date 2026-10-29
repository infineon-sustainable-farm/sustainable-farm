package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Partial update of a quota: only the provided fields are modified.
 */
public record WaterQuotaUpdateRequest(
        LocalDate quotaMonth,
        @Positive Double quotaLiters,
        String label) {
}
