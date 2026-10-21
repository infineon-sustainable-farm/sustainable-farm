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
     * @param plantedVarieties        the planted variety rows that still have trees and have an
     *                                agronomic reference, in the order of the plantings
     * @param plantedWithoutReference the planted variety rows that still have trees but whose
     *                                name is not in the reference, in the same order
     * @param sharesByPhase           yield share in effect of every growth phase, by phase label
     */
    record ForecastBasis(List<PlantedVariety> plantedVarieties,
                         List<PlantedVariety> plantedWithoutReference,
                         Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {

        /** The planted variety rows without reference, as the forecast and the alerts list them. */
        List<VarietyWithoutReference> varietiesWithoutReference() {
            return plantedWithoutReference.stream()
                    .map(planted -> new VarietyWithoutReference(planted.variety().getFarmId(),
                            planted.variety().getBlockCode(), planted.variety().getName(), planted.treeCount()))
                    .toList();
        }
    }

    /**
     * A planted variety row that still has trees, with its agronomic reference.
     *
     * @param variety      the variety row
     * @param plantingDate date of its oldest PLANTING event
     * @param treeCount    its current number of trees, above 0
     * @param reference    its agronomic reference, {@code null} for a row of
     *                     {@link ForecastBasis#plantedWithoutReference()}
     */
    record PlantedVariety(Variety variety, LocalDate plantingDate, int treeCount, VarietyReference reference) {
    }

    /**
     * The expected yield of a planted variety row in one month, with what it
     * is computed from.
     *
     * @param phase      growth phase of the trees on the first day of the month, with its share
     * @param monthShare share of the year's harvest picked in the month, 0 outside the season
     * @param expectedKg expected yield, rounded to 0.1 kg, possibly 0
     */
    record MonthExpectation(PhaseOfMonth phase, double monthShare, double expectedKg) {
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
                entry(month, planted, basis.sharesByPhase()).ifPresent(entries::add);
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
     * The plant alerts, the yield comparison, the harvest status and the
     * overview read it too, so that they say what the forecast says.
     *
     * <p>A variety row with no tree left is left out: nothing to harvest. A
     * variety row whose name is not in the reference is listed apart, with its
     * planting date and its trees.
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
        List<PlantedVariety> plantedWithoutReference = new ArrayList<>();
        for (PopulationEvent planting : plantings) {
            Variety variety = planting.getVariety();
            Integer treeCount = treeCounts.get(variety.getId());
            if (treeCount == null || treeCount <= 0) {
                // No tree left on this row: nothing to harvest.
                continue;
            }
            Optional<VarietyReference> reference = VarietyReferenceMatcher.find(variety.getName(), references);
            if (reference.isEmpty()) {
                plantedWithoutReference.add(new PlantedVariety(variety, planting.getEventDate(), treeCount, null));
                continue;
            }
            plantedVarieties.add(new PlantedVariety(variety, planting.getEventDate(), treeCount, reference.get()));
        }
        return new ForecastBasis(plantedVarieties, plantedWithoutReference, sharesByPhase);
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
     * Expected yield of a planted variety row in one month, from the growth
     * phase of its trees on the first day of the month: the rule of the
     * forecast, which the yield comparison follows too.
     *
     * @param month         the month
     * @param planted       a planted variety row with its agronomic reference
     * @param sharesByPhase yield share in effect of every growth phase, by phase label
     * @return the expected yield, possibly 0, or empty when the trees are
     *         planted after the first day of the month: they give nothing that month
     */
    static Optional<MonthExpectation> expectationIn(YearMonth month, PlantedVariety planted,
                                                    Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {
        PhaseOfMonth phase = phaseAtStartOf(month, planted.plantingDate(), sharesByPhase);
        if (phase == null) {
            return Optional.empty();
        }
        VarietyReference reference = planted.reference();
        double monthShare = YieldForecastCalculator.monthShare(
                month, reference.getHarvestStartMonth(), reference.getHarvestEndMonth());
        double expectedKg = YieldForecastCalculator.expectedKg(
                planted.treeCount(), reference.getYieldPerTreeKg(), phase.share().yieldShare(), monthShare);
        return Optional.of(new MonthExpectation(phase, monthShare, expectedKg));
    }

    /**
     * The forecast entry of one variety row in one month.
     *
     * @return the entry, or empty when the trees give nothing that month or the
     *         expected yield rounds to zero
     */
    private static Optional<Entry> entry(YearMonth month, PlantedVariety planted,
                                         Map<String, GrowthPhaseYieldShareResponse> sharesByPhase) {
        Optional<MonthExpectation> found = expectationIn(month, planted, sharesByPhase);
        if (found.isEmpty() || found.get().expectedKg() <= 0) {
            return Optional.empty();
        }
        MonthExpectation expectation = found.get();
        PhaseOfMonth phase = expectation.phase();
        GrowthPhaseYieldShareResponse phaseShare = phase.share();
        Variety variety = planted.variety();
        VarietyReference reference = planted.reference();
        int treeCount = planted.treeCount();
        double monthShare = expectation.monthShare();
        double expectedKg = expectation.expectedKg();
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
