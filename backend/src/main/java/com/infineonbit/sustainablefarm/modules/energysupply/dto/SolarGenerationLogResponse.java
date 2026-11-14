package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolarGenerationLogResponse(
        String logId,
        String componentId,
        String componentName,
        LocalDate date,
        BigDecimal kwhProduced,
        BigDecimal efficiencyPct) {

    public static SolarGenerationLogResponse from(SolarGenerationLog log) {
        return new SolarGenerationLogResponse(
                log.getLogId(),
                log.getComponent().getComponentId(),
                log.getComponent().getName(),
                log.getDate(),
                log.getKwhProduced(),
                log.getEfficiencyPct());
    }
}
