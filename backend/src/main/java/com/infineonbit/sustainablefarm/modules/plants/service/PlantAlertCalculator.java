package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Dates, wording and order of the plant alerts, with no data of their own:
 * every month, date and rule comes in as a parameter.
 */
final class PlantAlertCalculator {

    /**
     * A harvest season, from the first day of its first month to the last day
     * of its last month.
     */
    record Season(LocalDate start, LocalDate end) {

        /** The months of the season, in order. */
        List<YearMonth> months() {
            List<YearMonth> months = new ArrayList<>();
            for (YearMonth month = YearMonth.from(start); !month.isAfter(YearMonth.from(end));
                 month = month.plusMonths(1)) {
                months.add(month);
            }
            return months;
        }
    }

    /**
     * The part of a harvest season in which the trees give a yield: from the
     * first day of its first productive month to the last day of the season.
     */
    record HarvestWindow(LocalDate start, LocalDate end) {

        /** Whether the day falls in the window, both ends included. */
        boolean isOpenOn(LocalDate day) {
            return !start.isAfter(day) && !end.isBefore(day);
        }
    }

    /**
     * Order of the alerts: CRITICAL first; then by date, the alerts without a
     * date last; then by type, farm (none first), block (none first) and
     * variety; then by the row concerned, so that two reads list the same
     * alerts in the same order. Sorted in Java, as databases place NULL
     * differently.
     */
    static final Comparator<PlantAlert> ORDER = Comparator
            .comparing(PlantAlert::severity)
            .thenComparing(PlantAlert::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PlantAlert::type)
            .thenComparing(PlantAlert::farmId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PlantAlert::blockCode, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PlantAlert::varietyName, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PlantAlertCalculator::concernedId, Comparator.nullsFirst(Comparator.naturalOrder()));

    /** A date as written in a message, for example "1 May 2027". */
    private static final DateTimeFormatter MESSAGE_DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);

    private PlantAlertCalculator() {
    }

    /**
     * The harvest season under way on a day, or the next one when none is.
     *
     * <p>A season whose end month is lower than its start month runs over the
     * new year, as in the forecast: on 15 January, a November to February
     * season is the one that started on 1 November of the year before.
     *
     * @param today      the day
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @return the season that holds the day, or else the next one to start
     */
    static Season currentOrNextSeason(LocalDate today, int startMonth, int endMonth) {
        int length = YieldForecastCalculator.seasonLength(startMonth, endMonth);
        YearMonth startThisYear = YearMonth.of(today.getYear(), startMonth);
        YearMonth lastStart = YearMonth.from(today).isBefore(startThisYear)
                ? startThisYear.minusYears(1)
                : startThisYear;
        Season last = season(lastStart, length);
        return today.isAfter(last.end()) ? season(lastStart.plusYears(1), length) : last;
    }

    /** The same season, one year later. */
    static Season nextYear(Season season) {
        return season(YearMonth.from(season.start()).plusYears(1), season.months().size());
    }

    private static Season season(YearMonth firstMonth, int length) {
        return new Season(firstMonth.atDay(1), firstMonth.plusMonths(length - 1L).atEndOfMonth());
    }

    /**
     * The harvest window of a variety on a day: the season under way, or the
     * next one, from its first month in which the trees give a yield.
     *
     * <p>A season counts as soon as one of its months is productive. When none
     * of the months of the season under way is, because the trees are still
     * too young, the next season is taken. When the next season to start has
     * none either, there is no window: the season after it starts more than a
     * year later, beyond any look-ahead of the alerts.
     *
     * @param today      the day
     * @param startMonth first month of the season, 1 to 12
     * @param endMonth   last month of the season, 1 to 12
     * @param productive whether the trees give a yield in a month, as the forecast decides it
     * @return the window, which never ends before the day, or empty when no season counts
     */
    static Optional<HarvestWindow> harvestWindow(LocalDate today, int startMonth, int endMonth,
                                                 Predicate<YearMonth> productive) {
        Season season = currentOrNextSeason(today, startMonth, endMonth);
        Optional<HarvestWindow> window = firstProductive(season, productive);
        if (window.isEmpty() && !season.start().isAfter(today)) {
            window = firstProductive(nextYear(season), productive);
        }
        return window;
    }

    private static Optional<HarvestWindow> firstProductive(Season season, Predicate<YearMonth> productive) {
        return season.months().stream()
                .filter(productive)
                .findFirst()
                .map(month -> new HarvestWindow(month.atDay(1), season.end()));
    }

    /**
     * Days from a day to a date.
     *
     * @return positive when the date is after the day, 0 on it, negative before it
     */
    static int daysFromToday(LocalDate today, LocalDate date) {
        return Math.toIntExact(ChronoUnit.DAYS.between(today, date));
    }

    /**
     * A number of days from today in words: "in 30 days", "in 1 day",
     * "today", "1 day ago", "12 days ago".
     */
    static String relativeDays(int days) {
        if (days == 0) {
            return "today";
        }
        int count = Math.abs(days);
        String span = count + (count == 1 ? " day" : " days");
        return days > 0 ? "in " + span : span + " ago";
    }

    /** A date as written in a message, for example "1 May 2027". */
    static String formatDate(LocalDate date) {
        return date.format(MESSAGE_DATE);
    }

    /** A nursery stage as written in a message, for example "ready to transplant". */
    static String stageName(NurseryStage stage) {
        return stage.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /**
     * The row an alert is about, the last criterion of {@link #ORDER}. The
     * switch has no default, so a new type does not compile until it names
     * its row.
     */
    private static Long concernedId(PlantAlert alert) {
        return switch (alert.type()) {
            case HARVEST_APPROACHING -> alert.varietyId();
            case PRE_HARVEST_INTERVAL -> alert.treatmentId();
            case LOW_STOCK -> alert.fertilizerId();
            case OPEN_HEALTH_ISSUE -> alert.findingId();
            case NURSERY_READY -> alert.batchId();
        };
    }
}
