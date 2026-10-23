package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.YearMonth;
import java.util.List;

/**
 * Actual mango yield against the expected yield, over whole months, computed
 * on every read and never stored.
 *
 * <p>The expected yield of a month is that of the yield forecast for the same
 * month: trees × yield per tree × share of the growth phase × share of the
 * month in the harvest season, rounded to 0.1 kg. The actual yield is the sum
 * of the harvests dated in the months, rounded to 0.1 kg. {@code basis} names
 * the data layer of the expected yield, always {@code "recorded_plantings"}.
 *
 * <p>A yield is {@code null} when there is no data: no harvest recorded, or no
 * agronomic reference to compute the expected yield from. An expected yield of
 * 0 is a value: trees too young to give fruit.
 *
 * @param from        first month, as {@code yyyy-MM}
 * @param months      number of months
 * @param basis       data layer of the expected yield
 * @param varieties   one line per planted variety row with an agronomic reference, planted by the end
 *                    of the months
 * @param blocks      the lines of {@code varieties} added up by block
 * @param total       the lines of {@code varieties} added up
 * @param notCompared planted variety rows with no expected yield: absent from the reference, or with no
 *                    tree left; their harvests are listed here rather than left out
 */
public record YieldComparisonResponse(
        YearMonth from,
        Integer months,
        String basis,
        List<VarietyComparison> varieties,
        List<BlockComparison> blocks,
        Comparison total,
        List<NotCompared> notCompared) {

    /**
     * Expected and actual yield of one planted variety row.
     *
     * @param farmId         farm of the variety row, possibly {@code null}
     * @param blockCode      block of the variety row
     * @param varietyId      identifier of the variety row
     * @param varietyName    name of the variety row, as planted
     * @param treeCount      current number of trees of the variety row
     * @param expectedKg     sum of the expected yields of the months, possibly 0
     * @param actualKg       sum of the harvests dated in the months, {@code null} when there is none
     * @param harvestCount   number of those harvests
     * @param achievedPct    actual yield × 100 / expected yield, to 0.1, {@code null} without
     *                       harvest or when nothing is expected
     * @param yieldPerTreeKg yearly yield of one tree in full production, from the reference
     * @param yieldSource    source of the yield per tree
     * @param seasonSource   source of the harvest season
     * @param phases         growth phases of the trees in the harvest months, in month order, each
     *                       with its share and the source of that share
     */
    public record VarietyComparison(
            Integer farmId,
            String blockCode,
            Long varietyId,
            String varietyName,
            Integer treeCount,
            Double expectedKg,
            Double actualKg,
            Integer harvestCount,
            Double achievedPct,
            Double yieldPerTreeKg,
            String yieldSource,
            String seasonSource,
            List<PhaseShare> phases) {
    }

    /**
     * A growth phase of the trees in the harvest months, with the share of the
     * full yield it gives.
     *
     * @param growthPhase the growth phase
     * @param yieldShare  share of the full yield in that phase
     * @param source      source of that share
     */
    public record PhaseShare(String growthPhase, Double yieldShare, String source) {
    }

    /**
     * Expected and actual yield of one block, all its varieties together.
     *
     * @param farmId      farm of the block, possibly {@code null}
     * @param blockCode   the block
     * @param expectedKg  sum of the expected yields of its lines
     * @param actualKg    sum of the actual yields of its lines, {@code null} when none has a harvest
     * @param achievedPct actual yield × 100 / expected yield, to 0.1, {@code null} as for a line
     */
    public record BlockComparison(
            Integer farmId,
            String blockCode,
            Double expectedKg,
            Double actualKg,
            Double achievedPct) {
    }

    /**
     * Expected and actual yield of every line together.
     *
     * @param expectedKg  sum of the expected yields
     * @param actualKg    sum of the actual yields, {@code null} when no line has a harvest
     * @param achievedPct actual yield × 100 / expected yield, to 0.1, {@code null} as for a line
     */
    public record Comparison(Double expectedKg, Double actualKg, Double achievedPct) {
    }

    /**
     * A planted variety row with no expected yield.
     *
     * @param farmId       farm of the variety row, possibly {@code null}
     * @param blockCode    block of the variety row
     * @param varietyId    identifier of the variety row
     * @param varietyName  name of the variety row, as planted
     * @param treeCount    current number of trees, {@code null} when none is left
     * @param actualKg     sum of the harvests dated in the months, {@code null} when there is none
     * @param harvestCount number of those harvests
     * @param reason       {@code NO_REFERENCE} when the name is not in the agronomic reference,
     *                     {@code NO_TREE_LEFT} when the row has no tree left
     */
    public record NotCompared(
            Integer farmId,
            String blockCode,
            Long varietyId,
            String varietyName,
            Integer treeCount,
            Double actualKg,
            Integer harvestCount,
            String reason) {
    }
}
