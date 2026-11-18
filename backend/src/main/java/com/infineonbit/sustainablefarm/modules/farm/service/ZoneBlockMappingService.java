package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneBlockMappingCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneBlockMappingResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneBlockMappingUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Zone;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zone to Plants block links of the Water Supply "Plants blocks" screen, backed by Zone -> Block.
 *
 * <p>There is no mapping table: a zone is linked to the Plants block of code X when its block carries
 * the code X. Linking a zone gives its block the code, unlinking clears it. A zone therefore waters
 * a single block, and every zone of a block shares its code (docs/adr/0004).</p>
 */
@Service
@Transactional(readOnly = true)
public class ZoneBlockMappingService {
    private final FarmZoneRepository zoneRepository;
    private final BlockRepository blockRepository;
    private final FarmRepository farmRepository;
    private final BlockReferences blockReferences;

    public ZoneBlockMappingService(FarmZoneRepository zoneRepository, BlockRepository blockRepository,
                                   FarmRepository farmRepository, BlockReferences blockReferences) {
        this.zoneRepository = zoneRepository;
        this.blockRepository = blockRepository;
        this.farmRepository = farmRepository;
        this.blockReferences = blockReferences;
    }

    public List<ZoneBlockMappingResponse> findAll(Long zoneId, String blockCode, Long farmId) {
        String normalizedBlock = FarmDirectory.normalizeBlockCode(blockCode);
        return zoneRepository.findByBlockCodeIsNotNull().stream()
                .map(FarmMapper::toDto)
                .filter(zone -> zoneId == null || zoneId.equals(zone.id()))
                .filter(zone -> farmId == null || farmId.equals(zone.farmId()))
                .filter(zone -> normalizedBlock == null || normalizedBlock.equals(zone.blockCode()))
                .map(ZoneBlockMappingResponse::from)
                .toList();
    }

    @Transactional
    public ZoneBlockMappingResponse create(ZoneBlockMappingCreateRequest request) {
        requireFarm(request.farmId());
        Zone zone = requireZone(request.zoneId());
        requireSameFarm(zone, request.farmId());
        String code = requireBlockCode(request.blockCode());
        Block block = zone.getBlock();
        if (code.equals(block.getCode())) {
            throw new ConflictException("This zone is already linked to block " + code);
        }
        if (block.getCode() != null) {
            throw new ConflictException("The field of this zone is already linked to block " + block.getCode()
                    + ": a zone waters a single block");
        }
        assignCode(block, code);
        return ZoneBlockMappingResponse.from(FarmMapper.toDto(zone));
    }

    public ZoneBlockMappingResponse get(Long mappingId) {
        return ZoneBlockMappingResponse.from(FarmMapper.toDto(getLinkedZone(mappingId)));
    }

    /** Renames the block code of a linked zone; the zone and its farm cannot change. */
    @Transactional
    public ZoneBlockMappingResponse update(Long mappingId, ZoneBlockMappingUpdateRequest request) {
        Zone zone = getLinkedZone(mappingId);
        if (request.zoneId() != null && !request.zoneId().equals(zone.getId())) {
            throw new BadRequestException("zoneId cannot change: unlink the zone and link the other one");
        }
        if (request.farmId() != null) {
            requireSameFarm(zone, request.farmId());
        }
        if (request.blockCode() != null) {
            String code = requireBlockCode(request.blockCode());
            if (!code.equals(zone.getBlock().getCode())) {
                assignCode(zone.getBlock(), code);
            }
        }
        return ZoneBlockMappingResponse.from(FarmMapper.toDto(zone));
    }

    /**
     * Unlinks the zone: its block loses its code, for every zone of the block. Refused (409) while
     * rows of Plants or Product Transformation reference the block, since they designate it by its code.
     */
    @Transactional
    public void delete(Long mappingId) {
        Block block = getLinkedZone(mappingId).getBlock();
        if (blockReferences.isReferenced(block.getId())) {
            throw new ConflictException("Block " + block.getCode()
                    + " is used by Plants or Product Transformation: it keeps its code");
        }
        block.setCode(null);
        blockRepository.save(block);
    }

    private void assignCode(Block block, String code) {
        blockRepository.findByCode(code).filter(other -> !other.getId().equals(block.getId())).ifPresent(other -> {
            throw new ConflictException("Block " + code + " is already the field " + other.getName()
                    + ": move the zone into that field");
        });
        block.setCode(code);
        blockRepository.saveAndFlush(block);
    }

    private void requireFarm(Long farmId) {
        if (farmId == null || !farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm not found");
        }
    }

    private static void requireSameFarm(Zone zone, Long farmId) {
        if (!farmId.equals(zone.getBlock().getFarm().getId())) {
            throw new BadRequestException("farmId must match the farm of the mapped zone");
        }
    }

    private Zone requireZone(Long zoneId) {
        if (zoneId == null) {
            throw new ResourceNotFoundException("Zone not found");
        }
        return zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }

    private Zone getLinkedZone(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .filter(zone -> zone.getBlock().getCode() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Zone block mapping not found"));
    }

    private static String requireBlockCode(String blockCode) {
        String normalized = FarmDirectory.normalizeBlockCode(blockCode);
        if (normalized == null) {
            throw new BadRequestException("blockCode must not be blank");
        }
        return normalized;
    }
}
