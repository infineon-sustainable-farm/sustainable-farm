package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a treatment, one line of the treatment record.
 *
 * <p>{@code findingId} is {@code null} for a preventive treatment.
 * {@code targetOtherLabel} names the targeted problem when its code is
 * {@code OTHER}. {@code harvestAllowedFrom} is {@code treatedOn} plus
 * {@code preHarvestIntervalDays}, computed on every read; no harvest is refused
 * because of it.
 */
public record HealthTreatmentResponse(
        Long id,
        Integer farmId,
        String blockCode,
        String targetIssueCode,
        String targetIssueName,
        String targetOtherLabel,
        Long findingId,
        LocalDate treatedOn,
        String productName,
        String activeIngredient,
        Double quantity,
        TreatmentUnit unit,
        Integer preHarvestIntervalDays,
        LocalDate harvestAllowedFrom,
        String applicator,
        String equipment,
        String source,
        Instant lastUpdated) {
}
