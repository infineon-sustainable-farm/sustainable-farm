package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.ForecastBasis;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PlantedVariety;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The four figures of the Plants dashboard: total plants, plants in growth
 * stage, plants ready for harvest and active alerts.
 *
 * <p>Each figure sums up a list of the module, with its rules, so that the
 * dashboard and the list always agree: the planted variety rows of the
 * forecast, the harvest status and the plant alerts. Every figure is read on
 * the same day, and nothing is stored.
 */
@Service
public class PlantsOverviewService {

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
        int readyForHarvest = harvestStatusService.getHarvestStatus(farmId, blockCode, null, today).varieties()
                .stream()
                .filter(status -> status.maturityStatus() == MaturityStatus.READY_FOR_HARVEST)
                .mapToInt(VarietyHarvestStatus::treeCount)
                .sum();
        List<PlantAlert> alerts = plantAlertService.getAlerts(farmId, blockCode, null, today).alerts();
        int criticalAlerts = (int) alerts.stream()
                .filter(alert -> alert.severity() == PlantAlertSeverity.CRITICAL)
                .count();
        return new PlantsOverviewResponse(today, totalPlants, inGrowthStage, readyForHarvest, alerts.size(),
                criticalAlerts);
    }
}
