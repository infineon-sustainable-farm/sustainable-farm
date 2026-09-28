package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;

import java.time.LocalDate;

/**
 * Health values computed on every read, with no data of their own: the
 * category of a health score and the status of a finding. None of them is
 * stored.
 */
final class HealthCalculator {

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
}
