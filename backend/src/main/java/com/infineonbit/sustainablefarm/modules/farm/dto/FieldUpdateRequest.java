package com.infineonbit.sustainablefarm.modules.farm.dto;


public record FieldUpdateRequest(
        Long farmId,
        String name,
        Double areaHectares,
        String cropType,
        String soilType,
        String coordinates) {
}