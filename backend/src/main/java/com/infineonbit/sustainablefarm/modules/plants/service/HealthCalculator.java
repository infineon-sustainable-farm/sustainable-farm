package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Health values computed on every read, with no data of their own: the
 * category of a health score, the status of a finding and the date from which
 * a treated block can be harvested. None of them is stored, and the date
 * arithmetic stays in Java, so the result is the same on every database.
 */
final class HealthCalculator {

    /**
     * What the treatments of one finding add up to.
     *
     * @param count              number of treatments
     * @param lastTreatedOn      date of the latest treatment, {@code null} without treatment
     * @param harvestAllowedFrom latest of the dates the treatments allow, {@code null} without treatment
     */
    record TreatmentSummary(int count, LocalDate lastTreatedOn, LocalDate harvestAllowedFrom) {

        static final TreatmentSummary NONE = new TreatmentSummary(0, null, null);
    }

    // Highest score of each category, the only place the bands are written:
    // 0-20 severe stress, 21-40 weakened, 41-60 moderate, 61-80 healthy, 81-100 very healthy.
    private static final int SEVERE_STRESS_MAX = 20;
    private static final int WEAKENED_MAX = 40;
    private static final int MODERATE_MAX = 60;
    private static final int HEALTHY_MAX = 80;
    private static final int SCORE_MAX = 100;

    private HealthCalculator() {
    }

    /**
     * The health category of a score.
     *
     * @param healthScorePct a score from 0 to 100, as the request validation guarantees
     * @return its category
     * @throws IllegalArgumentException if the score is outside 0 to 100
     */
    static HealthCategory category(int healthScorePct) {
        if (healthScorePct < 0 || healthScorePct > SCORE_MAX) {
            throw new IllegalArgumentException("A health score is between 0 and 100, got " + healthScorePct);
        }
        if (healthScorePct <= SEVERE_STRESS_MAX) {
            return HealthCategory.SEVERE_STRESS;
        }
        if (healthScorePct <= WEAKENED_MAX) {
            return HealthCategory.WEAKENED;
        }
        if (healthScorePct <= MODERATE_MAX) {
            return HealthCategory.MODERATE;
        }
        if (healthScorePct <= HEALTHY_MAX) {
            return HealthCategory.HEALTHY;
        }
        return HealthCategory.VERY_HEALTHY;
    }

    /**
     * The status of a finding, from its number of treatments and its resolution.
     *
     * @param treatmentCount number of treatments recorded for the finding
     * @param resolvedOn     resolution date, or {@code null} when not resolved
     * @return its status
     */
    static HealthFindingStatus status(int treatmentCount, LocalDate resolvedOn) {
        boolean treated = treatmentCount > 0;
        if (resolvedOn == null) {
            return treated ? HealthFindingStatus.IN_PROGRESS : HealthFindingStatus.UNTREATED;
        }
        return treated ? HealthFindingStatus.TREATED : HealthFindingStatus.CLOSED_WITHOUT_TREATMENT;
    }

    /**
     * The first day the block of a treatment can be harvested: the treatment
     * date plus the pre-harvest interval.
     *
     * @param treatedOn              treatment date
     * @param preHarvestIntervalDays interval in days, 0 or more
     * @return the date from which harvesting is allowed
     */
    static LocalDate harvestAllowedFrom(LocalDate treatedOn, int preHarvestIntervalDays) {
        return treatedOn.plusDays(preHarvestIntervalDays);
    }

    /**
     * What the treatments of one finding add up to. The harvest date is the
     * latest one any treatment allows, which is not always the one of the
     * latest treatment: an earlier treatment can have a longer interval.
     *
     * @param treatments the treatments of one finding, possibly empty
     * @return their count, latest date and latest allowed harvest date
     */
    static TreatmentSummary summarize(List<HealthTreatment> treatments) {
        if (treatments.isEmpty()) {
            return TreatmentSummary.NONE;
        }
        LocalDate lastTreatedOn = treatments.stream()
                .map(HealthTreatment::getTreatedOn)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate harvestAllowedFrom = treatments.stream()
                .map(treatment -> harvestAllowedFrom(treatment.getTreatedOn(), treatment.getPreHarvestIntervalDays()))
                .max(Comparator.naturalOrder())
                .orElseThrow();
        return new TreatmentSummary(treatments.size(), lastTreatedOn, harvestAllowedFrom);
    }
}
