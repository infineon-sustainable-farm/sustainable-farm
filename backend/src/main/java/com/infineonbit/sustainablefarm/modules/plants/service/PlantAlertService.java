package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertCalculator.HarvestWindow;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.ForecastBasis;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PhaseOfMonth;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PlantedVariety;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The plant alerts: harvest seasons, pre-harvest intervals, low stocks, open
 * health problems and nursery batches ready to transplant.
 *
 * <p>They are computed on every read, with the computations of the other
 * reads of the module: the forecast decides in which months the trees of a
 * variety give a yield, the fertilizer catalogue which stock is low, the
 * health records which problem is open and from when a treated block can be
 * harvested, the nursery at which stage a batch is. Nothing is stored and
 * nothing is written: an alert warns and never blocks anything.
 */
@Service
public class PlantAlertService {

    /**
     * Days ahead an upcoming harvest season is announced when none is given: a
     * default, which no source sets.
     */
    static final int DEFAULT_WITHIN_DAYS = 30;

    private final YieldForecastService yieldForecastService;
    private final HealthTreatmentService healthTreatmentService;
    private final HealthFindingService healthFindingService;
    private final FertilizerService fertilizerService;
    private final NurseryBatchService nurseryBatchService;
    private final Clock clock;

    public PlantAlertService(YieldForecastService yieldForecastService,
                             HealthTreatmentService healthTreatmentService,
                             HealthFindingService healthFindingService,
                             FertilizerService fertilizerService,
                             NurseryBatchService nurseryBatchService,
                             @Qualifier("plantsClock") Clock clock) {
        this.yieldForecastService = yieldForecastService;
        this.healthTreatmentService = healthTreatmentService;
        this.healthFindingService = healthFindingService;
        this.fertilizerService = fertilizerService;
        this.nurseryBatchService = nurseryBatchService;
        this.clock = clock;
    }

    /** A block, as the (farm, block) pair: a {@code null} farm is a farm of its own. */
    private record BlockKey(Integer farmId, String blockCode) {
    }

    /**
     * Turns a blank filter into no filter at all, as for the other lists of the
     * module.
     *
     * @param value the filter value as received, possibly {@code null}
     * @return the trimmed value, or {@code null} if it was null or blank
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Retrieves the alerts of today, optionally filtered.
     *
     * <p>A missing farm means every farm, and a missing or blank block every
     * block, as for the other lists of the module. A fertilizer belongs to no
     * farm and no block: its stock alerts are kept with a farm and left out
     * with a block. A nursery batch is matched on its planned block.
     *
     * <ul>
     *     <li>HARVEST_APPROACHING: a planted variety with an agronomic
     *         reference whose season counts, that is, holds a month in which
     *         its trees give a yield, as in the forecast. The alert starts on
     *         the first such month, and is listed from {@code withinDays} days
     *         before it to the end of the season.</li>
     *     <li>PRE_HARVEST_INTERVAL: a treatment whose block cannot be harvested
     *         yet. CRITICAL when a harvest alert of the same block is under
     *         way, WARNING otherwise.</li>
     *     <li>LOW_STOCK: a fertilizer with the "Low stock" badge; CRITICAL when
     *         nothing is left.</li>
     *     <li>OPEN_HEALTH_ISSUE: a finding not resolved yet, treated or not.</li>
     *     <li>NURSERY_READY: a batch with plants left, at the stage
     *         READY_TO_TRANSPLANT or whose planned transplant date has come.</li>
     * </ul>
     *
     * @param farmId     farm identifier, or {@code null} for every farm
     * @param blockCode  raw block value as stored (for example {@code "C"}),
     *                   or {@code null} for every block
     * @param withinDays how many days ahead an upcoming harvest season is
     *                   announced, from 1 to 90, or {@code null} for 30
     * @return the alerts of today, CRITICAL first, then by date
     * @throws IllegalStateException if the growth phase of some trees has no
     *                               yield share, as for the forecast
     */
    public PlantAlertResponse getAlerts(Integer farmId, String blockCode, Integer withinDays) {
        LocalDate today = LocalDate.now(clock);
        int window = withinDays == null ? DEFAULT_WITHIN_DAYS : withinDays;
        String block = normalizeFilter(blockCode);

        ForecastBasis basis = yieldForecastService.basis(farmId, block);
        List<PlantAlert> alerts = new ArrayList<>();
        Set<BlockKey> blocksInSeason = new HashSet<>();
        for (PlantedVariety planted : basis.plantedVarieties()) {
            Optional<HarvestWindow> harvest = harvestWindow(planted, basis.sharesByPhase(), today);
            if (harvest.isEmpty()) {
                continue;
            }
            if (harvest.get().isOpenOn(today)) {
                blocksInSeason.add(new BlockKey(planted.variety().getFarmId(), planted.variety().getBlockCode()));
            }
            int days = PlantAlertCalculator.daysFromToday(today, harvest.get().start());
            if (days <= window) {
                alerts.add(harvestAlert(planted, harvest.get(), days));
            }
        }
        for (HealthTreatmentResponse treatment : healthTreatmentService.getAllTreatments(farmId, block, null, today)) {
            if (treatment.harvestAllowedFrom().isAfter(today)) {
                boolean inSeason = blocksInSeason.contains(new BlockKey(treatment.farmId(), treatment.blockCode()));
                alerts.add(preHarvestIntervalAlert(treatment, inSeason, today));
            }
        }
        if (block == null) {
            for (FertilizerResponse fertilizer : fertilizerService.getAllFertilizers()) {
                if (fertilizer.belowThreshold()) {
                    alerts.add(lowStockAlert(fertilizer));
                }
            }
        }
        for (HealthFindingResponse finding : healthFindingService.getAllFindings(farmId, block, null, null, null)) {
            if (finding.status() == HealthFindingStatus.UNTREATED
                    || finding.status() == HealthFindingStatus.IN_PROGRESS) {
                alerts.add(openHealthIssueAlert(finding, today));
            }
        }
        for (NurseryBatchResponse batch : nurseryBatchService.getAllBatches(farmId, null)) {
            if (isReady(batch, today) && (block == null || block.equals(batch.plannedBlockCode()))) {
                alerts.add(nurseryReadyAlert(batch, today));
            }
        }
        alerts.sort(PlantAlertCalculator.ORDER);
        return new PlantAlertResponse(today, window, alerts, basis.varietiesWithoutReference());
    }

