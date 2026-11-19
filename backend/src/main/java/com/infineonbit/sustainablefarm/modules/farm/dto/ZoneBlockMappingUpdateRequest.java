package com.infineonbit.sustainablefarm.modules.farm.dto;


public record ZoneBlockMappingUpdateRequest(
        Long zoneId,
        String blockCode,
        Long farmId) {
}
