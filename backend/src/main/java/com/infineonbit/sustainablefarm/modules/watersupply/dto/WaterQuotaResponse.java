package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.StoredValue;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import java.time.LocalDate;

/**
 * View of a monthly quota as exposed by the API.
 */
public record WaterQuotaResponse(Long id, String targetType, Long targetId, LocalDate quotaMonth,
                                 Double quotaLiters, String label) {
    public static WaterQuotaResponse from(WaterQuota quota) {
        return new WaterQuotaResponse(quota.getId(), StoredValue.valueOf(quota.getTargetType()), quota.getTargetId(),
                quota.getQuotaMonth(), quota.getQuotaLiters(), quota.getLabel());
    }
}