    /**
     * The harvest window of a planted variety on a day. A month is productive
     * when the growth phase of the trees on its first day has a yield share
     * above 0: the rule of the forecast.
     */
    private static Optional<HarvestWindow> harvestWindow(PlantedVariety planted,
                                                         Map<String, GrowthPhaseYieldShare> sharesByPhase,
                                                         LocalDate today) {
        VarietyReference reference = planted.reference();
        return PlantAlertCalculator.harvestWindow(today, reference.getHarvestStartMonth(),
                reference.getHarvestEndMonth(), month -> {
                    PhaseOfMonth phase = YieldForecastService.phaseAtStartOf(
                            month, planted.plantingDate(), sharesByPhase);
                    return phase != null && phase.share().getYieldShare() > 0;
                });
    }

    /**
     * "Keitt on block C: harvest season starts on 1 May 2027, in 24 days", or
     * "… harvest season in progress, from 1 May 2027 to 31 July 2027".
     */
    private static PlantAlert harvestAlert(PlantedVariety planted, HarvestWindow harvest, int days) {
        Variety variety = planted.variety();
        String subject = variety.getName() + " on block " + variety.getBlockCode() + ": harvest season ";
        String message = days > 0
                ? subject + "starts on " + PlantAlertCalculator.formatDate(harvest.start()) + ", "
                        + PlantAlertCalculator.relativeDays(days)
                : subject + "in progress, from " + PlantAlertCalculator.formatDate(harvest.start()) + " to "
                        + PlantAlertCalculator.formatDate(harvest.end());
        return new PlantAlert(PlantAlertType.HARVEST_APPROACHING, PlantAlertSeverity.WARNING,
                variety.getFarmId(), variety.getBlockCode(), variety.getName(), message,
                harvest.start(), harvest.end(), days,
                variety.getId(), null, null, null, null,
                planted.reference().getSeasonSource());
    }

    /**
     * "Block C, harvest season open: harvest allowed from 22 May 2027, in 7
     * days (Insecticide B applied on 8 May 2027, 14-day pre-harvest
     * interval)"; without the season part outside the harvest season.
     */
    private static PlantAlert preHarvestIntervalAlert(HealthTreatmentResponse treatment, boolean inSeason,
                                                      LocalDate today) {
        int days = PlantAlertCalculator.daysFromToday(today, treatment.harvestAllowedFrom());
        String message = "Block " + treatment.blockCode() + (inSeason ? ", harvest season open" : "")
                + ": harvest allowed from " + PlantAlertCalculator.formatDate(treatment.harvestAllowedFrom())
                + ", " + PlantAlertCalculator.relativeDays(days)
                + " (" + treatment.productName()
                + " applied on " + PlantAlertCalculator.formatDate(treatment.treatedOn())
                + ", " + treatment.preHarvestIntervalDays() + "-day pre-harvest interval)";
        return new PlantAlert(PlantAlertType.PRE_HARVEST_INTERVAL,
                inSeason ? PlantAlertSeverity.CRITICAL : PlantAlertSeverity.WARNING,
                treatment.farmId(), treatment.blockCode(), null, message,
                treatment.harvestAllowedFrom(), null, days,
                null, treatment.id(), treatment.findingId(), null, null,
                null);
    }

