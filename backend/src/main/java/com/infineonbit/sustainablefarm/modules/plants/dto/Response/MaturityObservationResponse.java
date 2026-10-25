package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a maturity observation recorded in the field.
 *
 * <p>{@code farmId}, {@code blockCode}, {@code varietyId} and
 * {@code varietyName} describe the variety row the observation belongs to, as
 * stored: an observation of "keitt" comes back as "Keitt" when that is the
 * planted name.
 *
 * <p>{@code measuredUnit} is the unit of the method, given with the value:
 * "°Bx" for {@code BRIX}, "%" for {@code DRY_MATTER}, {@code null} when no
 * value was read.
 */
public record MaturityObservationResponse(
        Long id,
        Integer farmId,
        String blockCode,
        Long varietyId,
        String varietyName,
        LocalDate observedOn,
        MaturityStatus maturityStatus,
        MaturityMethod method,
        Double measuredValue,
        String measuredUnit,
        String observer,
        String source,
        Instant lastUpdated) {
}
