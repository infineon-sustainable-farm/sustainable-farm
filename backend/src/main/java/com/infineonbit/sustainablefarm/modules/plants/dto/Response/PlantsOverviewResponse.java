package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * The figures of the Plants dashboard on one day, computed on every read
 * with the rules of the lists they sum up; nothing of them is stored.
 *
 * <p>The tree figures count the planted variety rows that still have trees,
 * from their recorded plantings; the nursery is not counted. They do not add
 * up: trees in production out of their harvest window are neither in growth
 * stage nor ready for harvest. The entries of {@code maturity} cover the same
 * rows as {@code totalPlants}, each row under its maturity status.
 *
 * @param today           the day the figures are computed for
 * @param totalPlants     trees of the planted variety rows: the sum of their current tree counts
 * @param inGrowthStage   trees of the rows whose growth phase today, from their planting date, is
 *                        establishment (0 to 2 completed years): in the field, not producing yet
 * @param readyForHarvest trees of the rows whose maturity status today is READY_FOR_HARVEST, as given
 *                        by the harvest status
 * @param activeAlerts    number of the plant alerts of today, with the default look-ahead of 30 days
 * @param criticalAlerts  how many of them are CRITICAL
 * @param maturity        the trees by maturity status today, as given by the harvest status: one entry
 *                        per value of {@link MaturityStatus}, in its declaration order (IMMATURE, RIPE,
 *                        READY_FOR_HARVEST), then one entry without status for the trees of the rows
 *                        absent from the agronomic reference; every entry is sent, with 0 trees when no
 *                        row has its status
 */
public record PlantsOverviewResponse(
        LocalDate today,
        Integer totalPlants,
        Integer inGrowthStage,
        Integer readyForHarvest,
        Integer activeAlerts,
        Integer criticalAlerts,
        List<MaturityShare> maturity) {

    /**
     * The trees of the planted variety rows with one maturity status.
     *
     * @param maturityStatus the status, {@code null} for the rows absent from the agronomic reference,
     *                       which have no harvest window and so no status
     * @param treeCount      trees of the rows with this status
     * @param sharePct       {@code treeCount} × 100 / {@code totalPlants}, rounded half up to one decimal,
     *                       {@code null} when {@code totalPlants} is 0; each share is rounded on its own,
     *                       so together they can make 99.9 or 100.1
     */
    public record MaturityShare(MaturityStatus maturityStatus, Integer treeCount, Double sharePct) {
    }
}
