package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.FieldObservation;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertCalculator.HarvestWindow;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertCalculator.Season;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.ForecastBasis;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PhaseOfMonth;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PlantedVariety;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The maturity status and the estimated harvest date of the planted varieties
 * (PL-05-01 and PL-05-02).
 *
 * <p>Both are estimated on every read with the rules of the plant alerts: the
 * harvest window of a variety, from its first month in which the trees give a
 * yield as in the forecast, and the pre-harvest intervals of the treatments
 * of its block. A variety is ready for harvest when its window is open and no
 * pre-harvest interval runs on its block, ripe when the window opens within
 * the look-ahead of the alerts or while an interval still runs, and immature
 * otherwise.
 *
 * <p>A maturity observation made in the field takes precedence over the
 * estimated status for the season under way on the day it was made, or the
 * next one when none was, until the last day of that season; the latest one
 * of the season counts. A running pre-harvest interval still holds an observed
 * READY_FOR_HARVEST back to RIPE. The estimated harvest date does not move.
 * No status is stored and nothing is blocked.
 */
@Service
public class HarvestStatusService {

    /** {@code statusSource} of a status that is the estimate. */
    static final String ESTIMATE_SOURCE = "estimate";

    /** {@code statusSource} of a status that is the one of a field observation. */
    static final String FIELD_OBSERVATION_SOURCE = "field_observation";

