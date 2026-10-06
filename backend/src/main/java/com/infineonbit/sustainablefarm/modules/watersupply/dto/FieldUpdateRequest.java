package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.util.UUID;

public record FieldUpdateRequest(
        UUID farmId,
        String name,
        Double areaHectares,
        String cropType,
        String soilType,
        String coordinates) {
}