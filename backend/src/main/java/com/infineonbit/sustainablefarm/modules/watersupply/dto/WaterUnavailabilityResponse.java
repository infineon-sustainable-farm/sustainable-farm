package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import java.time.Instant;
import java.util.UUID;

public record WaterUnavailabilityResponse(UUID id, String scope, UUID scopeId, Instant startAt,
                                          Instant endAt, String reason, String status,
                                          Instant createdAt) {
    public static WaterUnavailabilityResponse from(WaterUnavailability outage) {
        return new WaterUnavailabilityResponse(outage.getId(), outage.getScope(), outage.getScopeId(),
                outage.getStartAt(), outage.getEndAt(), outage.getReason(), outage.getStatus(),
                outage.getCreatedAt());
    }
}
