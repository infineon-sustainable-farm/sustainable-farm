package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WaterSourceService {
    private final FarmRepository farmRepository;
    private final WaterSourceRepository waterSourceRepository;

    public WaterSourceService(FarmRepository farmRepository, WaterSourceRepository waterSourceRepository) {
        this.farmRepository = farmRepository;
        this.waterSourceRepository = waterSourceRepository;
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
        source.setCapacityLiters(request.capacityLiters() == null
                ? source.getCapacityLiters() : request.capacityLiters());
        source.setCurrentLevelLiters(request.currentLevelLiters() == null
                ? source.getCurrentLevelLiters() : request.currentLevelLiters());
        source.setLatitude(request.latitude());
        source.setLongitude(request.longitude());
        return WaterSourceResponse.from(waterSourceRepository.save(source));
    }

    @Transactional
    public void deleteSource(UUID sourceId) {
        waterSourceRepository.delete(getSourceEntity(sourceId));
    }

    private void requireFarm(UUID farmId) {
        if (farmId == null || farmRepository.findById(farmId).isEmpty()) {
            throw new ResourceNotFoundException("Farm not found");
        }
    }

    private WaterSource getSourceEntity(UUID sourceId) {
        return waterSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource not found"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}