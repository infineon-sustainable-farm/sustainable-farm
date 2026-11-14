package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergySource;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumptionLogRequest(
        @NotNull @PastOrPresent LocalDate date,
        @NotNull LoadCategory loadCategory,
        @NotNull @PositiveOrZero BigDecimal kwhConsumed,
        EnergySource source) {
}
