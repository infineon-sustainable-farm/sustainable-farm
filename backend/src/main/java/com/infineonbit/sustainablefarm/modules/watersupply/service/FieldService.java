package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FieldService {
    private final FarmRepository farmRepository;
    private final FieldRepository fieldRepository;

    public FieldService(FarmRepository farmRepository, FieldRepository fieldRepository) {
        this.farmRepository = farmRepository;
        this.fieldRepository = fieldRepository;
    }

    public List<FieldResponse> findFields() {
        return fieldRepository.findAll().stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFields(Pageable pageable) {
        return toPageResponse(fieldRepository.findAll(pageable).map(FieldResponse::from));
    }

    public List<FieldResponse> findFarmFields(UUID farmId) {
        getFarm(farmId);
        return fieldRepository.findByFarmId(farmId).stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFarmFields(UUID farmId, Pageable pageable) {
        getFarm(farmId);
        return toPageResponse(fieldRepository.findByFarmId(farmId, pageable).map(FieldResponse::from));
    }

    @Transactional
    public FieldResponse createField(FieldCreateRequest request) {
        getFarm(request.farmId());
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

    private void getFarm(UUID farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm not found");
        }
    }

    private Field getFieldEntity(UUID fieldId) {
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}