package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of an event of a nursery batch.
 *
 * <p>{@code batchCode} and {@code farmId} are those of the batch. The other
 * components depend on the type, and are {@code null} otherwise:
 * {@code stage} for a stage change; {@code quantity} and {@code reason} for a
 * loss; {@code quantity}, {@code blockCode} and {@code populationEventId} for
 * a transplant, the last one being the PLANTING it created in the orchard.
 */
public record NurseryEventResponse(
        Long id,
        Long batchId,
        String batchCode,
        Integer farmId,
        NurseryEventType eventType,
        LocalDate eventDate,
        NurseryStage stage,
        Integer quantity,
        String reason,
        String blockCode,
        Long populationEventId,
        String source,
        Instant lastUpdated) {
}
