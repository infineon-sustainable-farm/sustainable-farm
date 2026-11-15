package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergySource;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumptionLogResponse(
        String logId,
        LocalDate date,
        LoadCategory loadCategory,
        BigDecimal kwhConsumed,
        EnergySource source) {

    public static ConsumptionLogResponse from(ConsumptionLog log) {
        return new ConsumptionLogResponse(
                log.getLogId(),
                log.getDate(),
                log.getLoadCategory(),
                log.getKwhConsumed(),
                log.getSource());
    }
}
