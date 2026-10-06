package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Level of a water source, read in real time for the interface and the other modules
 * (module 5.2 of the specification: "reservoir level in real time").
 *
 * <p>The value comes from the last measurement sent by the level sensor; {@code readAt}
 * is the reading instant, {@code status} summarizes the situation against capacity.</p>
 */
public record WaterLevelResponse(
        UUID sourceId,
        String name,
        String type,
        Double capacityLiters,
        Double currentLevelLiters,
        Double levelPercentage,
        String status,
        boolean rainwaterTank,
        Instant readAt) {
}
