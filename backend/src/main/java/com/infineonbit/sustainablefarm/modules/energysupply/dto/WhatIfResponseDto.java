package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfResponseDto {

    private BigDecimal additionalCapexEur;
    private BigDecimal newPaybackYears;
    private BigDecimal newCo2ReductionPct;
    private BigDecimal newSystemCapacityKwp;
}
