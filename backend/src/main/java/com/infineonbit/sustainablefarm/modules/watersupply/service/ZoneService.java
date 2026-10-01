package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ZoneService {
    private final FieldRepository fieldRepository;
    private final FieldZoneRepository fieldZoneRepository;

    public ZoneService(FieldRepository fieldRepository, FieldZoneRepository fieldZoneRepository) {
        this.fieldRepository = fieldRepository;
        this.fieldZoneRepository = fieldZoneRepository;
    }

    public List<ZoneResponse> findFieldZones(UUID fieldId) {
        getFieldEntity(fieldId);
        return fieldZoneRepository.findByFieldId(fieldId).stream().map(ZoneResponse::from).toList();
    }

    public PageResponse<ZoneResponse> findFieldZones(UUID fieldId, Pageable pageable) {
        getFieldEntity(fieldId);
        return toPageResponse(fieldZoneRepository.findByFieldId(fieldId, pageable).map(ZoneResponse::from));
    }

    public List<ZoneResponse> findZones() {
        return fieldZoneRepository.findAll().stream().map(ZoneResponse::from).toList();
    }

    public PageResponse<ZoneResponse> findZones(Pageable pageable) {
        return toPageResponse(fieldZoneRepository.findAll(pageable).map(ZoneResponse::from));
    }

    public ZoneResponse getZone(UUID zoneId) {
        return ZoneResponse.from(getZoneEntity(zoneId));
    }

    @Transactional
    public ZoneResponse updateZone(UUID zoneId, ZoneUpdateRequest request) {
        Zone zone = getZoneEntity(zoneId);
        if (request.fieldId() != null && !request.fieldId().equals(zone.getFieldId())) {
            getFieldEntity(request.fieldId());
            zone.setFieldId(request.fieldId());
        }
        zone.setName(request.name() == null ? zone.getName() : request.name());
        zone.setAreaHectares(request.areaHectares() == null ? zone.getAreaHectares() : request.areaHectares());
        zone.setIrrigationMethod(request.irrigationMethod() == null
                ? zone.getIrrigationMethod() : request.irrigationMethod());
        return ZoneResponse.from(fieldZoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(UUID zoneId) {
        fieldZoneRepository.delete(getZoneEntity(zoneId));
    }

    @Transactional
    public ZoneResponse createZone(ZoneCreateRequest request) {
        getFieldEntity(request.fieldId());
        Zone zone = new Zone();
        zone.setFieldId(request.fieldId());
        zone.setName(request.name());
        zone.setAreaHectares(request.areaHectares());
        zone.setIrrigationMethod(request.irrigationMethod());
        return ZoneResponse.from(fieldZoneRepository.save(zone));
    }

    private Field getFieldEntity(UUID fieldId) {
        return fieldRepository.findById(fieldId).orElseThrow(() -> new ResourceNotFoundException("Field not found"));
    }

    private Zone getZoneEntity(UUID zoneId) {
        return fieldZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}