package com.infineonbit.sustainablefarm.modules.plants.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Month-by-month arithmetic of the yield forecast, with no data of its own.
 *
 * <p>Every yield, share and harvest month comes in as a parameter, read from the
 * agronomic reference by the caller; nothing here is an agronomic value. The
 * expected yield of a variety in a month is
 * {@code trees × yield per tree × phase share × month share}, rounded to 0.1 kg.
 */
final class YieldForecastCalculator {

    private YieldForecastCalculator() {
    }

    /**
     * The months of the forecast, in order.
     *
     * @param from   first month
     * @param months number of months, at least 1
     * @return {@code from} and the months that follow it, {@code months} in all
     */
    static List<YearMonth> window(YearMonth from, int months) {
        List<YearMonth> window = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            window.add(from.plusMonths(i));
        }
        return window;
    }

    /**
     * Whether a month falls in a harvest season. A season whose end month is
     * lower than its start month runs over the new year: November to February
     * holds November, December, January and February.
     *
     * @param month      month of the year, 1 to 12
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @return {@code true} if the month is in the season, both ends included
     */
    static boolean isInSeason(int month, int startMonth, int endMonth) {
        return startMonth <= endMonth
                ? month >= startMonth && month <= endMonth
                : month >= startMonth || month <= endMonth;
    }

    /**
     * Number of months of a harvest season, both ends included.
     *
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @return from 1 (a one-month season) to 12
     */
    static int seasonLength(int startMonth, int endMonth) {
        return startMonth <= endMonth
                ? endMonth - startMonth + 1
                : 12 - startMonth + 1 + endMonth;
    }

    /**
     * Share of the yearly yield picked in a month: the yield is spread evenly
     * over the months of the season.
     *
     * @param month      the month
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @return 1 / season length inside the season, 0 outside
     */
    static double monthShare(YearMonth month, int startMonth, int endMonth) {
        return isInSeason(month.getMonthValue(), startMonth, endMonth)
                ? 1.0 / seasonLength(startMonth, endMonth)
                : 0.0;
    }

    /**
     * Age of the trees on the first day of a month, from the planting date of
     * their variety.
     *
     * @param month        the month
     * @param plantingDate date of the PLANTING event of the variety
     * @return the age, or {@code null} when the trees are planted after the
     *         first day of the month
     */
    static Period ageAtStartOf(YearMonth month, LocalDate plantingDate) {
        return GrowthPhaseCalculator.computeAge(plantingDate, month.atDay(1));
    }

    /**
     * Expected yield of a variety in a month.
     *
     * @param treeCount      current number of trees
     * @param yieldPerTreeKg yearly yield of one tree in full production
     * @param phaseShare     share of that yield in the growth phase of the trees
     * @param monthShare     share of the year's harvest picked in the month
     * @return the product, rounded to 0.1 kg
     */
    static double expectedKg(int treeCount, double yieldPerTreeKg, double phaseShare, double monthShare) {
        return roundToTenth(treeCount * yieldPerTreeKg * phaseShare * monthShare);
    }

    /**
     * Rounds a quantity to 0.1 kg, halves up. Also used on sums of rounded
     * quantities, to drop the binary noise of the addition.
     */
    static double roundToTenth(double kg) {
        return BigDecimal.valueOf(kg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Rounds a share to 4 decimals, halves up, for display only: one third is
     * shown as 0.3333, while the expected yield is computed from the exact share.
     */
    static double roundShare(double share) {
        return BigDecimal.valueOf(share).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
