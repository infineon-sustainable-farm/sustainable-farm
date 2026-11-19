package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
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

/**
 * Blocks of the farms, served as Water Supply "fields" under {@code /api/farm/fields}.
 *
 * <p>The only place, with the zone to block links, where a block gets its Plants code: Plants and
 * Product Transformation pick existing blocks by code and never create one (docs/adr/0004).</p>
 */
@Service
@Transactional(readOnly = true)
public class BlockService {
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final BlockReferences blockReferences;

    public BlockService(FarmRepository farmRepository, BlockRepository blockRepository,
                        BlockReferences blockReferences) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.blockReferences = blockReferences;
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
        String code = FarmDirectory.normalizeBlockCode(request.code());
        if (code != null) {
            requireFreeCode(code, null);
            block.setCode(code);
        }
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
        if (request.code() != null) {
            changeCode(block, FarmDirectory.normalizeBlockCode(request.code()));
        }
        return FieldResponse.from(blockRepository.save(block));
    }

    /**
     * Sets or clears the Plants code of a block. Another block's code is refused (409); clearing the
     * code of a block that Plants or Product Transformation rows reference is refused too (409).
     */
    private void changeCode(Block block, String code) {
        if (code == null) {
            if (block.getCode() != null && blockReferences.isReferenced(block.getId())) {
                throw new ConflictException("Block " + block.getCode()
                        + " is used by Plants or Product Transformation: it keeps its code");
            }
            block.setCode(null);
            return;
        }
        requireFreeCode(code, block.getId());
        block.setCode(code);
    }

    private void requireFreeCode(String code, Long blockId) {
        blockRepository.findByCode(code).filter(other -> !other.getId().equals(blockId)).ifPresent(other -> {
            throw new ConflictException("Block code " + code + " is already the code of the field " + other.getName());
        });
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
