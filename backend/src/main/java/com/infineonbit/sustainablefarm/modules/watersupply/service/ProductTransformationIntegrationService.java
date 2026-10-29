package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.AvailabilityForecastRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.LotUsageRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ConsumptionCriteria;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Transformation integration (T1-T4): water consumed per lot and activity, washing water
 * quality for a lot, and availability forecast for planned batches.
 *
 * <p>Flow meters know nothing about lots: Product Transformation sends the time window of a lot
 * (and ideally its source/zone) and the measured consumptions are attributed to it afterwards. A
 * consumption already attributed to another lot is never taken over: the overlap is reported.</p>
 */
@Service
@Transactional(readOnly = true)
public class ProductTransformationIntegrationService {
    private static final int DEFAULT_HORIZON_DAYS = 7;

    private final WaterConsumptionRepository consumptionRepository;
    private final WaterQualityTestRepository qualityRepository;
    private final WaterSourceRepository sourceRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final AlertService alertService;
    private final WaterIntegrationProperties properties;

    public ProductTransformationIntegrationService(WaterConsumptionRepository consumptionRepository,
                                                   WaterQualityTestRepository qualityRepository,
                                                   WaterSourceRepository sourceRepository,
                                                   IrrigationScheduleRepository scheduleRepository,
                                                   AlertService alertService,
                                                   WaterIntegrationProperties properties) {
        this.consumptionRepository = consumptionRepository;
        this.qualityRepository = qualityRepository;
        this.sourceRepository = sourceRepository;
        this.scheduleRepository = scheduleRepository;
        this.alertService = alertService;
        this.properties = properties;
    }

