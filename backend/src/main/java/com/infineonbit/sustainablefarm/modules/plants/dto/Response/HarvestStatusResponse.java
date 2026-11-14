package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * The maturity status and the estimated harvest date of the planted
 * varieties on one day, computed on every read from the recorded data: the
 * estimate, and the field observation that takes precedence over it. No
 * status is stored, and no status blocks anything.
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
     * agronomic reference there is no window: the statuses, the dates and the
     * sources are {@code null}, and an observation is shown without counting.
     *
     * <p>An observation counts for the season under way on the day it was made,
     * or the next one when none was, until the last day of that season; the
     * latest one that counts, by date then identifier, takes precedence over
     * the estimate.
     *
     * @param farmId               farm of the variety row, possibly {@code null}
     * @param blockCode            block of the variety row
     * @param varietyId            identifier of the variety row
     * @param varietyName          name of the variety row, as planted
     * @param treeCount            current number of trees of the variety row
     * @param maturityStatus       the status that holds: the one of the observation that counts, else the
     *                             estimate; READY_FOR_HARVEST is held back to RIPE while a pre-harvest
     *                             interval runs on the block, whatever its source
     * @param statusSource         {@code "field_observation"} when an observation counts, {@code "estimate"}
     *                             otherwise, {@code null} when there is no status
     * @param estimatedStatus      READY_FOR_HARVEST when the window is open and no pre-harvest interval
     *                             runs on the block; RIPE when the window opens within {@code withinDays}
     *                             days, or is open while a pre-harvest interval runs; IMMATURE otherwise
     * @param observation          the observation that counts; without an agronomic reference, the latest
     *                             one, which does not count; {@code null} when there is none
     * @param estimatedHarvestDate first day of the window, {@code null} when the trees never give a yield;
     *                             an observation does not move it
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
            String statusSource,
            MaturityStatus estimatedStatus,
            FieldObservation observation,
            LocalDate estimatedHarvestDate,
            LocalDate seasonEnd,
            Integer daysFromToday,
            LocalDate harvestAllowedFrom,
            Long treatmentId,
            LocalDate firstHarvestDate,
            String seasonSource,
            String phaseShareSource) {
    }

    /**
     * A maturity observation of the variety row, as recorded.
     *
     * @param id             identifier of the observation
     * @param observedOn     the day of the observation
     * @param maturityStatus the status the observer concluded
     * @param method         how the observation was made
     * @param measuredValue  the value read with the method, {@code null} when none was read
     * @param measuredUnit   unit of the value: "°Bx" for BRIX, "%" for DRY_MATTER, {@code null} without value
     * @param observer       who made the observation, possibly {@code null}
     * @param source         where the observation comes from, for example {@code "user_entry"}
     * @param countsUntil    last day the observation counts: the last day of its season; {@code null}
     *                       when it does not count, as without an agronomic reference
     */
    public record FieldObservation(
            Long id,
            LocalDate observedOn,
            MaturityStatus maturityStatus,
            MaturityMethod method,
            Double measuredValue,
            String measuredUnit,
            String observer,
            String source,
            LocalDate countsUntil) {
    }
}