    /**
     * "NPK 15-15-15: 30 kg left, at or below the alert threshold of 50 kg", or
     * "NPK 15-15-15: out of stock (alert threshold 50 kg)".
     */
    private static PlantAlert lowStockAlert(FertilizerResponse fertilizer) {
        String unit = fertilizer.unit().getSymbol();
        String threshold = quantity(fertilizer.reorderThreshold()) + " " + unit;
        boolean empty = fertilizer.currentStock() <= 0;
        String message = empty
                ? fertilizer.name() + ": out of stock (alert threshold " + threshold + ")"
                : fertilizer.name() + ": " + quantity(fertilizer.currentStock()) + " " + unit
                        + " left, at or below the alert threshold of " + threshold;
        return new PlantAlert(PlantAlertType.LOW_STOCK,
                empty ? PlantAlertSeverity.CRITICAL : PlantAlertSeverity.WARNING,
                null, null, null, message,
                null, null, null,
                null, null, null, fertilizer.id(), null,
                null);
    }

    /** A quantity of the fertilizer API as written in a message, as in the refusals: 50.0 is "50". */
    private static String quantity(Double value) {
        return FertilizerStockCalculator.plain(BigDecimal.valueOf(value));
    }

    /**
     * "Block C: Anthracnose seen on 3 May 2027, 12 days ago, untreated", or
     * "…, treated, not resolved yet"; "Block C, tree 42: …" for one tree. The
     * label of "Other" names the problem.
     */
    private static PlantAlert openHealthIssueAlert(HealthFindingResponse finding, LocalDate today) {
        int days = PlantAlertCalculator.daysFromToday(today, finding.inspectedOn());
        String place = "Block " + finding.blockCode()
                + (finding.treeLabel() == null ? "" : ", tree " + finding.treeLabel());
        String problem = finding.otherLabel() == null ? finding.issueName() : finding.otherLabel();
        String treatment = finding.status() == HealthFindingStatus.UNTREATED
                ? "untreated"
                : "treated, not resolved yet";
        String message = place + ": " + problem + " seen on " + PlantAlertCalculator.formatDate(finding.inspectedOn())
                + ", " + PlantAlertCalculator.relativeDays(days) + ", " + treatment;
        return new PlantAlert(PlantAlertType.OPEN_HEALTH_ISSUE, PlantAlertSeverity.WARNING,
                finding.farmId(), finding.blockCode(), null, message,
                finding.inspectedOn(), null, days,
                null, null, finding.id(), null, null,
                null);
    }

    /**
     * Whether a batch should be transplanted: it has plants left, and is ready
     * to transplant or its planned transplant date has come. A batch
     * transplanted or lost in full has no plant left.
     */
    private static boolean isReady(NurseryBatchResponse batch, LocalDate today) {
        return batch.currentCount() > 0
                && (batch.currentStage() == NurseryStage.READY_TO_TRANSPLANT
                    || !batch.plannedTransplantOn().isAfter(today));
    }

    /**
     * "Batch P1 (Keitt, 120 plants): ready to transplant since 20 April 2027;
     * transplant planned on 15 June 2027 to block D, in 31 days", or, at
     * another stage, "Batch P1 (Keitt, 120 plants): transplant planned on 15
     * June 2027 to block D, 14 days ago; current stage hardening".
     */
    private static PlantAlert nurseryReadyAlert(NurseryBatchResponse batch, LocalDate today) {
        int days = PlantAlertCalculator.daysFromToday(today, batch.plannedTransplantOn());
        String head = "Batch " + batch.batchCode() + " (" + batch.varietyName() + ", " + batch.currentCount()
                + (batch.currentCount() == 1 ? " plant" : " plants") + "): ";
        String planned = "transplant planned on " + PlantAlertCalculator.formatDate(batch.plannedTransplantOn())
                + (batch.plannedBlockCode() == null ? "" : " to block " + batch.plannedBlockCode())
                + ", " + PlantAlertCalculator.relativeDays(days);
        String message;
        if (batch.currentStage() == NurseryStage.READY_TO_TRANSPLANT) {
            message = head + "ready to transplant since " + PlantAlertCalculator.formatDate(batch.currentStageSince())
                    + "; " + planned;
        } else {
            message = head + planned + "; "
                    + (batch.currentStage() == null
                            ? "no stage recorded"
                            : "current stage " + PlantAlertCalculator.stageName(batch.currentStage()));
        }
        return new PlantAlert(PlantAlertType.NURSERY_READY, PlantAlertSeverity.WARNING,
                batch.farmId(), batch.plannedBlockCode(), batch.varietyName(), message,
                batch.plannedTransplantOn(), null, days,
                null, null, null, null, batch.id(),
                null);
    }
}
