package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.util.UUID;

public record ZoneBlockMappingUpdateRequest(
        UUID zoneId,
        String blockCode,
        UUID farmId) {
}
