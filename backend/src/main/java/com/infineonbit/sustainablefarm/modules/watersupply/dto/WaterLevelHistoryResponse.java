package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import java.time.Instant;

public record WaterLevelHistoryResponse(Long id, Long sourceId, Double levelLiters, Instant measuredAt) {
    public static WaterLevelHistoryResponse from(WaterLevelHistory history) {
        return new WaterLevelHistoryResponse(history.getId(), history.getSourceId(), history.getLevelLiters(),
                history.getMeasuredAt());
    }
}
