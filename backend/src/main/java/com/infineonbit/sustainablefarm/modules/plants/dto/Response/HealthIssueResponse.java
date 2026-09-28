package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;

/**
 * API representation of a pest or disease of the catalogue.
 *
 * <p>{@code code} is the value an inspection or a treatment sends, such as
 * {@code "ANTHRACNOSE"}. {@code scientificName} and {@code eppoCode} are
 * {@code null} when the catalogue has none, as for "Termites" or "Other".
 */
public record HealthIssueResponse(
        Long id,
        String code,
        String name,
        HealthIssueKind kind,
        String scientificName,
        String eppoCode,
        String source) {
}
