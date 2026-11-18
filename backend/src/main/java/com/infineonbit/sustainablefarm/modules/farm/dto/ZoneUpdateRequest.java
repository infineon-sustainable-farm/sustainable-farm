package com.infineonbit.sustainablefarm.modules.farm.dto;


public record ZoneUpdateRequest(
        Long fieldId,
        String name,
        Double areaHectares,
        String irrigationMethod,
        Double cropCoefficient,
        Integer emitterCount,
        Double emitterNominalFlowLh) {
}