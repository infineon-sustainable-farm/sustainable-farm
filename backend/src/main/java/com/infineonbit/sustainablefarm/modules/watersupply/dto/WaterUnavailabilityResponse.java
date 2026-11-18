package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.StoredValue;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import java.time.Instant;

public record WaterUnavailabilityResponse(Long id, String scope, Long scopeId, Instant startAt,
                                          Instant endAt, String reason, String status,
                                          Instant createdAt) {
    public static WaterUnavailabilityResponse from(WaterUnavailability outage) {
        return new WaterUnavailabilityResponse(outage.getId(), StoredValue.valueOf(outage.getScope()), outage.getScopeId(),
                outage.getStartAt(), outage.getEndAt(), outage.getReason(), StoredValue.valueOf(outage.getStatus()),
                outage.getCreatedAt());
    }
}
