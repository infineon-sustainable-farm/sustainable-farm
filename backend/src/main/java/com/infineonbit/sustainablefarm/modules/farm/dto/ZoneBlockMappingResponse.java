package com.infineonbit.sustainablefarm.modules.farm.dto;

import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import java.time.Instant;

/**
 * Link between a zone and the Plants block it waters, read from the zone's block (Zone -> Block):
 * there is no mapping table any more. A zone has at most one link, so the link id is the zone id.
 */
public record ZoneBlockMappingResponse(
        Long id,
        Long zoneId,
        String blockCode,
        Long farmId,
        Instant createdAt) {
    public static ZoneBlockMappingResponse from(ZoneDto zone) {
        return new ZoneBlockMappingResponse(zone.id(), zone.id(), zone.blockCode(), zone.farmId(), zone.createdAt());
    }
}
