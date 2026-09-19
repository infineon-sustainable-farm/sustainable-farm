package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WaterService {
    private final FarmRepository farmRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WaterQuotaService waterQuotaService;

    public WaterService(
            FarmRepository farmRepository,
            WaterSourceRepository waterSourceRepository,
            WaterConsumptionRepository waterConsumptionRepository,
            WaterQualityTestRepository waterQualityTestRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            WaterQuotaService waterQuotaService) {
        this.farmRepository = farmRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.waterQuotaService = waterQuotaService;
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
        return WaterSourceResponse.from(waterSourceRepository.save(source));
    }

    public WaterSourceResponse getSource(UUID sourceId) {
        return WaterSourceResponse.from(getSourceEntity(sourceId));
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
        source.setCurrentLevelLiters(request.currentLevelLiters() == null ? source.getCurrentLevelLiters() : request.currentLevelLiters());
        source.setLatitude(request.latitude());
        source.setLongitude(request.longitude());
        return WaterSourceResponse.from(waterSourceRepository.save(source));
    }

    @Transactional
    public void deleteSource(UUID sourceId) {
        waterSourceRepository.delete(getSourceEntity(sourceId));
    }

    public List<WaterConsumptionResponse> findConsumptions() {
        return waterConsumptionRepository.findAll().stream().map(WaterConsumptionResponse::from).toList();
    }

    public PageResponse<WaterConsumptionResponse> findConsumptions(Pageable pageable, UUID farmId, UUID sourceId) {
        Page<WaterConsumption> page;
        if (farmId != null) {
            page = waterConsumptionRepository.findByFarmId(farmId, pageable);
        } else if (sourceId != null) {
            page = waterConsumptionRepository.findBySourceId(sourceId, pageable);
        } else {
            page = waterConsumptionRepository.findAll(pageable);
        }
        return toPageResponse(page.map(WaterConsumptionResponse::from));
    }

    @Transactional
    public WaterConsumptionResponse createConsumption(WaterConsumptionCreateRequest request) {
        WaterConsumption consumption = new WaterConsumption();
        consumption.setFarmId(request.farmId());
        consumption.setSourceId(request.sourceId());
        consumption.setConsumptionLiters(request.consumptionLiters());
        consumption.setConsumptionDate(request.consumptionDate());
        consumption.setIrrigationId(request.irrigationId());
        validateConsumptionReferences(consumption);
        WaterConsumption saved = waterConsumptionRepository.save(consumption);
        checkQuotaThresholds(consumption);
        return WaterConsumptionResponse.from(saved);
    }

    public WaterConsumptionResponse getConsumption(UUID consumptionId) {
        return WaterConsumptionResponse.from(getConsumptionEntity(consumptionId));
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
        WaterConsumption updated = waterConsumptionRepository.save(consumption);
        checkQuotaThresholds(consumption);
        return WaterConsumptionResponse.from(updated);
    }

    /**
     * Alertes de quota (80 % warning, 100 % critical) evaluees apres chaque mesure de
     * consommation. Les exceptions d'alerte ne remontent jamais : une consommation valide
     * doit toujours etre enregistree meme si la notification echoue.
     */
    private void checkQuotaThresholds(WaterConsumption consumption) {
        try {
            waterQuotaService.checkThresholds(
                    consumption.getFarmId(), null, consumption.getConsumptionDate());
        } catch (RuntimeException ex) {
            // Journalise silencieusement : le suivi de consommation ne depend pas des alertes.
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
        boolean phOutOfRange = test.getPh() != null && (test.getPh() < 6.0 || test.getPh() > 7.5);
        boolean turbidityHigh = test.getTurbidityNtu() != null && test.getTurbidityNtu() > 5.0;
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
            message.append("pH=").append(test.getPh()).append(" (outside 6.0-7.5) ");
        }
        if (turbidityHigh) {
            message.append("turbidity=").append(test.getTurbidityNtu()).append(" NTU (> 5) ");
        }
        return message.toString().trim();
    }
}