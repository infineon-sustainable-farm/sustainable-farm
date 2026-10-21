package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.BlockComparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.Comparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.NotCompared;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.PhaseShare;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.VarietyComparison;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.ForecastBasis;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.MonthExpectation;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService.PlantedVariety;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The actual yield of the planted varieties against their expected yield,
 * over the same whole months.
 *
 * <p>The expected yield is computed with the rule of the yield forecast, month
 * by month, so that it is the forecast of the same months; the actual yield
 * is the sum of the recorded harvests. Both are computed on every read and
 * never stored. The expected yield of past months uses today's trees and
 * today's reference values: nothing of an earlier computation is kept.
 */
@Service
public class YieldComparisonService {

    /** Number of months compared when none is given: the calendar year, the campaign. */
    static final int DEFAULT_MONTHS = 12;

    /** Reason of a variety row not compared because its name is not in the agronomic reference. */
    static final String NO_REFERENCE = "NO_REFERENCE";

    /** Reason of a variety row not compared because it has no tree left. */
    static final String NO_TREE_LEFT = "NO_TREE_LEFT";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Order of the lines: farm (none first), block, variety name, then variety row, sorted in Java. */
    private static final Comparator<Variety> VARIETY_ORDER = Comparator
            .comparing(Variety::getFarmId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Variety::getBlockCode)
            .thenComparing(Variety::getName)
            .thenComparing(Variety::getId);

