package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse.MaturityShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.ForecastBasis;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PlantedVariety;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The figures of the Plants dashboard: total plants, plants in growth stage,
 * plants ready for harvest, active alerts, and the trees by maturity status.
 *
 * <p>Each figure sums up a list of the module, with its rules, so that the
 * dashboard and the list always agree: the planted variety rows of the
 * forecast, the harvest status and the plant alerts. Every figure is read on
 * the same day, and nothing is stored.
 */
@Service
public class PlantsOverviewService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final YieldForecastService yieldForecastService;
    private final HarvestStatusService harvestStatusService;
    private final PlantAlertService plantAlertService;
    private final Clock clock;

    public PlantsOverviewService(YieldForecastService yieldForecastService,
                                 HarvestStatusService harvestStatusService,
                                 PlantAlertService plantAlertService,
                                 @Qualifier("plantsClock") Clock clock) {
        this.yieldForecastService = yieldForecastService;
        this.harvestStatusService = harvestStatusService;
        this.plantAlertService = plantAlertService;
        this.clock = clock;
    }

    /**
     * Retrieves the figures of today, optionally filtered.
     *
     * <p>A missing farm means every farm, and a missing or blank block every
     * block, as for the lists the figures sum up. A fertilizer belongs to no
     * farm and no block: its stock alerts count with a farm and not with a
     * block, as in the alerts.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @return the figures of today
     */
    public PlantsOverviewResponse getOverview(Integer farmId, String blockCode) {
        LocalDate today = LocalDate.now(clock);
        ForecastBasis basis = yieldForecastService.basis(farmId, blockCode);
        List<PlantedVariety> planted = new ArrayList<>(basis.plantedVarieties());
        planted.addAll(basis.plantedWithoutReference());

        int totalPlants = planted.stream().mapToInt(PlantedVariety::treeCount).sum();
        int inGrowthStage = planted.stream()
                .filter(row -> GrowthPhaseCalculator.ESTABLISHMENT.equals(GrowthPhaseCalculator.computePhase(
                        GrowthPhaseCalculator.computeAge(row.plantingDate(), today))))
                .mapToInt(PlantedVariety::treeCount)
                .sum();
        List<VarietyHarvestStatus> statuses =
                harvestStatusService.getHarvestStatus(farmId, blockCode, null, today).varieties();
        int readyForHarvest = treesWith(statuses, MaturityStatus.READY_FOR_HARVEST);
        List<PlantAlert> alerts = plantAlertService.getAlerts(farmId, blockCode, null, today).alerts();
        int criticalAlerts = (int) alerts.stream()
                .filter(alert -> alert.severity() == PlantAlertSeverity.CRITICAL)
                .count();
        return new PlantsOverviewResponse(today, totalPlants, inGrowthStage, readyForHarvest, alerts.size(),
                criticalAlerts, maturityShares(statuses, totalPlants));
    }

    /**
     * The trees of each maturity status, in the declaration order of the
     * statuses, then those of the rows without agronomic reference, which have
     * no status. A status that no row has is listed with 0 trees.
     */
    private static List<MaturityShare> maturityShares(List<VarietyHarvestStatus> statuses, int totalPlants) {
        List<MaturityStatus> maturityStatuses = new ArrayList<>(List.of(MaturityStatus.values()));
        maturityStatuses.add(null);
        return maturityStatuses.stream()
                .map(maturityStatus -> {
                    int treeCount = treesWith(statuses, maturityStatus);
                    return new MaturityShare(maturityStatus, treeCount, sharePct(treeCount, totalPlants));
                })
                .toList();
    }

    /** Trees of the rows with this maturity status; {@code null} gives those without one. */
    private static int treesWith(List<VarietyHarvestStatus> statuses, MaturityStatus maturityStatus) {
        return statuses.stream()
                .filter(status -> status.maturityStatus() == maturityStatus)
                .mapToInt(VarietyHarvestStatus::treeCount)
                .sum();
    }

    /**
     * A share of the trees, computed on exact decimals.
     *
     * @param treeCount   trees of one maturity status
     * @param totalPlants trees of every planted variety row
     * @return the share in percent, rounded half up to one decimal: 40 of 170
     *         is 23.5; {@code null} when there is no tree at all
     */
    private static Double sharePct(int treeCount, int totalPlants) {
        if (totalPlants == 0) {
            return null;
        }
        return BigDecimal.valueOf(treeCount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalPlants), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
