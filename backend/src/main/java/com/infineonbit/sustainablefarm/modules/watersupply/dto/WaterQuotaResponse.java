package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue d'un quota mensuel telle qu'exposee par l'API.
 */
public record WaterQuotaResponse(UUID id, String targetType, UUID targetId, LocalDate quotaMonth,
                                 Double quotaLiters, String label) {
    public static WaterQuotaResponse from(WaterQuota quota) {
        return new WaterQuotaResponse(quota.getId(), quota.getTargetType(), quota.getTargetId(),
                quota.getQuotaMonth(), quota.getQuotaLiters(), quota.getLabel());
    }
}
