package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterLevelHistoryResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterLevelResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ConsumptionCriteria;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterLevelHistoryRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WaterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaterService.class);
    private final FarmRepository farmRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WaterQuotaService waterQuotaService;
    private final ZoneBlockResolver zoneBlockResolver;
    private final WaterIntegrationProperties properties;
    private final WaterLevelHistoryRepository levelHistoryRepository;

    public WaterService(
            FarmRepository farmRepository,
            WaterSourceRepository waterSourceRepository,
            WaterConsumptionRepository waterConsumptionRepository,
            WaterQualityTestRepository waterQualityTestRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            WaterQuotaService waterQuotaService,
            ZoneBlockResolver zoneBlockResolver,
            WaterIntegrationProperties properties,
            WaterLevelHistoryRepository levelHistoryRepository) {
        this.farmRepository = farmRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.waterQuotaService = waterQuotaService;
        this.zoneBlockResolver = zoneBlockResolver;
        this.properties = properties;
        this.levelHistoryRepository = levelHistoryRepository;
    }

    public List<WaterSourceResponse> findSources() {
        return waterSourceRepository.findAll().stream().map(WaterSourceResponse::from).toList();
    }

    public PageResponse<WaterSourceResponse> findSources(Pageable pageable, UUID farmId) {
        Page<WaterSource> page = farmId == null
                ? waterSourceRepository.findAll(pageable)
                : waterSourceRepository.findByFarmId(farmId, pageable);
        return toPageResponse(page.map(WaterSourceResponse::from));
    }

    @Transactional
    public WaterSourceResponse createSource(WaterSourceCreateRequest request) {
        requireFarm(request.farmId());
        WaterSource source = new WaterSource();
        source.setFarmId(request.farmId());
        source.setName(request.name());
        source.setType(request.type());
        source.setCapacityLiters(request.capacityLiters());
        source.setCurrentLevelLiters(request.currentLevelLiters());
        source.setLatitude(request.latitude());
        source.setLongitude(request.longitude());
        WaterSource saved = waterSourceRepository.save(source);
        recordLevel(saved, saved.getCurrentLevelLiters(), Instant.now());
        return WaterSourceResponse.from(saved);
    }

    public WaterSourceResponse getSource(UUID sourceId) {
        return WaterSourceResponse.from(getSourceEntity(sourceId));
    }

    /**
     * Level of a source, read in real time (specification module 5.2).
     *
     * <p>The displayed value is the last measurement sent by the reservoir level sensor;
     * the percentage and status are computed here so that the UI and the other modules
     * do not need to know the thresholds.</p>
     */
    public WaterLevelResponse sourceLevel(UUID sourceId) {
        WaterSource source = getSourceEntity(sourceId);
        Double percent = RainwaterTankRules.levelPercent(source);
        return new WaterLevelResponse(
                source.getId(),
                source.getName(),
                source.getType(),
                source.getCapacityLiters(),
                source.getCurrentLevelLiters(),
                percent == null ? null : Math.round(percent * 10d) / 10d,
                RainwaterTankRules.classify(percent),
                RainwaterTankRules.isRainwaterTank(source),
                Instant.now());
    }

    /**
     * Level history of a source (internal logic 5.2): every sensor reading and every manual level
     * change is kept, whereas {@code currentLevelLiters} only holds the last value.
     */
    public List<WaterLevelHistoryResponse> sourceLevelHistory(UUID sourceId, Instant from, Instant to) {
        requireSource(sourceId);
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("to must be after from");
        }
        return levelHistoryRepository.findBySourceIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(sourceId, start, end)
                .stream()
                .map(WaterLevelHistoryResponse::from)
                .toList();
    }

    /** Appends a level reading to the history (no-op when the level is unknown). */
    @Transactional
    public void recordLevel(WaterSource source, Double levelLiters, Instant measuredAt) {
        if (levelLiters == null) {
            return;
        }
        WaterLevelHistory history = new WaterLevelHistory();
        history.setSourceId(source.getId());
        history.setLevelLiters(levelLiters);
        history.setMeasuredAt(measuredAt == null ? Instant.now() : measuredAt);
        levelHistoryRepository.save(history);
    }

    @Transactional
    public WaterSourceResponse updateSource(UUID sourceId, WaterSourceUpdateRequest request) {
        WaterSource source = getSourceEntity(sourceId);
        if (request.farmId() != null && !request.farmId().equals(source.getFarmId())) {
            requireFarm(request.farmId());
            source.setFarmId(request.farmId());
        }
        source.setName(request.name() == null ? source.getName() : request.name());
        source.setType(request.type() == null ? source.getType() : request.type());
        source.setCapacityLiters(request.capacityLiters() == null ? source.getCapacityLiters() : request.capacityLiters());
        Double previousLevel = source.getCurrentLevelLiters();
        source.setCurrentLevelLiters(request.currentLevelLiters() == null ? source.getCurrentLevelLiters() : request.currentLevelLiters());
        source.setLatitude(request.latitude());
        source.setLongitude(request.longitude());
        WaterSource saved = waterSourceRepository.save(source);
        if (request.currentLevelLiters() != null && !request.currentLevelLiters().equals(previousLevel)) {
            recordLevel(saved, saved.getCurrentLevelLiters(), Instant.now());
        }
        return WaterSourceResponse.from(saved);
    }

    @Transactional
    public void deleteSource(UUID sourceId) {
        waterSourceRepository.delete(getSourceEntity(sourceId));
    }

    public List<WaterConsumptionResponse> findConsumptions() {
        return consumptionResponses(waterConsumptionRepository.findAll());
    }

    public PageResponse<WaterConsumptionResponse> findConsumptions(Pageable pageable, UUID farmId, UUID sourceId) {
        return findConsumptions(pageable, farmId, sourceId, null, null, null);
    }

    /**
     * Paginated consumption list; every filter is optional and they combine (AND). {@code blockCode}
     * is translated into the zones mapped to that Plants block (restricted to {@code farmId} if given).
     * Filters are applied in the database so that {@code totalElements} matches the filtered content.
     */
    public PageResponse<WaterConsumptionResponse> findConsumptions(Pageable pageable, UUID farmId, UUID sourceId,
                                                                   UUID zoneId, String lotId, String blockCode) {
        List<UUID> blockZones = blockCode == null || blockCode.isBlank()
                ? null : zoneBlockResolver.zoneIdsForBlock(farmId, blockCode);
        ConsumptionCriteria criteria = new ConsumptionCriteria(farmId, sourceId, zoneId, blockZones,
                normalizeLotId(lotId), null, null);
        Page<WaterConsumption> page = waterConsumptionRepository.findAll(criteria.toSpecification(), pageable);
        return toPageResponse(new PageImpl<>(consumptionResponses(page.getContent()), pageable, page.getTotalElements()));
    }

    @Transactional
    public WaterConsumptionResponse createConsumption(WaterConsumptionCreateRequest request) {
        WaterConsumption consumption = new WaterConsumption();
        consumption.setFarmId(request.farmId());
        consumption.setSourceId(request.sourceId());
        consumption.setConsumptionLiters(request.consumptionLiters());
        consumption.setConsumptionDate(request.consumptionDate());
        consumption.setIrrigationId(request.irrigationId());
        consumption.setZoneId(request.zoneId());
        consumption.setLotId(normalizeLotId(request.lotId()));
        consumption.setActivity(ConsumptionActivity.normalize(request.activity()));
        validateConsumptionReferences(consumption);
        WaterConsumption saved = waterConsumptionRepository.save(consumption);
        checkQuotaThresholds(consumption);
        return consumptionResponse(saved);
    }

    public WaterConsumptionResponse getConsumption(UUID consumptionId) {
        return consumptionResponse(getConsumptionEntity(consumptionId));
    }

    @Transactional
    public WaterConsumptionResponse updateConsumption(UUID consumptionId, WaterConsumptionUpdateRequest request) {
        WaterConsumption consumption = getConsumptionEntity(consumptionId);
        if (request.farmId() != null) {
            consumption.setFarmId(request.farmId());
        }
        if (request.sourceId() != null) {
            consumption.setSourceId(request.sourceId());
        }
        validateConsumptionReferences(consumption);
        consumption.setConsumptionLiters(request.consumptionLiters() == null ? consumption.getConsumptionLiters() : request.consumptionLiters());
        consumption.setConsumptionDate(request.consumptionDate() == null ? consumption.getConsumptionDate() : request.consumptionDate());
        consumption.setIrrigationId(request.irrigationId() == null ? consumption.getIrrigationId() : request.irrigationId());
        consumption.setZoneId(request.zoneId() == null ? consumption.getZoneId() : request.zoneId());
        if (request.lotId() != null) {
            consumption.setLotId(normalizeLotId(request.lotId()));
        }
        if (request.activity() != null) {
            consumption.setActivity(ConsumptionActivity.normalize(request.activity()));
        }
        WaterConsumption updated = waterConsumptionRepository.save(consumption);
        checkQuotaThresholds(consumption);
        return consumptionResponse(updated);
    }

    /**
     * Quota alerts (80 % warning, 100 % critical) evaluated after each consumption
     * measurement. Alert exceptions never propagate: a valid consumption must always be
     * recorded even if the notification fails.
     */
    private void checkQuotaThresholds(WaterConsumption consumption) {
        try {
            waterQuotaService.checkThresholds(
                    consumption.getFarmId(), consumption.getZoneId(), consumption.getConsumptionDate());
        } catch (RuntimeException ex) {
            // Logged silently: consumption tracking does not depend on alerts.
        }
    }

    @Transactional
    public void deleteConsumption(UUID consumptionId) {
        waterConsumptionRepository.delete(getConsumptionEntity(consumptionId));
    }

    public List<WaterQualityResponse> findQualityTests() {
        return waterQualityTestRepository.findAll().stream().map(WaterQualityResponse::from).toList();
    }

    public PageResponse<WaterQualityResponse> findQualityTests(Pageable pageable, UUID sourceId) {
        Page<WaterQualityTest> page = sourceId == null
                ? waterQualityTestRepository.findAll(pageable)
                : waterQualityTestRepository.findBySourceId(sourceId, pageable);
        return toPageResponse(page.map(WaterQualityResponse::from));
    }

    @Transactional
    public WaterQualityResponse createQualityTest(WaterQualityCreateRequest request) {
        WaterQualityTest test = new WaterQualityTest();
        test.setSourceId(request.sourceId());
        test.setPh(request.ph());
        test.setTemperatureCelsius(request.temperatureCelsius());
        test.setTurbidityNtu(request.turbidityNtu());
        test.setConductivityUsCm(request.conductivityUsCm());
        test.setSalinityPpt(request.salinityPpt());
        test.setTestDate(request.testDate());
        requireSource(test.getSourceId());
        WaterQualityTest saved = waterQualityTestRepository.save(test);
        checkQualityAndAlert(saved);
        return WaterQualityResponse.from(saved);
    }

    public WaterQualityResponse getQualityTest(UUID qualityTestId) {
        return WaterQualityResponse.from(getQualityTestEntity(qualityTestId));
    }

    @Transactional
    public WaterQualityResponse updateQualityTest(UUID qualityTestId, WaterQualityUpdateRequest request) {
        WaterQualityTest test = getQualityTestEntity(qualityTestId);
        if (request.sourceId() != null) {
            requireSource(request.sourceId());
            test.setSourceId(request.sourceId());
        }
        test.setPh(request.ph());
        test.setTemperatureCelsius(request.temperatureCelsius());
        test.setTurbidityNtu(request.turbidityNtu());
        test.setConductivityUsCm(request.conductivityUsCm());
        test.setSalinityPpt(request.salinityPpt());
        test.setTestDate(request.testDate() == null ? test.getTestDate() : request.testDate());
        WaterQualityTest updated = waterQualityTestRepository.save(test);
        checkQualityAndAlert(updated);
        return WaterQualityResponse.from(updated);
    }

    @Transactional
    public void deleteQualityTest(UUID qualityTestId) {
        waterQualityTestRepository.delete(getQualityTestEntity(qualityTestId));
    }

    private void validateConsumptionReferences(WaterConsumption consumption) {
        requireFarm(consumption.getFarmId());
        WaterSource source = requireSource(consumption.getSourceId());
        if (!source.getFarmId().equals(consumption.getFarmId())) {
            throw new IllegalArgumentException("Water source does not belong to the selected farm");
        }
    }

    private void requireFarm(UUID farmId) {
        if (farmId == null || farmRepository.findById(farmId).isEmpty()) {
            throw new NotFoundException("Farm");
        }
    }

    private WaterSource requireSource(UUID sourceId) {
        if (sourceId == null) {
            throw new NotFoundException("WaterSource");
        }
        return getSourceEntity(sourceId);
    }

    private WaterSource getSourceEntity(UUID sourceId) {
        return waterSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("WaterSource"));
    }

    private WaterConsumption getConsumptionEntity(UUID consumptionId) {
        return waterConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new NotFoundException("WaterConsumption"));
    }

    private WaterQualityTest getQualityTestEntity(UUID qualityTestId) {
        return waterQualityTestRepository.findById(qualityTestId)
                .orElseThrow(() -> new NotFoundException("WaterQualityTest"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private void checkQualityAndAlert(WaterQualityTest test) {
        boolean phOutOfRange = test.getPh() != null
                && (test.getPh() < properties.qualityPhMin || test.getPh() > properties.qualityPhMax);
        boolean turbidityHigh = test.getTurbidityNtu() != null
                && test.getTurbidityNtu() > properties.qualityTurbidityMaxNtu;
        if (!phOutOfRange && !turbidityHigh) {
            return;
        }

        UUID recipient = userRepository.findAll().stream().map(user -> user.getId()).findFirst().orElse(null);
        if (recipient == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(recipient);
        notification.setTitle("Non-compliant water");
        notification.setMessage(buildQualityMessage(test, phOutOfRange, turbidityHigh));
        notification.setType("warning");
        notification.setRead(false);
        notification.setActionUrl("/api/water/quality/" + test.getId());
        notificationRepository.save(notification);
    }

    private String buildQualityMessage(WaterQualityTest test, boolean phOut, boolean turbidityHigh) {
        StringBuilder message = new StringBuilder("Out-of-range quality test on source ")
                .append(test.getSourceId()).append(": ");
        if (phOut) {
            message.append("pH=").append(test.getPh()).append(" (outside ")
                    .append(properties.qualityPhMin).append('-').append(properties.qualityPhMax).append(") ");
        }
        if (turbidityHigh) {
            message.append("turbidity=").append(test.getTurbidityNtu()).append(" NTU (> ")
                    .append(properties.qualityTurbidityMaxNtu).append(") ");
        }
        return message.toString().trim();
    }

    private WaterConsumptionResponse consumptionResponse(WaterConsumption consumption) {
        return consumptionResponse(consumption, zoneBlockResolver.blockCodesForZone(consumption.getZoneId()));
    }

    /** Batch variant: one mapping lookup for the whole list instead of one per row. */
    private List<WaterConsumptionResponse> consumptionResponses(List<WaterConsumption> consumptions) {
        Map<UUID, List<String>> blocks = zoneBlockResolver.blockCodesForZones(
                consumptions.stream().map(WaterConsumption::getZoneId).toList());
        return consumptions.stream()
                .map(c -> consumptionResponse(c, c.getZoneId() == null ? List.of() : blocks.getOrDefault(c.getZoneId(), List.of())))
                .toList();
    }

    /**
     * {@code blockCode} is only set when the zone maps to exactly one block; with several blocks,
     * {@code multipleBlockCodes=true} tells Plants the volume cannot be attributed to one block.
     */
    private static WaterConsumptionResponse consumptionResponse(WaterConsumption consumption, List<String> blocks) {
        return WaterConsumptionResponse.from(consumption, blocks.size() == 1 ? blocks.get(0) : null, blocks.size() > 1);
    }

    static String normalizeLotId(String lotId) {
        return lotId == null || lotId.isBlank() ? null : lotId.trim();
    }
}
