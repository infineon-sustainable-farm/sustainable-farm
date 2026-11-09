package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;

import java.math.BigDecimal;

public record EnergyComponentResponse(
        String componentId,
        String name,
        ComponentCategory category,
        BigDecimal capacityValue,
        ComponentStatus status) {

    public static EnergyComponentResponse from(EnergyComponent component) {
        return new EnergyComponentResponse(
                component.getComponentId(),
                component.getName(),
                component.getCategory(),
                component.getCapacityValue(),
                component.getStatus());
    }
}
