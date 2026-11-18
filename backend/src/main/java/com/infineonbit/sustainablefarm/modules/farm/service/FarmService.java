package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.entity.Zone;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Farms and zones of the site referential, served under {@code /api/farms} and {@code /api/zones}. */
@Service
@Transactional(readOnly = true)
public class FarmService {
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final FarmZoneRepository zoneRepository;

    public FarmService(FarmRepository farmRepository, BlockRepository blockRepository, FarmZoneRepository zoneRepository) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.zoneRepository = zoneRepository;
    }

    public List<FarmResponse> findFarms() {
        return farmRepository.findAll().stream().map(FarmResponse::from).toList();
    }

    public PageResponse<FarmResponse> findFarms(Pageable pageable) {
        return toPageResponse(farmRepository.findAll(pageable).map(FarmResponse::from));
    }

    @Transactional
    public FarmResponse createFarm(FarmCreateRequest request) {
        Farm farm = new Farm();
        farm.setName(request.name());
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
        return FarmResponse.from(farmRepository.save(farm));
    }

    public FarmResponse getFarm(Long farmId) {
        return FarmResponse.from(getFarmEntity(farmId));
    }

    @Transactional
    public FarmResponse updateFarm(Long farmId, FarmUpdateRequest request) {
        Farm farm = getFarmEntity(farmId);
        if (request.name() != null) farm.setName(request.name());
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
        return FarmResponse.from(farmRepository.save(farm));
    }

    /** A farm still referenced (blocks, sources, consumption...) is refused with 409 by the foreign keys. */
    @Transactional
    public void deleteFarm(Long farmId) {
        farmRepository.delete(getFarmEntity(farmId));
        farmRepository.flush();
    }

    public List<ZoneResponse> findFieldZones(Long fieldId) {
        getBlockEntity(fieldId);
        return zoneRepository.findByBlockId(fieldId).stream().map(FarmService::toResponse).toList();
    }

    public PageResponse<ZoneResponse> findFieldZones(Long fieldId, Pageable pageable) {
        getBlockEntity(fieldId);
        return toPageResponse(zoneRepository.findByBlockId(fieldId, pageable).map(FarmService::toResponse));
    }

    public List<ZoneResponse> findZones() {
        return zoneRepository.findAll().stream().map(FarmService::toResponse).toList();
    }

    public PageResponse<ZoneResponse> findZones(Pageable pageable) {
        return toPageResponse(zoneRepository.findAll(pageable).map(FarmService::toResponse));
    }

    @Transactional
    public ZoneResponse createZone(ZoneCreateRequest request) {
        Zone zone = new Zone();
        zone.setBlock(getBlockEntity(request.fieldId()));
        zone.setName(request.name());
        zone.setAreaHectares(request.areaHectares());
        zone.setIrrigationMethod(request.irrigationMethod());
        zone.setCropCoefficient(request.cropCoefficient());
        zone.setEmitterCount(request.emitterCount());
        zone.setEmitterNominalFlowLh(request.emitterNominalFlowLh());
        return toResponse(zoneRepository.save(zone));
    }

    public ZoneResponse getZone(Long zoneId) {
        return toResponse(getZoneEntity(zoneId));
    }

    @Transactional
    public ZoneResponse updateZone(Long zoneId, ZoneUpdateRequest request) {
        Zone zone = getZoneEntity(zoneId);
        if (request.fieldId() != null && !request.fieldId().equals(zone.getBlock().getId())) {
            zone.setBlock(getBlockEntity(request.fieldId()));
        }
        zone.setName(request.name() == null ? zone.getName() : request.name());
        zone.setAreaHectares(request.areaHectares() == null ? zone.getAreaHectares() : request.areaHectares());
        // An absent field must not erase the existing value (same rule as the other fields).
        zone.setIrrigationMethod(request.irrigationMethod() == null ? zone.getIrrigationMethod() : request.irrigationMethod());
        zone.setCropCoefficient(request.cropCoefficient() == null ? zone.getCropCoefficient() : request.cropCoefficient());
        zone.setEmitterCount(request.emitterCount() == null ? zone.getEmitterCount() : request.emitterCount());
        zone.setEmitterNominalFlowLh(request.emitterNominalFlowLh() == null
                ? zone.getEmitterNominalFlowLh() : request.emitterNominalFlowLh());
        return toResponse(zoneRepository.save(zone));
    }

    /** A zone still referenced (schedules, consumption...) is refused with 409 by the foreign keys. */
    @Transactional
    public void deleteZone(Long zoneId) {
        zoneRepository.delete(getZoneEntity(zoneId));
        zoneRepository.flush();
    }

    private static ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.from(FarmMapper.toDto(zone));
    }

    private Farm getFarmEntity(Long farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
    }

    private Block getBlockEntity(Long blockId) {
        return blockRepository.findById(blockId).orElseThrow(() -> new ResourceNotFoundException("Field not found"));
    }

    private Zone getZoneEntity(Long zoneId) {
        return zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }

    static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
