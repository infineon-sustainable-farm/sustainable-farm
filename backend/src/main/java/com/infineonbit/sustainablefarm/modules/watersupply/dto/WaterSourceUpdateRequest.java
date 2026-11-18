package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.util.UUID;

public record WaterSourceUpdateRequest(
        Long farmId,
        String name,
        String type,
        Double capacityLiters,
        Double currentLevelLiters,
        Double latitude,
        Double longitude) {
}