    /** Order of the lines: farm (none first), block, variety name, then variety row, sorted in Java. */
    private static final Comparator<PlantedVariety> ORDER = Comparator
            .comparing((PlantedVariety planted) -> planted.variety().getFarmId(),
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(planted -> planted.variety().getBlockCode())
            .thenComparing(planted -> planted.variety().getName())
            .thenComparing(planted -> planted.variety().getId());

    /** The treatment whose pre-harvest interval ends last, then the one recorded last. */
    private static final Comparator<HealthTreatmentResponse> LAST_INTERVAL = Comparator
            .comparing(HealthTreatmentResponse::harvestAllowedFrom)
            .thenComparing(HealthTreatmentResponse::id);

    /** The latest observation: by date, then the one recorded last. */
    private static final Comparator<MaturityObservationResponse> LATEST_OBSERVATION = Comparator
            .comparing(MaturityObservationResponse::observedOn)
            .thenComparing(MaturityObservationResponse::id);

    private final YieldForecastService yieldForecastService;
    private final HealthTreatmentService healthTreatmentService;
    private final HarvestService harvestService;
    private final MaturityObservationService maturityObservationService;
    private final Clock clock;

    public HarvestStatusService(YieldForecastService yieldForecastService,
                                HealthTreatmentService healthTreatmentService,
                                HarvestService harvestService,
                                MaturityObservationService maturityObservationService,
                                @Qualifier("plantsClock") Clock clock) {
        this.yieldForecastService = yieldForecastService;
        this.healthTreatmentService = healthTreatmentService;
        this.harvestService = harvestService;
        this.maturityObservationService = maturityObservationService;
        this.clock = clock;
    }

    /** A block, as the (farm, block) pair: a {@code null} farm is a farm of its own. */
    private record BlockKey(Integer farmId, String blockCode) {
    }

    /**
     * Turns a blank filter into no filter at all, as for the other lists of the
     * module.
     *
     * @param value the filter value as received, possibly {@code null}
     * @return the trimmed value, or {@code null} if it was null or blank
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Retrieves the harvest status of today of the planted varieties,
     * optionally filtered.
     *
     * <p>A missing farm means every farm, and a missing or blank block every
     * block, as for the other lists of the module.
     *
     * @param farmId     farm identifier, or {@code null} for every farm
     * @param blockCode  raw block value as stored (for example {@code "A"}),
     *                   or {@code null} for every block
     * @param withinDays how many days before its harvest window a variety is
     *                   ripe, from 1 to 90, or {@code null} for 30, as for the alerts
     * @return the status of every planted variety row with trees
     */
    public HarvestStatusResponse getHarvestStatus(Integer farmId, String blockCode, Integer withinDays) {
        return getHarvestStatus(farmId, blockCode, withinDays, LocalDate.now(clock));
    }

    /**
     * Same as {@link #getHarvestStatus(Integer, String, Integer)} on an
     * explicit day, so that the overview reads every figure on the same day.
     */
    HarvestStatusResponse getHarvestStatus(Integer farmId, String blockCode, Integer withinDays, LocalDate today) {
        int window = withinDays == null ? PlantAlertService.DEFAULT_WITHIN_DAYS : withinDays;
        String block = normalizeFilter(blockCode);
        ForecastBasis basis = yieldForecastService.basis(farmId, block);
        Map<BlockKey, HealthTreatmentResponse> intervals =
                runningIntervals(healthTreatmentService.getAllTreatments(farmId, block, null, today), today);
        Map<Long, List<MaturityObservationResponse>> observations = observationsByVariety(
                maturityObservationService.getAllObservations(farmId, block, null, today), today);

        Map<Long, HarvestWindow> windows = new HashMap<>();
        for (PlantedVariety planted : basis.plantedVarieties()) {
            harvestWindow(planted, basis, today).ifPresent(found -> windows.put(planted.variety().getId(), found));
        }
        Map<Long, LocalDate> firstHarvests = firstHarvests(farmId, block, windows, today);

        List<PlantedVariety> rows = new ArrayList<>(basis.plantedVarieties());
        rows.addAll(basis.plantedWithoutReference());
        rows.sort(ORDER);
        List<VarietyHarvestStatus> varieties = rows.stream()
                .map(planted -> {
                    Variety variety = planted.variety();
                    HealthTreatmentResponse interval = intervals.get(
                            new BlockKey(variety.getFarmId(), variety.getBlockCode()));
                    List<MaturityObservationResponse> ofTheRow =
                            observations.getOrDefault(variety.getId(), List.of());
                    return planted.reference() == null
                            ? withoutReference(planted, interval, ofTheRow)
                            : status(planted, windows.get(variety.getId()), interval,
                                    firstHarvests.get(variety.getId()), ofTheRow, basis, today, window);
                })
                .toList();
        return new HarvestStatusResponse(today, window, varieties);
    }

    /**
     * The harvest window of a planted variety on a day, however far ahead. A
     * month is productive when the growth phase of the trees on its first day
     * has a yield share above 0: the rule of the forecast and of the alerts.
     * The search stops at the first season the trees start in their last
     * growth phase, as no later season can differ from it.
     */
    private static Optional<HarvestWindow> harvestWindow(PlantedVariety planted, ForecastBasis basis,
                                                         LocalDate today) {
        VarietyReference reference = planted.reference();
        return PlantAlertCalculator.firstHarvestWindow(today, reference.getHarvestStartMonth(),
                reference.getHarvestEndMonth(),
                month -> {
                    PhaseOfMonth phase = YieldForecastService.phaseAtStartOf(
                            month, planted.plantingDate(), basis.sharesByPhase());
                    return phase != null && phase.share().yieldShare() > 0;
                },
                season -> {
                    Period age = YieldForecastCalculator.ageAtStartOf(
                            YearMonth.from(season.start()), planted.plantingDate());
                    return age != null && age.getYears() >= GrowthPhaseCalculator.lastPhaseFirstCompletedYear();
                });
    }

    /**
     * The pre-harvest interval that still runs on each block: the treatment
     * whose interval ends last, after today.
     */
    private static Map<BlockKey, HealthTreatmentResponse> runningIntervals(List<HealthTreatmentResponse> treatments,
                                                                           LocalDate today) {
        Map<BlockKey, HealthTreatmentResponse> intervals = new HashMap<>();
        for (HealthTreatmentResponse treatment : treatments) {
            if (treatment.harvestAllowedFrom().isAfter(today)) {
                intervals.merge(new BlockKey(treatment.farmId(), treatment.blockCode()), treatment,
                        (kept, other) -> LAST_INTERVAL.compare(kept, other) >= 0 ? kept : other);
            }
        }
        return intervals;
    }

    /**
     * The observations made up to today, by variety row, each list from the
     * earliest to the latest. One made after the day, as when the overview
     * reads an earlier day, does not exist yet.
     */
    private static Map<Long, List<MaturityObservationResponse>> observationsByVariety(
            List<MaturityObservationResponse> observations, LocalDate today) {
        Map<Long, List<MaturityObservationResponse>> byVariety = new HashMap<>();
        for (MaturityObservationResponse observation : observations) {
            if (!observation.observedOn().isAfter(today)) {
                byVariety.computeIfAbsent(observation.varietyId(), id -> new ArrayList<>()).add(observation);
            }
        }
        byVariety.values().forEach(list -> list.sort(LATEST_OBSERVATION));
        return byVariety;
    }

    /**
     * The date of the first harvest of each variety row in its open harvest
     * window, from one read of the harvests since the earliest open window.
     * A window still to come holds no harvest.
     */
    private Map<Long, LocalDate> firstHarvests(Integer farmId, String blockCode, Map<Long, HarvestWindow> windows,
                                               LocalDate today) {
        Optional<LocalDate> earliest = windows.values().stream()
                .filter(found -> found.isOpenOn(today))
                .map(HarvestWindow::start)
                .min(Comparator.naturalOrder());
        Map<Long, LocalDate> firstHarvests = new HashMap<>();
        if (earliest.isEmpty()) {
            return firstHarvests;
        }
        for (HarvestResponse harvest : harvestService.getAllHarvests(farmId, blockCode, earliest.get(), today)) {
            HarvestWindow found = windows.get(harvest.varietyId());
            if (found != null && found.isOpenOn(today) && found.isOpenOn(harvest.harvestDate())) {
                // The harvests come by date: the first one seen is the first of the window.
                firstHarvests.putIfAbsent(harvest.varietyId(), harvest.harvestDate());
            }
        }
        return firstHarvests;
    }

    /**
     * The estimated maturity status of a variety on a day.
     *
     * @param window          its harvest window, or {@code null} when the trees never give a yield
     * @param today           the day
     * @param withinDays      how many days before its window a variety is ripe
     * @param intervalRunning whether a pre-harvest interval still runs on its block
     * @return the status
     */
    static MaturityStatus maturityStatus(HarvestWindow window, LocalDate today, int withinDays,
                                         boolean intervalRunning) {
        if (window == null) {
            return MaturityStatus.IMMATURE;
        }
        if (window.isOpenOn(today)) {
            return intervalRunning ? MaturityStatus.RIPE : MaturityStatus.READY_FOR_HARVEST;
        }
        return PlantAlertCalculator.daysFromToday(today, window.start()) <= withinDays
                ? MaturityStatus.RIPE
                : MaturityStatus.IMMATURE;
    }

    /**
     * The observation that counts on a day: the latest one made for the season
     * under way, or the next one when none is. An observation is made for the
     * season under way on its own day, or for the next one when none was, so
     * one made between two seasons counts for the coming one.
     *
     * @param observations the observations of the variety row up to the day, earliest first
     * @param season       the season under way on the day, or the next one
     * @param reference    the agronomic reference that gives the seasons
     * @return the observation, or {@code null} when none counts
     */
    private static MaturityObservationResponse countingObservation(List<MaturityObservationResponse> observations,
                                                                   Season season, VarietyReference reference) {
        MaturityObservationResponse counting = null;
        for (MaturityObservationResponse observation : observations) {
            Season ofTheObservation = PlantAlertCalculator.currentOrNextSeason(observation.observedOn(),
                    reference.getHarvestStartMonth(), reference.getHarvestEndMonth());
            if (ofTheObservation.equals(season)) {
                counting = observation;
            }
        }
        return counting;
    }

    private static FieldObservation fieldObservation(MaturityObservationResponse observation, LocalDate countsUntil) {
        return new FieldObservation(
                observation.id(),
                observation.observedOn(),
                observation.maturityStatus(),
                observation.method(),
                observation.measuredValue(),
                observation.measuredUnit(),
                observation.observer(),
                observation.source(),
                countsUntil);
    }

    private static VarietyHarvestStatus status(PlantedVariety planted, HarvestWindow window,
                                               HealthTreatmentResponse interval, LocalDate firstHarvestDate,
                                               List<MaturityObservationResponse> observations,
                                               ForecastBasis basis, LocalDate today, int withinDays) {
        Variety variety = planted.variety();
        VarietyReference reference = planted.reference();
        String phaseShareSource = window == null
                ? null
                : YieldForecastService.phaseAtStartOf(YearMonth.from(window.start()), planted.plantingDate(),
                        basis.sharesByPhase()).share().source();
        MaturityStatus estimatedStatus = maturityStatus(window, today, withinDays, interval != null);

        Season season = PlantAlertCalculator.currentOrNextSeason(today, reference.getHarvestStartMonth(),
                reference.getHarvestEndMonth());
        MaturityObservationResponse counting = countingObservation(observations, season, reference);
        MaturityStatus maturityStatus = estimatedStatus;
        String statusSource = ESTIMATE_SOURCE;
        if (counting != null) {
            // Ready for harvest means harvestable today: a running interval holds an observed READY back too.
            maturityStatus = counting.maturityStatus() == MaturityStatus.READY_FOR_HARVEST && interval != null
                    ? MaturityStatus.RIPE
                    : counting.maturityStatus();
            statusSource = FIELD_OBSERVATION_SOURCE;
        }
        return new VarietyHarvestStatus(
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                planted.treeCount(),
                maturityStatus,
                statusSource,
                estimatedStatus,
                counting == null ? null : fieldObservation(counting, season.end()),
                window == null ? null : window.start(),
                window == null ? null : window.end(),
                window == null ? null : PlantAlertCalculator.daysFromToday(today, window.start()),
                interval == null ? null : interval.harvestAllowedFrom(),
                interval == null ? null : interval.id(),
                firstHarvestDate,
                reference.getSeasonSource(),
                phaseShareSource);
    }

    /**
     * A planted variety without agronomic reference: no season, hence no
     * status, estimated or observed. Its latest observation is shown, without
     * counting; the interval still shows.
     */
    private static VarietyHarvestStatus withoutReference(PlantedVariety planted, HealthTreatmentResponse interval,
                                                         List<MaturityObservationResponse> observations) {
        Variety variety = planted.variety();
        MaturityObservationResponse latest = observations.isEmpty() ? null : observations.getLast();
        return new VarietyHarvestStatus(
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                planted.treeCount(),
                null,
                null,
                null,
                latest == null ? null : fieldObservation(latest, null),
                null,
                null,
                null,
                interval == null ? null : interval.harvestAllowedFrom(),
                interval == null ? null : interval.id(),
                null,
                null,
                null);
    }
}
