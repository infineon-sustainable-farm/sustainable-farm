package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import java.time.Instant;
import java.util.UUID;

public record WaterLevelHistoryResponse(UUID id, UUID sourceId, Double levelLiters, Instant measuredAt) {
    public static WaterLevelHistoryResponse from(WaterLevelHistory history) {
        return new WaterLevelHistoryResponse(history.getId(), history.getSourceId(), history.getLevelLiters(),
                history.getMeasuredAt());
    }
}
