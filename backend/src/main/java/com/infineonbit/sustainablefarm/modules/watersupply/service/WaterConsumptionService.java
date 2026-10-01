package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WaterConsumptionService {
    private final FarmRepository farmRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final WaterConsumptionRepository waterConsumptionRepository;

    public WaterConsumptionService(
            FarmRepository farmRepository,
            WaterSourceRepository waterSourceRepository,
            WaterConsumptionRepository waterConsumptionRepository) {
        this.farmRepository = farmRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
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
        consumption.setZoneId(request.zoneId());
        validateReferences(consumption);
        return WaterConsumptionResponse.from(waterConsumptionRepository.save(consumption));
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
        validateReferences(consumption);
        consumption.setConsumptionLiters(request.consumptionLiters() == null
                ? consumption.getConsumptionLiters() : request.consumptionLiters());
        consumption.setConsumptionDate(request.consumptionDate() == null
                ? consumption.getConsumptionDate() : request.consumptionDate());
        consumption.setIrrigationId(request.irrigationId() == null
                ? consumption.getIrrigationId() : request.irrigationId());
        consumption.setZoneId(request.zoneId() == null ? consumption.getZoneId() : request.zoneId());
        return WaterConsumptionResponse.from(waterConsumptionRepository.save(consumption));
    }

    @Transactional
    public void deleteConsumption(UUID consumptionId) {
        waterConsumptionRepository.delete(getConsumptionEntity(consumptionId));
    }

    private void validateReferences(WaterConsumption consumption) {
        if (consumption.getFarmId() == null || farmRepository.findById(consumption.getFarmId()).isEmpty()) {
            throw new ResourceNotFoundException("Farm not found");
        }
        WaterSource source = waterSourceRepository.findById(consumption.getSourceId())
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource not found"));
        if (!source.getFarmId().equals(consumption.getFarmId())) {
            throw new BusinessRuleException("Water source does not belong to the selected farm");
        }
    }

    private WaterConsumption getConsumptionEntity(UUID consumptionId) {
        return waterConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterConsumption not found"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}