package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EnergyComponentRequest(
        @NotBlank @Pattern(regexp = "ENG-\\d{3}") String componentId,
        @NotBlank @Size(max = 255) String name,
        @NotNull ComponentCategory category,
        @PositiveOrZero BigDecimal capacityValue,
        @NotNull ComponentStatus status) {
}
