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
 * exact yield of a variety in a month of its season is
 * {@code trees × yield per tree × phase share / season length}. The months of a
 * season are rounded to 0.1 kg together, by largest remainder, so that they add
 * up to the season's exact yield rounded to 0.1 kg: 8,800 kg over three months
 * gives 2,933.4, 2,933.3 and 2,933.3 kg, not 3 × 2,933.3 = 8,799.9 kg.
 */
final class YieldForecastCalculator {

    private static final BigDecimal TENTH = new BigDecimal("0.1");

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
     * The months of the harvest season that holds a month, in order, the whole
     * season even when a window cuts it. A season over the new year starts in
     * the year before for its months after the new year: January 2027 is in
     * the November 2026 to February 2027 season.
     *
     * @param month      a month of the season
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @return the months of the season, from its first month
     * @throws IllegalArgumentException if the month is not in the season
     */
    static List<YearMonth> seasonOf(YearMonth month, int startMonth, int endMonth) {
        if (!isInSeason(month.getMonthValue(), startMonth, endMonth)) {
            throw new IllegalArgumentException(month + " is not in the season " + startMonth + "-" + endMonth);
        }
        int startYear = month.getMonthValue() >= startMonth ? month.getYear() : month.getYear() - 1;
        return window(YearMonth.of(startYear, startMonth), seasonLength(startMonth, endMonth));
    }

    /**
     * Yearly yield of a variety in a growth phase, exact:
     * {@code trees × yield per tree × phase share}. The doubles are read as the
     * decimals they print as, so 0.25 stays 0.25.
     *
     * @param treeCount      current number of trees
     * @param yieldPerTreeKg yearly yield of one tree in full production
     * @param phaseShare     share of that yield in the growth phase of the trees
     * @return the product, at least 0
     */
    static BigDecimal yearlyKg(int treeCount, double yieldPerTreeKg, double phaseShare) {
        return BigDecimal.valueOf(treeCount)
                .multiply(BigDecimal.valueOf(yieldPerTreeKg))
                .multiply(BigDecimal.valueOf(phaseShare));
    }

    /**
     * Spreads the yield of a season over its months, to 0.1 kg, by largest
     * remainder.
     *
     * <p>The exact yield of a month is its yearly yield divided by the season
     * length. Each month first gets its exact yield cut down to 0.1 kg. The
     * tenths still missing to reach the season's exact yield, rounded to 0.1 kg
     * halves up, then go one each to the months with the largest remainders;
     * on equal remainders, to the earliest month. The months add up to that
     * rounded total, and none is more than 0.1 kg from its exact yield.
     *
     * @param yearlyKgByMonth yearly yield in effect in each month of the season,
     *                        in order, 0 for a month the trees give nothing
     * @return the yield of each month, in the same order, with one decimal
     */
    static List<BigDecimal> spreadOverSeason(List<BigDecimal> yearlyKgByMonth) {
        int length = yearlyKgByMonth.size();
        BigDecimal divisor = BigDecimal.valueOf(length);
        List<BigDecimal> tenths = new ArrayList<>(length);
        List<BigDecimal> remainders = new ArrayList<>(length);
        BigDecimal yearlySum = BigDecimal.ZERO;
        BigDecimal truncatedSum = BigDecimal.ZERO.setScale(1);
        for (BigDecimal yearlyKg : yearlyKgByMonth) {
            BigDecimal truncated = yearlyKg.divide(divisor, 1, RoundingMode.DOWN);
            tenths.add(truncated);
            // Exact remainder × season length: the same factor for every month, so the order is kept.
            remainders.add(yearlyKg.subtract(truncated.multiply(divisor)));
            yearlySum = yearlySum.add(yearlyKg);
            truncatedSum = truncatedSum.add(truncated);
        }
        BigDecimal target = yearlySum.divide(divisor, 1, RoundingMode.HALF_UP);
        int missing = target.subtract(truncatedSum).movePointRight(1).intValueExact();
        List<Integer> byRemainder = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            byRemainder.add(i);
        }
        // A stable sort: on equal remainders, the earliest month comes first.
        byRemainder.sort((a, b) -> remainders.get(b).compareTo(remainders.get(a)));
        for (int i = 0; i < missing; i++) {
            int month = byRemainder.get(i);
            tenths.set(month, tenths.get(month).add(TENTH));
        }
        return tenths;
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
