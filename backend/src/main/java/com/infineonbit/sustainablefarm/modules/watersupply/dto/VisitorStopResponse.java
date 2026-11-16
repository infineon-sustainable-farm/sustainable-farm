package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorStop;
import java.time.Instant;
import java.util.UUID;

public record VisitorStopResponse(UUID id, String stopName, UUID zoneId, UUID sourceId, Instant createdAt) {
    public static VisitorStopResponse from(WaterVisitorStop stop) {
        return new VisitorStopResponse(stop.getId(), stop.getStopName(), stop.getZoneId(), stop.getSourceId(),
                stop.getCreatedAt());
    }
}
