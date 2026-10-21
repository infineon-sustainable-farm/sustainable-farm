package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * The maturity status and the estimated harvest date of the planted
 * varieties on one day, computed on every read from the recorded data.
 * Nothing of it is stored, and no status blocks anything.
 *
 * @param today      the day the statuses are computed for
 * @param withinDays how many days before its harvest window a variety is ripe
 * @param varieties  one line per planted variety row with trees, with or without an agronomic reference
 */
public record HarvestStatusResponse(
        LocalDate today,
        Integer withinDays,
        List<VarietyHarvestStatus> varieties) {

    /**
     * The harvest status of one planted variety row.
     *
     * <p>The harvest window is that of the plant alerts: from the first day of
     * the first month of the season in which the trees give a yield, as in the
     * yield forecast, to the last day of the season; the season under way, or
     * the first one to come in which the trees give a yield. Without an
     * agronomic reference there is no window: the status, the dates and the
     * sources are {@code null}.
     *
     * @param farmId               farm of the variety row, possibly {@code null}
     * @param blockCode            block of the variety row
     * @param varietyId            identifier of the variety row
     * @param varietyName          name of the variety row, as planted
     * @param treeCount            current number of trees of the variety row
     * @param maturityStatus       READY_FOR_HARVEST when the window is open and no pre-harvest interval
     *                             runs on the block; RIPE when the window opens within {@code withinDays}
     *                             days, or is open while a pre-harvest interval runs; IMMATURE otherwise
     * @param estimatedHarvestDate first day of the window, {@code null} when the trees never give a yield
     * @param seasonEnd            last day of the window
     * @param daysFromToday        {@code estimatedHarvestDate} minus today, in days: positive before it,
     *                             0 on it, negative after it
     * @param harvestAllowedFrom   first day the block can be harvested again, when a pre-harvest
     *                             interval still runs on it, else {@code null}
     * @param treatmentId          the treatment whose pre-harvest interval ends last
     * @param firstHarvestDate     date of the first harvest of the variety row in the window,
     *                             {@code null} when none is recorded
     * @param seasonSource         source of the harvest season
     * @param phaseShareSource     source of the yield share that makes the first month of the window
     *                             give a yield
     */
    public record VarietyHarvestStatus(
            Integer farmId,
            String blockCode,
            Long varietyId,
            String varietyName,
            Integer treeCount,
            MaturityStatus maturityStatus,
            LocalDate estimatedHarvestDate,
            LocalDate seasonEnd,
            Integer daysFromToday,
            LocalDate harvestAllowedFrom,
            Long treatmentId,
            LocalDate firstHarvestDate,
            String seasonSource,
            String phaseShareSource) {
    }
}