    private static final Comparator<NotCompared> NOT_COMPARED_ORDER = Comparator
            .comparing(NotCompared::farmId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(NotCompared::blockCode)
            .thenComparing(NotCompared::varietyName)
            .thenComparing(NotCompared::varietyId);

    private final YieldForecastService yieldForecastService;
    private final HarvestService harvestService;
    private final Clock clock;

    public YieldComparisonService(YieldForecastService yieldForecastService,
                                  HarvestService harvestService,
                                  @Qualifier("plantsClock") Clock clock) {
        this.yieldForecastService = yieldForecastService;
        this.harvestService = harvestService;
        this.clock = clock;
    }

    /** A block, as the (farm, block) pair: a {@code null} farm is a farm of its own. */
    private record BlockKey(Integer farmId, String blockCode) {
    }

    /**
     * The harvests of one variety row in the months.
     *
     * @param first   the first of them, which names the variety row
     * @param totalKg their exact sum
     * @param count   how many there are
     */
    private record Harvested(HarvestResponse first, BigDecimal totalKg, int count) {

        Harvested add(HarvestResponse harvest) {
            return new Harvested(first, totalKg.add(BigDecimal.valueOf(harvest.quantityKg())), count + 1);
        }
    }

    /** A quantity to 0.1 kg, halves up, as the forecast rounds it. */
    private static Double toTenth(BigDecimal kg) {
        return kg == null ? null : kg.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Actual yield × 100 / expected yield, to 0.1, computed from the rounded
     * values that are shown.
     *
     * @return the percentage, or {@code null} without harvest or when nothing is expected
     */
    private static Double achievedPct(BigDecimal expectedKg, BigDecimal actualKg) {
        if (actualKg == null || expectedKg.signum() == 0) {
            return null;
        }
        return actualKg.multiply(HUNDRED).divide(expectedKg, 1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * The harvests added up by variety row. Each quantity is turned into an
     * exact decimal first: a sum of doubles drifts.
     */
    private static Map<Long, Harvested> harvestedByVariety(List<HarvestResponse> harvests) {
        Map<Long, Harvested> harvested = new LinkedHashMap<>();
        for (HarvestResponse harvest : harvests) {
            harvested.merge(harvest.varietyId(),
                    new Harvested(harvest, BigDecimal.valueOf(harvest.quantityKg()), 1),
                    (sum, added) -> sum.add(added.first()));
        }
        return harvested;
    }

    /**
     * Compares the actual and the expected yield of the planted varieties,
     * optionally filtered, over whole months.
     *
     * <p>A missing farm means every farm, and a missing or blank block every
     * block, as for the other lists of the module. The months are the calendar
     * year of today when {@code from} is missing, and 12 when {@code months} is.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} for every block
     * @param from      first month, or {@code null} for January of the current year
     * @param months    number of months, from 1 to 24, or {@code null} for 12
     * @return the comparison
     */
    public YieldComparisonResponse getYieldComparison(Integer farmId, String blockCode, YearMonth from,
                                                      Integer months) {
        YearMonth firstMonth = from == null ? YearMonth.of(LocalDate.now(clock).getYear(), 1) : from;
        List<YearMonth> window = YieldForecastCalculator.window(firstMonth, months == null ? DEFAULT_MONTHS : months);
        LocalDate lastDay = window.getLast().atEndOfMonth();

        ForecastBasis basis = yieldForecastService.basis(farmId, blockCode);
        Map<Long, Harvested> harvested = harvestedByVariety(
                harvestService.getAllHarvests(farmId, blockCode, firstMonth.atDay(1), lastDay));

        List<VarietyComparison> varieties = basis.plantedVarieties().stream()
                .filter(planted -> !planted.plantingDate().isAfter(lastDay))
                .sorted(Comparator.comparing(PlantedVariety::variety, VARIETY_ORDER))
                .map(planted -> compare(planted, window, basis.sharesByPhase(),
                        harvested.get(planted.variety().getId())))
                .toList();

        return new YieldComparisonResponse(firstMonth, window.size(), YieldForecastService.RECORDED_PLANTINGS_BASIS,
                varieties, blocks(varieties), total(varieties), notCompared(basis, harvested, lastDay));
    }

    /**
     * Expected and actual yield of one planted variety row over the months.
     * The expected yield adds up the rounded expected yields of the months, as
     * the monthly totals of the forecast do.
     */
    private static VarietyComparison compare(PlantedVariety planted, List<YearMonth> window,
                                             Map<String, GrowthPhaseYieldShareResponse> sharesByPhase,
                                             Harvested harvested) {
        BigDecimal expectedKg = BigDecimal.ZERO;
        Map<String, PhaseShare> phases = new LinkedHashMap<>();
        for (YearMonth month : window) {
            Optional<MonthExpectation> expectation = YieldForecastService.expectationIn(month, planted, sharesByPhase);
            if (expectation.isEmpty()) {
                continue;
            }
            expectedKg = expectedKg.add(BigDecimal.valueOf(expectation.get().expectedKg()));
            if (expectation.get().monthShare() > 0) {
                GrowthPhaseYieldShareResponse share = expectation.get().phase().share();
                phases.putIfAbsent(share.growthPhase(),
                        new PhaseShare(share.growthPhase(), share.yieldShare(), share.source()));
            }
        }
        BigDecimal expected = expectedKg.setScale(1, RoundingMode.HALF_UP);
        BigDecimal actual = harvested == null ? null : harvested.totalKg().setScale(1, RoundingMode.HALF_UP);
        Variety variety = planted.variety();
        VarietyReference reference = planted.reference();
        return new VarietyComparison(
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                planted.treeCount(),
                expected.doubleValue(),
                toTenth(actual),
                harvested == null ? 0 : harvested.count(),
                achievedPct(expected, actual),
                reference.getYieldPerTreeKg(),
                reference.getYieldSource(),
                reference.getSeasonSource(),
                List.copyOf(phases.values()));
    }

    /** The lines added up by block, in the order of the lines. */
    private static List<BlockComparison> blocks(List<VarietyComparison> varieties) {
        Map<BlockKey, List<VarietyComparison>> byBlock = new LinkedHashMap<>();
        for (VarietyComparison line : varieties) {
            byBlock.computeIfAbsent(new BlockKey(line.farmId(), line.blockCode()), key -> new ArrayList<>()).add(line);
        }
        return byBlock.entrySet().stream()
                .map(block -> {
                    Comparison sum = total(block.getValue());
                    return new BlockComparison(block.getKey().farmId(), block.getKey().blockCode(),
                            sum.expectedKg(), sum.actualKg(), sum.achievedPct());
                })
                .toList();
    }

    /**
     * The lines added up. Their values have one decimal, so the sums are exact
     * and match what the lines show.
     */
    private static Comparison total(List<VarietyComparison> lines) {
        BigDecimal expectedKg = BigDecimal.ZERO.setScale(1);
        BigDecimal actualKg = null;
        for (VarietyComparison line : lines) {
            expectedKg = expectedKg.add(BigDecimal.valueOf(line.expectedKg()));
            if (line.actualKg() != null) {
                BigDecimal actual = BigDecimal.valueOf(line.actualKg());
                actualKg = actualKg == null ? actual : actualKg.add(actual);
            }
        }
        return new Comparison(toTenth(expectedKg), toTenth(actualKg), achievedPct(expectedKg, actualKg));
    }

    /**
     * The planted variety rows with no expected yield, so that none of their
     * harvests is left out: those absent from the agronomic reference, planted
     * by the end of the months, and the harvested rows the forecast leaves out
     * because no tree is left.
     */
    private static List<NotCompared> notCompared(ForecastBasis basis, Map<Long, Harvested> harvested,
                                                 LocalDate lastDay) {
        List<NotCompared> lines = new ArrayList<>();
        Set<Long> planted = new HashSet<>();
        basis.plantedVarieties().forEach(row -> planted.add(row.variety().getId()));
        for (PlantedVariety row : basis.plantedWithoutReference()) {
            planted.add(row.variety().getId());
            if (row.plantingDate().isAfter(lastDay)) {
                continue;
            }
            Variety variety = row.variety();
            Harvested harvests = harvested.get(variety.getId());
            lines.add(new NotCompared(variety.getFarmId(), variety.getBlockCode(), variety.getId(), variety.getName(),
                    row.treeCount(),
                    harvests == null ? null : toTenth(harvests.totalKg()),
                    harvests == null ? 0 : harvests.count(),
                    NO_REFERENCE));
        }
        for (Harvested harvests : harvested.values()) {
            HarvestResponse first = harvests.first();
            if (!planted.contains(first.varietyId())) {
                lines.add(new NotCompared(first.farmId(), first.blockCode(), first.varietyId(), first.varietyName(),
                        null, toTenth(harvests.totalKg()), harvests.count(), NO_TREE_LEFT));
            }
        }
        lines.sort(NOT_COMPARED_ORDER);
        return lines;
    }
}
