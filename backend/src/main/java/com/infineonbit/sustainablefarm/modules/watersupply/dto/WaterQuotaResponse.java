package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import java.time.LocalDate;
import java.util.UUID;

/**
 * View of a monthly quota as exposed by the API.
 */
public record WaterQuotaResponse(UUID id, String targetType, UUID targetId, LocalDate quotaMonth,
                                 Double quotaLiters, String label) {
    public static WaterQuotaResponse from(WaterQuota quota) {
        return new WaterQuotaResponse(quota.getId(), quota.getTargetType(), quota.getTargetId(),
                quota.getQuotaMonth(), quota.getQuotaLiters(), quota.getLabel());
    }
}
