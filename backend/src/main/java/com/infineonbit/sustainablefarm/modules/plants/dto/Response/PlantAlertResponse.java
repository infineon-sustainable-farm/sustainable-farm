package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;

import java.time.LocalDate;
import java.util.List;

/**
 * The plant alerts of one day, computed on every read from the recorded data.
 * Nothing of them is stored, and no alert blocks anything.
 *
 * @param today                     the day the alerts are computed for
 * @param withinDays                how many days ahead an upcoming harvest season is announced
 * @param alerts                    the alerts, CRITICAL first, then by date, those without a date last
 * @param varietiesWithoutReference planted varieties absent from the agronomic reference: no harvest
 *                                  alert can be computed for them, as no forecast can
 */
public record PlantAlertResponse(
        LocalDate today,
        Integer withinDays,
        List<PlantAlert> alerts,
        List<VarietyWithoutReference> varietiesWithoutReference) {

    /**
     * One alert. Every field is always sent, {@code null} when it does not
     * apply to the type of the alert.
     *
     * <ul>
     *     <li>HARVEST_APPROACHING, WARNING: {@code date} is the first day of the
     *         first month of the season in which the trees give a yield, as in
     *         the forecast, and {@code endDate} the last day of the season;
     *         {@code varietyId}; {@code source} is the source of the season.</li>
     *     <li>PRE_HARVEST_INTERVAL, CRITICAL when the block is in its harvest
     *         season, WARNING otherwise: {@code date} is the first day the block
     *         can be harvested; {@code treatmentId}, and {@code findingId} when
     *         the treatment answers a finding.</li>
     *     <li>LOW_STOCK, CRITICAL when nothing is left, WARNING otherwise: no
     *         farm, block nor date, since a fertilizer belongs to none;
     *         {@code fertilizerId}.</li>
     *     <li>OPEN_HEALTH_ISSUE, WARNING: {@code date} is the day of the
     *         inspection that saw the problem; {@code findingId}.</li>
     *     <li>NURSERY_READY, WARNING: {@code blockCode} is the planned block,
     *         {@code varietyName} the variety of the batch and {@code date} the
     *         planned transplant date; {@code batchId}.</li>
     * </ul>
     *
     * @param type          kind of the alert
     * @param severity      CRITICAL or WARNING
     * @param farmId        farm concerned, possibly {@code null}
     * @param blockCode     block concerned
     * @param varietyName   variety concerned
     * @param message       the alert in one English sentence
     * @param date          the date the alert is about, as above
     * @param endDate       last day of the harvest season
     * @param daysFromToday {@code date} minus today, in days: positive before the date, 0 on it,
     *                      negative after it
     * @param varietyId     the variety row
     * @param treatmentId   the treatment
     * @param findingId     the finding
     * @param fertilizerId  the fertilizer
     * @param batchId       the nursery batch
     * @param source        source of the reference value the alert relies on
     */
    public record PlantAlert(
            PlantAlertType type,
            PlantAlertSeverity severity,
            Integer farmId,
            String blockCode,
            String varietyName,
            String message,
            LocalDate date,
            LocalDate endDate,
            Integer daysFromToday,
            Long varietyId,
            Long treatmentId,
            Long findingId,
            Long fertilizerId,
            Long batchId,
            String source) {
    }
}
