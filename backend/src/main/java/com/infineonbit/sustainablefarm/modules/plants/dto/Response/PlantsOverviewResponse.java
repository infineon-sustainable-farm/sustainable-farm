package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.LocalDate;

/**
 * The four figures of the Plants dashboard on one day, computed on every read
 * with the rules of the lists they sum up; nothing of them is stored.
 *
 * <p>The tree figures count the planted variety rows that still have trees,
 * from their recorded plantings; the nursery is not counted. They do not add
 * up: trees in production out of their harvest window are neither in growth
 * stage nor ready for harvest.
 *
 * @param today           the day the figures are computed for
 * @param totalPlants     trees of the planted variety rows: the sum of their current tree counts
 * @param inGrowthStage   trees of the rows whose growth phase today, from their planting date, is
 *                        establishment (0 to 2 completed years): in the field, not producing yet
 * @param readyForHarvest trees of the rows whose maturity status today is READY_FOR_HARVEST, as given
 *                        by the harvest status
 * @param activeAlerts    number of the plant alerts of today, with the default look-ahead of 30 days
 * @param criticalAlerts  how many of them are CRITICAL
 */
public record PlantsOverviewResponse(
        LocalDate today,
        Integer totalPlants,
        Integer inGrowthStage,
        Integer readyForHarvest,
        Integer activeAlerts,
        Integer criticalAlerts) {
}
