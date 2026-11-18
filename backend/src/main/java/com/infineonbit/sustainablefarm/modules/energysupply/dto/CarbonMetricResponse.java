package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarbonMetricResponse(
        Long metricId,
        LocalDate period,
        BigDecimal co2AvoidedKg,
        BigDecimal emissionFactorUsed,
        String scenario) {

    public static CarbonMetricResponse from(CarbonMetric metric) {
        return new CarbonMetricResponse(
                metric.getId(),
                metric.getPeriod(),
                metric.getCo2AvoidedKg(),
                metric.getEmissionFactorUsed(),
                metric.getScenario());
    }
}
