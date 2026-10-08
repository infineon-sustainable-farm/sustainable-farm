package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.Entry;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.MonthlyTotal;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class YieldForecastService {

    /** {@code basis} of every forecast: the recorded plantings, never a scenario. */
    static final String RECORDED_PLANTINGS_BASIS = "recorded_plantings";

    /** Number of months of the forecast when none is given. */
    static final int DEFAULT_MONTHS = 6;

    private final PopulationEventRepository populationEventRepository;
    private final VarietyReferenceRepository varietyReferenceRepository;
    private final GrowthPhaseYieldShareService growthPhaseYieldShareService;

    /**
     * What the forecast is computed from.
     *
     * @param plantedVarieties          the planted variety rows that still have trees and have an
     *                                  agronomic reference, in the order of the plantings
     * @param varietiesWithoutReference the planted variety rows that still have trees but whose
     *                                  name is not in the reference, in the same order
     * @param sharesByPhase             yield share in effect of every growth phase, by phase label
     */
    record ForecastBasis(List<PlantedVariety> plantedVarieties,
                         List<VarietyWithoutReference> varietiesWithoutReference,
                         Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {
    }

    /**
     * A planted variety row that still has trees, with its agronomic reference.
     *
     * @param variety      the variety row
     * @param plantingDate date of its oldest PLANTING event
     * @param treeCount    its current number of trees, above 0
     * @param reference    its agronomic reference
     */
    record PlantedVariety(Variety variety, LocalDate plantingDate, int treeCount, VarietyReference reference) {
    }

    /**
     * The growth phase of trees on the first day of a month.
     *
     * @param age         age of the trees on that day
     * @param growthPhase growth phase for that age
     * @param share       yield share in effect of that phase, with its source
     */
    record PhaseOfMonth(Period age, String growthPhase, GrowthPhaseYieldShareResponse share) {
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
     * One planting per variety row, the oldest. A variety row is planted once, so
     * this only guards against a second PLANTING written outside the API.
     *
     * @param plantings PLANTING events ordered by block, name, variety row, then date
     * @return the oldest planting of each variety row, in the same order
     */
    private static Collection<PopulationEvent> oldestPlantingPerVariety(List<PopulationEvent> plantings) {
        Map<Long, PopulationEvent> byVariety = new LinkedHashMap<>();
        for (PopulationEvent planting : plantings) {
            byVariety.putIfAbsent(planting.getVariety().getId(), planting);
        }
        return byVariety.values();
    }

    /**
     * Retrieves the expected yield, month by month, of the planted varieties
     * matching the optional filters.
     *
     * <p>The farm and block filters work as for the other lists of the module:
     * a missing farm means every farm. The forecast starts at the current month
     * when {@code from} is missing, and lasts 6 months when {@code months} is.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @param from      first month, or {@code null} for the current month
     * @param months    number of months, from 1 to 24, or {@code null} for 6
     * @return the forecast
     */
    public YieldForecastResponse getYieldForecast(Integer farmId, String blockCode, YearMonth from, Integer months) {
        return getYieldForecast(farmId, blockCode, from, months, LocalDate.now());
    }

    /**
     * Same as {@link #getYieldForecast(Integer, String, YearMonth, Integer)},
     * against an explicit current date so the default month can be tested.
     */
    YieldForecastResponse getYieldForecast(Integer farmId, String blockCode, YearMonth from, Integer months,
                                           LocalDate today) {
        YearMonth firstMonth = from == null ? YearMonth.from(today) : from;
        List<YearMonth> window = YieldForecastCalculator.window(firstMonth, months == null ? DEFAULT_MONTHS : months);
        ForecastBasis basis = basis(farmId, blockCode);

        List<Entry> entries = new ArrayList<>();
        for (PlantedVariety planted : basis.plantedVarieties()) {
            for (YearMonth month : window) {
                entry(month, planted.variety(), planted.treeCount(), planted.plantingDate(), planted.reference(),
                        basis.sharesByPhase()).ifPresent(entries::add);
            }
        }
        // A stable sort: within a month, the entries keep the block, then name, order of the plantings.
        entries.sort(Comparator.comparing(Entry::month));

        List<MonthlyTotal> monthlyTotals = window.stream()
                .map(month -> new MonthlyTotal(month, YieldForecastCalculator.roundToTenth(entries.stream()
                        .filter(entry -> entry.month().equals(month))
                        .mapToDouble(Entry::expectedKg)
                        .sum())))
                .toList();
        return new YieldForecastResponse(firstMonth, window.size(), RECORDED_PLANTINGS_BASIS,
                monthlyTotals, entries, basis.varietiesWithoutReference());
    }

    /**
     * Reads what the forecast is computed from: the planted variety rows
     * matching the filters, each with its current number of trees and its
     * agronomic reference, and the yield share in effect of each growth phase.
     * The plant alerts read it too, so that a harvest alert says what the
     * forecast says.
     *
     * <p>A variety row with no tree left is left out: nothing to harvest. A
     * variety row whose name is not in the reference is listed apart.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @return the planted variety rows, those without reference, and the shares
     */
    ForecastBasis basis(Integer farmId, String blockCode) {
        Collection<PopulationEvent> plantings = oldestPlantingPerVariety(
                populationEventRepository.findPlantingsByOptionalFilters(farmId, normalizeFilter(blockCode)));
        Map<Long, Integer> treeCounts = currentTreeCounts(plantings);
        List<VarietyReference> references = varietyReferenceRepository.findAll();
        Map<String, GrowthPhaseYieldShareResponse> sharesByPhase = growthPhaseYieldShareService.getAllShares().stream()
                .collect(Collectors.toMap(GrowthPhaseYieldShareResponse::growthPhase, share -> share));

        List<PlantedVariety> plantedVarieties = new ArrayList<>();
        List<VarietyWithoutReference> varietiesWithoutReference = new ArrayList<>();
        for (PopulationEvent planting : plantings) {
            Variety variety = planting.getVariety();
            Integer treeCount = treeCounts.get(variety.getId());
            if (treeCount == null || treeCount <= 0) {
                // No tree left on this row: nothing to harvest.
                continue;
            }
            Optional<VarietyReference> reference = VarietyReferenceMatcher.find(variety.getName(), references);
            if (reference.isEmpty()) {
                varietiesWithoutReference.add(new VarietyWithoutReference(
                        variety.getFarmId(), variety.getBlockCode(), variety.getName(), treeCount));
                continue;
            }
            plantedVarieties.add(new PlantedVariety(variety, planting.getEventDate(), treeCount, reference.get()));
        }
        return new ForecastBasis(plantedVarieties, varietiesWithoutReference, sharesByPhase);
    }

    /**
     * The growth phase of trees on the first day of a month, with the yield
     * share of that phase: the rule of the forecast, which the plant alerts
     * follow too.
     *
     * <p>The age is taken on the first day of the month, from the planting date
     * of the variety. Every phase has a share, its default when the user has not
     * corrected it, so the phase of any age finds one.
     *
     * @param month         the month
     * @param plantingDate  planting date of the trees
     * @param sharesByPhase yield share in effect of every growth phase, by phase label
     * @return the phase, or {@code null} when the trees are planted after the
     *         first day of the month
     */
    static PhaseOfMonth phaseAtStartOf(YearMonth month, LocalDate plantingDate,
                                       Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {
        Period age = YieldForecastCalculator.ageAtStartOf(month, plantingDate);
        if (age == null) {
            return null;
        }
        String growthPhase = GrowthPhaseCalculator.computePhase(age);
        return new PhaseOfMonth(age, growthPhase, sharesByPhase.get(growthPhase));
    }

    /**
     * Expected yield of one variety row in one month, from the growth phase of
     * its trees on the first day of the month. Trees planted after that day
     * give nothing that month.
     *
     * @return the entry, or empty when the expected yield rounds to zero
     */
    private static Optional<Entry> entry(YearMonth month, Variety variety, int treeCount, LocalDate plantingDate,
                                         VarietyReference reference,
                                         Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {
        PhaseOfMonth phase = phaseAtStartOf(month, plantingDate, sharesByPhase);
        if (phase == null) {
            return Optional.empty();
        }
        GrowthPhaseYieldShareResponse phaseShare = phase.share();
        double monthShare = YieldForecastCalculator.monthShare(
                month, reference.getHarvestStartMonth(), reference.getHarvestEndMonth());
        double expectedKg = YieldForecastCalculator.expectedKg(
                treeCount, reference.getYieldPerTreeKg(), phaseShare.yieldShare(), monthShare);
        if (expectedKg <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Entry(
                month,
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                treeCount,
                phase.age().getYears(),
                phase.growthPhase(),
                reference.getYieldPerTreeKg(),
                phaseShare.yieldShare(),
                YieldForecastCalculator.roundShare(monthShare),
                expectedKg,
                reference.getYieldSource(),
                reference.getSeasonSource(),
                phaseShare.source()));
    }

    /**
     * Current number of trees of each planted variety row: the balance of its
     * population events, the same value as {@code currentTreeCount}.
     *
     * @param plantings one planting per variety row
     * @return the balance of each variety row, by variety ID
     */
    private Map<Long, Integer> currentTreeCounts(Collection<PopulationEvent> plantings) {
        if (plantings.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = plantings.stream().map(planting -> planting.getVariety().getId()).toList();
        return populationEventRepository.findTreeBalances(ids).stream()
                .collect(Collectors.toMap(
                        TreeBalance::getVarietyId,
                        balance -> Math.toIntExact(balance.getBalance())));
    }
}
