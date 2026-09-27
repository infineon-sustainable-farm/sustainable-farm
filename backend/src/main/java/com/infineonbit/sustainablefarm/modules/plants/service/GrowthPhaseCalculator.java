package com.infineonbit.sustainablefarm.modules.plants.service;

import java.time.LocalDate;
import java.time.Period;

/**
 * Tree age, growth phase and the phase's year band, computed from the planting date.
 *
 * <p>Nothing here is stored: all values are derived each time an entry is read,
 * so they cannot drift from the planting date. When the planting date is unknown
 * the result is {@code null} — never 0 and never an estimate.
 *
 * <p>This class is the single source of the growth phase scale. The API sends both
 * the phase and its year band, so no client needs to know the thresholds.
 */
final class GrowthPhaseCalculator {

    static final String ESTABLISHMENT = "establishment";
    static final String GRADUAL_PRODUCTION = "gradual production";
    static final String FULL_PRODUCTION = "full production";

    /**
     * Growth phase scale from the Zalka 2025 study, in ascending order.
     *
     * <p>The source states, verbatim: "Years 0 to 2: establishment / Years 3 to 5:
     * gradual production / Years 5 to 7: full production".
     *
     * <p>This is a reading of the source, not an invention, and two points of it
     * are interpretation:
     * <ul>
     *     <li>Year 5 appears in both "3 to 5" and "5 to 7". The overlap is resolved
     *         to gradual production, so full production starts at 6 completed
     *         years. This is the conservative reading, and the right one:
     *         announcing full production a year early would overstate the
     *         expected yield.</li>
     *     <li>The source says nothing beyond 7 years. Full production is applied as
     *         an open-ended last band, as instructed; the study does not state it.</li>
     * </ul>
     * Each phase declares only its last completed year ({@code null} for the open
     * last band); its first year is the previous phase's last year plus one. Phase
     * and year band are both derived from these bounds, so the thresholds exist once.
     */
    private enum Phase {
        ESTABLISHMENT_PHASE(ESTABLISHMENT, 2),
        GRADUAL_PRODUCTION_PHASE(GRADUAL_PRODUCTION, 5),
        FULL_PRODUCTION_PHASE(FULL_PRODUCTION, null);

        private final String label;
        private final Integer lastCompletedYear;

        Phase(String label, Integer lastCompletedYear) {
            this.label = label;
            this.lastCompletedYear = lastCompletedYear;
        }

        private int firstCompletedYear() {
            return ordinal() == 0 ? 0 : values()[ordinal() - 1].lastCompletedYear + 1;
        }

        /** For example "0–2 yrs", or "6+ yrs" for the open last band. */
        private String yearsBand() {
            return lastCompletedYear == null
                    ? firstCompletedYear() + "+ yrs"
                    : firstCompletedYear() + "–" + lastCompletedYear + " yrs";
        }

        private static Phase forCompletedYears(int completedYears) {
            for (Phase phase : values()) {
                if (phase.lastCompletedYear == null || completedYears <= phase.lastCompletedYear) {
                    return phase;
                }
            }
            throw new IllegalStateException("The last phase must be open-ended");
        }
    }

    private GrowthPhaseCalculator() {
    }

    /**
     * Age of the trees, in completed years and remaining months.
     *
     * @param plantingDate planting date, possibly {@code null}
     * @param today        the reference date
     * @return the age, or {@code null} if the planting date is unknown or lies
     *         after {@code today} (a future planting has no age)
     */
    static Period computeAge(LocalDate plantingDate, LocalDate today) {
        if (plantingDate == null || plantingDate.isAfter(today)) {
            return null;
        }
        return Period.between(plantingDate, today);
    }

    /**
     * Growth phase for an age. Years are completed years: 2 years and 11 months is
     * still year 2.
     *
     * @param age the computed age, possibly {@code null}
     * @return the phase, or {@code null} if the age is unknown
     */
    static String computePhase(Period age) {
        return age == null ? null : Phase.forCompletedYears(age.getYears()).label;
    }

    /**
     * Year band of the growth phase for an age: "0–2 yrs", "3–5 yrs" or "6+ yrs".
     *
     * @param age the computed age, possibly {@code null}
     * @return the band, or {@code null} if the age, hence the phase, is unknown
     */
    static String computePhaseYearsBand(Period age) {
        return age == null ? null : Phase.forCompletedYears(age.getYears()).yearsBand();
    }
}
