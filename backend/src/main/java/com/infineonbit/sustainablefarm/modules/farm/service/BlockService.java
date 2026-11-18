package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.FieldResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.FieldUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Blocks of the farms, served as Water Supply "fields" under {@code /api/fields}. */
@Service
@Transactional(readOnly = true)
public class BlockService {
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;

    public BlockService(FarmRepository farmRepository, BlockRepository blockRepository) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
    }

    public List<FieldResponse> findFarmFields(Long farmId) {
        getFarmEntity(farmId);
        return blockRepository.findByFarmId(farmId).stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFarmFields(Long farmId, Pageable pageable) {
        getFarmEntity(farmId);
        return FarmService.toPageResponse(blockRepository.findByFarmId(farmId, pageable).map(FieldResponse::from));
    }

    public List<FieldResponse> findFields() {
        return blockRepository.findAll().stream().map(FieldResponse::from).toList();
    }

    public PageResponse<FieldResponse> findFields(Pageable pageable) {
        return FarmService.toPageResponse(blockRepository.findAll(pageable).map(FieldResponse::from));
    }

    @Transactional
    public FieldResponse createField(FieldCreateRequest request) {
        Block block = new Block();
        block.setFarm(getFarmEntity(request.farmId()));
        block.setName(request.name());
        block.setAreaHectares(request.areaHectares());
        block.setCropType(request.cropType());
        block.setSoilType(request.soilType());
        block.setCoordinates(request.coordinates());
        return FieldResponse.from(blockRepository.save(block));
    }

    public FieldResponse getField(Long fieldId) {
        return FieldResponse.from(getBlockEntity(fieldId));
    }

    @Transactional
    public FieldResponse updateField(Long fieldId, FieldUpdateRequest request) {
        Block block = getBlockEntity(fieldId);
        if (request.farmId() != null && !request.farmId().equals(block.getFarm().getId())) {
            block.setFarm(getFarmEntity(request.farmId()));
        }
        block.setName(request.name() == null ? block.getName() : request.name());
        block.setAreaHectares(request.areaHectares() == null ? block.getAreaHectares() : request.areaHectares());
        block.setCropType(request.cropType());
        block.setSoilType(request.soilType());
        block.setCoordinates(request.coordinates());
        return FieldResponse.from(blockRepository.save(block));
    }

    /** A block that still has zones is refused with 409 by the foreign key. */
    @Transactional
    public void deleteField(Long fieldId) {
        blockRepository.delete(getBlockEntity(fieldId));
        blockRepository.flush();
    }

    private Farm getFarmEntity(Long farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
    }

    private Block getBlockEntity(Long blockId) {
        return blockRepository.findById(blockId).orElseThrow(() -> new ResourceNotFoundException("Field not found"));
    }
}
