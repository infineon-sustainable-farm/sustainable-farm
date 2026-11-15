package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import java.time.Instant;
import java.util.UUID;

public record FieldResponse(UUID id, UUID farmId, String name, Double areaHectares,
                            String cropType, String soilType, String coordinates, Instant createdAt) {
    public static FieldResponse from(Field field) {
        return new FieldResponse(field.getId(), field.getFarmId(), field.getName(), field.getAreaHectares(),
                field.getCropType(), field.getSoilType(), field.getCoordinates(), field.getCreatedAt());
    }
}