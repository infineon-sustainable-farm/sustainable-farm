package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * API representation of a variety.
 *
 * <p>The component names are English camelCase, like every other module of the
 * project (machinery, visitor management). They are the JSON contract other
 * modules consume.
 *
 * <p>The physical table keeps its snake_case French column names
 * ({@code nombre_arbres}, {@code bloc_parcelle}, ...): the mapping lives in the
 * explicit {@code @Column} annotations of
 * {@link com.infineonbit.sustainablefarm.modules.plants.entity.Variety}, so the
 * schema is untouched by this naming and the two can differ without either
 * side moving.
 *
 * <p>A component is {@code null} whenever the underlying data does not exist.
 * Consumers render that as an explicit "no value", never as zero.
 */
public record VarietyResponse(
        Long id,
        Integer farmId,
        String name,
        Integer treeCount,
        Double rowSpacingM,
        Double treeSpacingM,
        Double treeDensityPerHa,
        Double expectedYieldKg,
        Double actualYieldKg,
        String vigor,
        String blockCode,
        String plantOrigin,
        String source,
        Instant lastUpdated) {
}