    /** Attributes the consumptions of a time window to a lot (T1). Re-sending the same window is harmless. */
    @Transactional
    public Map<String, Object> attachLotUsage(LotUsageRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        String lotId = request.lotId().trim();
        String activity = ConsumptionActivity.normalize(request.activity());
        List<WaterConsumption> inWindow = consumptionRepository.findAll(
                ConsumptionCriteria.window(request.sourceId(), request.zoneId(), request.startAt(), request.endAt())
                        .toSpecification(), Sort.by("consumptionDate"));
        List<WaterConsumption> attached = new ArrayList<>();
        int otherLot = 0;
        for (WaterConsumption item : inWindow) {
            if (item.getLotId() != null && !item.getLotId().equals(lotId)) {
                otherLot++;
                continue;
            }
            item.setLotId(lotId);
            if (activity != null) {
                item.setActivity(activity);
            }
            attached.add(item);
        }
        consumptionRepository.saveAll(attached);

        List<String> warnings = new ArrayList<>();
        if (inWindow.isEmpty()) {
            warnings.add("no_consumption_found");
        }
        if (otherLot > 0) {
            warnings.add("window_overlaps_other_lot");
        }
        if (request.sourceId() == null && request.zoneId() == null) {
            warnings.add("no_source_or_zone_filter");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("lotId", lotId);
        response.put("activity", activity);
        response.put("startAt", request.startAt());
        response.put("endAt", request.endAt());
        response.put("attachedCount", attached.size());
        response.put("attachedLiters", liters(attached));
        response.put("skippedOtherLotCount", otherLot);
        response.put("warnings", warnings);
        return response;
    }

    /** Volume consumed by a lot, per activity (T1). An unknown lot answers with zero, not 404. */
    public Map<String, Object> byLot(String lotId) {
        String lot = requireLot(lotId);
        List<WaterConsumption> items = consumptionRepository.findByLotIdOrderByConsumptionDateAsc(lot);
        Map<String, Double> byActivity = new LinkedHashMap<>();
        for (WaterConsumption item : items) {
            byActivity.merge(item.getActivity() == null ? ConsumptionActivity.OTHER.name() : item.getActivity(),
                    item.getConsumptionLiters() == null ? 0d : item.getConsumptionLiters(), Double::sum);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("lotId", lot);
        response.put("totalLiters", liters(items));
        response.put("byActivity", byActivity);
        response.put("firstConsumptionAt", items.isEmpty() ? null : items.get(0).getConsumptionDate());
        response.put("lastConsumptionAt", items.isEmpty() ? null : items.get(items.size() - 1).getConsumptionDate());
        response.put("items", items.stream().map(this::consumptionItem).toList());
        return response;
    }

    /**
     * Quality of the water used by a lot (T2), source by source.
     *
     * <p>Window: the given one or, by default, the lot's first-to-last consumption. Each source that
     * supplied the lot needs its own test: a change of source therefore needs a test of the new
     * source, whatever the time elapsed. For a source, the decisive tests are the last one made in
     * the {@code app.water.quality.test-validity-days} before the window, and every test made during
     * it. A source without decisive test is {@code no_test}, and so is the lot: no proof is not a
     * proof of compliance.</p>
     */
    public Map<String, Object> qualityCompliance(String lotId, Instant from, Instant to) {
        String lot = lotId == null || lotId.isBlank() ? null : lotId.trim();
        List<WaterConsumption> lotItems = lot == null ? List.of() : consumptionRepository.findByLotIdOrderByConsumptionDateAsc(lot);
        Instant start = from != null ? from : lotItems.stream().map(WaterConsumption::getConsumptionDate)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        Instant end = to != null ? to : lotItems.stream().map(WaterConsumption::getConsumptionDate)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        if (start == null || end == null) {
            throw new IllegalArgumentException("from and to are required when the lot has no recorded consumption");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("to must be after from");
        }
        Instant validFrom = start.minus(properties.qualityTestValidityDays, ChronoUnit.DAYS);
        List<UUID> lotSources = lotItems.stream().map(WaterConsumption::getSourceId).filter(Objects::nonNull).distinct().toList();
        List<WaterQualityTest> candidates = lotSources.isEmpty()
                ? qualityRepository.findBetween(validFrom, end)
                : qualityRepository.findBySourcesBetween(lotSources, validFrom, end);
        // Without a lot, the sources checked are those that have a test in the period.
        List<UUID> sourceIds = lotSources.isEmpty()
                ? candidates.stream().map(WaterQualityTest::getSourceId).filter(Objects::nonNull).distinct().toList()
                : lotSources;

        List<Map<String, Object>> sources = new ArrayList<>();
        List<WaterQualityTest> tests = new ArrayList<>();
        for (UUID sourceId : sourceIds) {
            List<WaterQualityTest> decisive = decisiveTests(candidates, sourceId, start);
            tests.addAll(decisive);
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("sourceId", sourceId);
            source.put("status", verdict(decisive));
            source.put("testCount", decisive.size());
            source.put("lastTestDate", decisive.isEmpty() ? null : decisive.get(decisive.size() - 1).getTestDate());
            sources.add(source);
        }
        tests.sort(Comparator.comparing(WaterQualityTest::getTestDate, Comparator.nullsFirst(Comparator.naturalOrder())));
        List<String> verdicts = sources.stream().map(source -> (String) source.get("status")).toList();
        String status = verdicts.contains("non_compliant") ? "non_compliant"
                : verdicts.isEmpty() || verdicts.contains("no_test") ? "no_test" : "compliant";

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("phMin", properties.qualityPhMin);
        rule.put("phMax", properties.qualityPhMax);
        rule.put("turbidityMaxNtu", properties.qualityTurbidityMaxNtu);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("lotId", lot);
        response.put("from", start);
        response.put("to", end);
        response.put("validityDays", properties.qualityTestValidityDays);
        response.put("sourceIds", sourceIds);
        response.put("status", status);
        response.put("compliant", "compliant".equals(status));
        response.put("rule", rule);
        response.put("sources", sources);
        response.put("tests", tests.stream().map(this::testItem).toList());
        return response;
    }

    /** Last test of the source before the window (within the validity period, already filtered) + tests during it. */
    private static List<WaterQualityTest> decisiveTests(List<WaterQualityTest> candidates, UUID sourceId, Instant start) {
        List<WaterQualityTest> ofSource = candidates.stream()
                .filter(test -> sourceId.equals(test.getSourceId()) && test.getTestDate() != null)
                .sorted(Comparator.comparing(WaterQualityTest::getTestDate))
                .toList();
        List<WaterQualityTest> decisive = new ArrayList<>();
        ofSource.stream().filter(test -> test.getTestDate().isBefore(start)).reduce((first, second) -> second)
                .ifPresent(decisive::add);
        ofSource.stream().filter(test -> !test.getTestDate().isBefore(start)).forEach(decisive::add);
        return decisive;
    }

    private String verdict(List<WaterQualityTest> decisive) {
        if (decisive.isEmpty()) {
            return "no_test";
        }
        return decisive.stream().anyMatch(this::nonCompliant) ? "non_compliant" : "compliant";
    }

    /**
     * Availability forecast of a source (T3) and buffer check (T4): current level minus the demand of
     * the planned batches and, by default, of the scheduled irrigations of the source's farm over the
     * horizon. A tight or insufficient buffer raises a warning alert for the water team (hence the
     * read-write transaction: a read-only one would never flush the notification).
     */
    @Transactional
    public Map<String, Object> forecast(AvailabilityForecastRequest request) {
        WaterSource source = sourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new NotFoundException("WaterSource"));
        int horizon = request.horizonDays() == null ? DEFAULT_HORIZON_DAYS : request.horizonDays();
        Instant now = Instant.now();
        Instant end = now.plus(horizon, ChronoUnit.DAYS);

        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> batches = new ArrayList<>();
        double batchDemand = 0d;
        for (AvailabilityForecastRequest.Batch batch : request.batches() == null ? List.<AvailabilityForecastRequest.Batch>of() : request.batches()) {
            String activity = ConsumptionActivity.normalize(batch.activity());
            boolean inHorizon = batch.plannedAt() == null || (!batch.plannedAt().isBefore(now) && batch.plannedAt().isBefore(end));
            double demand = inHorizon ? batchDemand(activity, batch.sizeKg(), warnings) : 0d;
            batchDemand += demand;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lotId", batch.lotId());
            item.put("activity", activity);
            item.put("plannedAt", batch.plannedAt());
            item.put("demandLiters", Math.round(demand));
            item.put("inHorizon", inHorizon);
            batches.add(item);
        }
        boolean includeIrrigation = request.includeIrrigation() == null || request.includeIrrigation();
        double irrigationDemand = includeIrrigation && source.getFarmId() != null
                ? scheduleRepository.sumPlannedDemandForFarmBetween(source.getFarmId(), now, end) : 0d;
        double forecastDemand = batchDemand + irrigationDemand;
        double current = source.getCurrentLevelLiters() == null ? 0d : source.getCurrentLevelLiters();
        double available = current - forecastDemand;
        String status = available < 0 ? "insufficient" : available < properties.ptBufferMinLiters ? "tight" : "ok";
        if (!"ok".equals(status)) {
            alertService.raiseOnce("warning", "Water buffer " + status + " - " + source.getName(),
                    "Forecast demand of " + Math.round(forecastDemand) + " L over " + horizon + " day(s) leaves "
                            + Math.round(available) + " L in " + source.getName() + " (minimum buffer "
                            + Math.round(properties.ptBufferMinLiters) + " L).",
                    "/watersupply/sources");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceId", source.getId());
        response.put("horizonDays", horizon);
        response.put("currentLevelLiters", current);
        response.put("batchDemandLiters", Math.round(batchDemand));
        response.put("irrigationDemandLiters", Math.round(irrigationDemand));
        response.put("forecastDemandLiters", Math.round(forecastDemand));
        response.put("availableAfterForecastLiters", Math.round(available));
        response.put("bufferMinLiters", properties.ptBufferMinLiters);
        response.put("bufferStatus", status);
        response.put("batches", batches);
        response.put("warnings", warnings.stream().distinct().toList());
        response.put("parametersStatus", "unvalidated_defaults");
        return response;
    }

    private double batchDemand(String activity, Double sizeKg, List<String> warnings) {
        double size = sizeKg == null ? 0d : sizeKg;
        if (activity == null) {
            return 0d;
        }
        return switch (ConsumptionActivity.valueOf(activity)) {
            case WASHING -> size * properties.ptWashingLitersPerKg;
            case CUTTING -> size * properties.ptCuttingLitersPerKg;
            case DRYER_CLEANING -> properties.ptDryerCleaningM3PerLot * 1000d;
            case IRRIGATION, OTHER -> {
                warnings.add("no_coefficient_for_activity_" + activity.toLowerCase(java.util.Locale.ROOT));
                yield 0d;
            }
        };
    }

    private boolean nonCompliant(WaterQualityTest test) {
        return (test.getPh() != null && (test.getPh() < properties.qualityPhMin || test.getPh() > properties.qualityPhMax))
                || (test.getTurbidityNtu() != null && test.getTurbidityNtu() > properties.qualityTurbidityMaxNtu);
    }

    private Map<String, Object> consumptionItem(WaterConsumption item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("consumptionId", item.getId());
        map.put("activity", item.getActivity() == null ? ConsumptionActivity.OTHER.name() : item.getActivity());
        map.put("sourceId", item.getSourceId());
        map.put("zoneId", item.getZoneId());
        map.put("liters", item.getConsumptionLiters());
        map.put("consumptionDate", item.getConsumptionDate());
        return map;
    }

    private Map<String, Object> testItem(WaterQualityTest test) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("qualityTestId", test.getId());
        map.put("sourceId", test.getSourceId());
        map.put("ph", test.getPh());
        map.put("turbidityNtu", test.getTurbidityNtu());
        map.put("status", nonCompliant(test) ? "non_compliant" : "compliant");
        map.put("testDate", test.getTestDate());
        return map;
    }

    private static double liters(List<WaterConsumption> items) {
        return items.stream().map(WaterConsumption::getConsumptionLiters).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
    }

    private static String requireLot(String lotId) {
        if (lotId == null || lotId.isBlank()) {
            throw new IllegalArgumentException("lotId must not be blank");
        }
        return lotId.trim();
    }
}
