package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FarmService {
    private final FarmRepository farmRepository;
    private final FieldRepository fieldRepository;
    private final ZoneRepository zoneRepository;

    public FarmService(FarmRepository farmRepository, FieldRepository fieldRepository, ZoneRepository zoneRepository) {
        this.farmRepository = farmRepository;
        this.fieldRepository = fieldRepository;
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
        apply(farm, request);
        return FarmResponse.from(farmRepository.save(farm));
    }

    public FarmResponse getFarm(UUID farmId) {
        return FarmResponse.from(getFarmEntity(farmId));
    }

    @Transactional
    public FarmResponse updateFarm(UUID farmId, FarmUpdateRequest request) {
        Farm farm = getFarmEntity(farmId);
        if (request.name() != null) farm.setName(request.name());
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
        return FarmResponse.from(farmRepository.save(farm));
    }

    @Transactional
    public void deleteFarm(UUID farmId) {
        farmRepository.delete(getFarmEntity(farmId));
    }

    public List<FieldResponse> findFarmFields(UUID farmId) {
        getFarmEntity(farmId);
        return fieldRepository.findByFarmId(farmId).stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFarmFields(UUID farmId, Pageable pageable) {
        getFarmEntity(farmId);
        return toPageResponse(fieldRepository.findByFarmId(farmId, pageable).map(FieldResponse::from));
    }

    public List<FieldResponse> findFields() {
        return fieldRepository.findAll().stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFields(Pageable pageable) {
        return toPageResponse(fieldRepository.findAll(pageable).map(FieldResponse::from));
    }

    @Transactional
    public FieldResponse createField(FieldCreateRequest request) {
        getFarmEntity(request.farmId());
        Field field = new Field();
        field.setFarmId(request.farmId());
        field.setName(request.name());
        field.setAreaHectares(request.areaHectares());
        field.setCropType(request.cropType());
        field.setSoilType(request.soilType());
        field.setCoordinates(request.coordinates());
        return FieldResponse.from(fieldRepository.save(field));
    }

    public FieldResponse getField(UUID fieldId) {
        return FieldResponse.from(getFieldEntity(fieldId));
    }

    @Transactional
    public FieldResponse updateField(UUID fieldId, FieldUpdateRequest request) {
        Field field = getFieldEntity(fieldId);
        if (request.farmId() != null && !request.farmId().equals(field.getFarmId())) {
            getFarmEntity(request.farmId());
            field.setFarmId(request.farmId());
        }
        field.setName(request.name() == null ? field.getName() : request.name());
        field.setAreaHectares(request.areaHectares() == null ? field.getAreaHectares() : request.areaHectares());
        field.setCropType(request.cropType());
        field.setSoilType(request.soilType());
        field.setCoordinates(request.coordinates());
        return FieldResponse.from(fieldRepository.save(field));
    }

    @Transactional
    public void deleteField(UUID fieldId) {
        fieldRepository.delete(getFieldEntity(fieldId));
    }

    public List<ZoneResponse> findFieldZones(UUID fieldId) {
        getFieldEntity(fieldId);
        return zoneRepository.findByFieldId(fieldId).stream().map(ZoneResponse::from).toList();
    }

    public PageResponse<ZoneResponse> findFieldZones(UUID fieldId, Pageable pageable) {
        getFieldEntity(fieldId);
        return toPageResponse(zoneRepository.findByFieldId(fieldId, pageable).map(ZoneResponse::from));
    }

    public List<ZoneResponse> findZones() {
        return zoneRepository.findAll().stream().map(ZoneResponse::from).toList();
    }

    public PageResponse<ZoneResponse> findZones(Pageable pageable) {
        return toPageResponse(zoneRepository.findAll(pageable).map(ZoneResponse::from));
    }

    @Transactional
    public ZoneResponse createZone(ZoneCreateRequest request) {
        getFieldEntity(request.fieldId());
        Zone zone = new Zone();
        zone.setFieldId(request.fieldId());
        zone.setName(request.name());
        zone.setAreaHectares(request.areaHectares());
        zone.setIrrigationMethod(request.irrigationMethod());
        return ZoneResponse.from(zoneRepository.save(zone));
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
        zone.setIrrigationMethod(request.irrigationMethod());
        return ZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(UUID zoneId) {
        zoneRepository.delete(getZoneEntity(zoneId));
    }

    private Farm getFarmEntity(UUID farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new NotFoundException("Farm"));
    }

    private Field getFieldEntity(UUID fieldId) {
        return fieldRepository.findById(fieldId).orElseThrow(() -> new NotFoundException("Field"));
    }

    private Zone getZoneEntity(UUID zoneId) {
        return zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone"));
    }

    private void apply(Farm farm, FarmCreateRequest request) {
        farm.setName(request.name());
        farm.setDescription(request.description());
        farm.setAddress(request.address());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setAreaHectares(request.areaHectares());
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}