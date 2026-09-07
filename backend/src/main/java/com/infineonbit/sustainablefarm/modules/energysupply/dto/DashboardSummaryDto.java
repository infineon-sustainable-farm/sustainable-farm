package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private BigDecimal totalPvCapacityKwp;
    private BigDecimal currentDailyConsumptionKwh;
    private String generatorStatus;
    private BigDecimal co2AvoidedThisMonthKg;
}
