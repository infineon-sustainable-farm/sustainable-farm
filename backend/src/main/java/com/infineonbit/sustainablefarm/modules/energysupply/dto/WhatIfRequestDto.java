package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfRequestDto {

    private Integer additionalPanels;
    private BigDecimal additionalBatteryKwh;
    private BigDecimal productionScaleKgPerDay;
}
