package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.util.UUID;

public record ZoneUpdateRequest(
        UUID fieldId,
        String name,
        Double areaHectares,
        String irrigationMethod) {
}