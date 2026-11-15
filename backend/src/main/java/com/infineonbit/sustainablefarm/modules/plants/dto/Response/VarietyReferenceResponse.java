package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * API representation of the agronomic reference of a variety.
 *
 * <p>The yield and the harvest season each carry their own source, as they
 * often come from different studies. {@code lastUpdated} is when the
 * reference was entered or last corrected.
 *
 * @param id                identifier of the reference
 * @param varietyName       the variety name; a planted variety finds it ignoring
 *                          case, accents and surrounding spaces
 * @param yieldPerTreeKg    yearly yield of one tree in full production, in kg
 * @param yieldSource       source of the yield
 * @param harvestStartMonth first month of the harvest season, 1 to 12
 * @param harvestEndMonth   last month of the harvest season, 1 to 12, lower than
 *                          the start month for a season over the new year
 * @param seasonSource      source of the harvest season
 * @param lastUpdated       when the reference was entered or last corrected
 */
public record VarietyReferenceResponse(
        Long id,
        String varietyName,
        Double yieldPerTreeKg,
        String yieldSource,
        Integer harvestStartMonth,
        Integer harvestEndMonth,
        String seasonSource,
        Instant lastUpdated) {
}
