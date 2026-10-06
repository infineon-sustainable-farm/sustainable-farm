package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.YearMonth;
import java.util.List;

/**
 * Expected mango yield, month by month, computed from the recorded plantings.
 *
 * <p>{@code basis} names the data layer the forecast comes from. It is always
 * {@code "recorded_plantings"}: only the variety rows with a PLANTING event
 * count, never a scenario such as the Zalka 2025 figures.
 *
 * @param from                      first month of the forecast, as {@code yyyy-MM}
 * @param months                    number of months of the forecast
 * @param basis                     data layer of the forecast
 * @param monthlyTotals             expected yield of every month of the forecast,
 *                                  zeros included, in order
 * @param entries                   the (month, variety) pairs with a yield above zero
 * @param varietiesWithoutReference planted varieties absent from the agronomic
 *                                  reference, hence not forecast
 */
public record YieldForecastResponse(
        YearMonth from,
        Integer months,
        String basis,
        List<MonthlyTotal> monthlyTotals,
        List<Entry> entries,
        List<VarietyWithoutReference> varietiesWithoutReference) {

    /**
     * Expected yield of one month, all varieties together.
     *
     * @param month      the month, as {@code yyyy-MM}
     * @param expectedKg sum of the expected yields of the month's entries, in kg
     */
    public record MonthlyTotal(YearMonth month, Double expectedKg) {
    }

    /**
     * Expected yield of one variety row in one month, with every factor of the
     * computation and the source of each reference value.
     *
     * @param month            the month, as {@code yyyy-MM}
     * @param farmId           farm of the variety row, possibly {@code null}
     * @param blockCode        block of the variety row
     * @param varietyId        identifier of the variety row
     * @param varietyName      name of the variety row, as planted
     * @param treeCount        current number of trees of the variety row
     * @param ageYears         completed years of the trees on the first day of the month
     * @param growthPhase      growth phase for that age
     * @param yieldPerTreeKg   yearly yield of one tree in full production
     * @param phaseShare       share of that yield in the growth phase
     * @param monthShare       share of the year's harvest picked in the month,
     *                         shown to 4 decimals
     * @param expectedKg       expected yield, rounded to 0.1 kg
     * @param yieldSource      source of the yield per tree
     * @param seasonSource     source of the harvest season
     * @param phaseShareSource source of the phase share
     */
    public record Entry(
            YearMonth month,
            Integer farmId,
            String blockCode,
            Long varietyId,
            String varietyName,
            Integer treeCount,
            Integer ageYears,
            String growthPhase,
            Double yieldPerTreeKg,
            Double phaseShare,
            Double monthShare,
            Double expectedKg,
            String yieldSource,
            String seasonSource,
            String phaseShareSource) {
    }

    /**
     * A planted variety row whose name is not in the agronomic reference. It is
     * listed rather than guessed or silently left out.
     *
     * @param farmId      farm of the variety row, possibly {@code null}
     * @param blockCode   block of the variety row
     * @param varietyName name of the variety row, as planted
     * @param treeCount   current number of trees of the variety row
     */
    public record VarietyWithoutReference(Integer farmId, String blockCode, String varietyName, Integer treeCount) {
    }
}
