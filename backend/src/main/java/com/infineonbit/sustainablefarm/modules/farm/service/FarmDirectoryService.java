package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link FarmDirectory} implementation: read-only views of the farm repositories. */
@Service
@Transactional(readOnly = true)
public class FarmDirectoryService implements FarmDirectory {
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final FarmZoneRepository zoneRepository;

    public FarmDirectoryService(FarmRepository farmRepository, BlockRepository blockRepository,
                                FarmZoneRepository zoneRepository) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    public Optional<FarmDto> findFarm(Long farmId) {
        return farmId == null ? Optional.empty() : farmRepository.findById(farmId).map(FarmMapper::toDto);
    }

    @Override
    public boolean farmExists(Long farmId) {
        return farmId != null && farmRepository.existsById(farmId);
    }

    @Override
    public List<FarmDto> listFarms() {
        return farmRepository.findAll().stream().map(FarmMapper::toDto).toList();
    }

    @Override
    public Optional<BlockDto> findBlock(Long blockId) {
        return blockId == null ? Optional.empty() : blockRepository.findById(blockId).map(FarmMapper::toDto);
    }

    @Override
    public Optional<BlockDto> findBlockByCode(String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        return normalized == null ? Optional.empty() : blockRepository.findByCode(normalized).map(FarmMapper::toDto);
    }

    @Override
    public List<BlockDto> listBlocksOfFarm(Long farmId) {
        return farmId == null ? List.of() : blockRepository.findByFarmId(farmId).stream().map(FarmMapper::toDto).toList();
    }

    @Override
    public Optional<ZoneDto> findZone(Long zoneId) {
        return zoneId == null ? Optional.empty() : zoneRepository.findById(zoneId).map(FarmMapper::toDto);
    }

    @Override
    public boolean zoneExists(Long zoneId) {
        return zoneId != null && zoneRepository.existsById(zoneId);
    }

    @Override
    public List<ZoneDto> listZones() {
        return zoneRepository.findAll().stream().map(FarmMapper::toDto).toList();
    }

    @Override
    public List<ZoneDto> listZones(Collection<Long> zoneIds) {
        List<Long> ids = zoneIds == null ? List.of() : zoneIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return zoneRepository.findAllByIdIn(ids).stream().map(FarmMapper::toDto).toList();
    }

    @Override
    public List<ZoneDto> listZonesOfBlock(Long blockId) {
        return blockId == null ? List.of() : zoneRepository.findByBlockId(blockId).stream().map(FarmMapper::toDto).toList();
    }

    @Override
    public List<ZoneDto> listZonesOfFarm(Long farmId) {
        return farmId == null ? List.of() : zoneRepository.findByBlockFarmId(farmId).stream().map(FarmMapper::toDto).toList();
    }
}
