package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.ZoneBlockMapping;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneBlockMappingRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ZoneBlockMappingService implements ZoneBlockResolver {
    /** Same rule as Plants: 1 to 10 letters or digits, upper case ("B", "B2", never "Block B"). */
    private static final Pattern BLOCK_CODE = Pattern.compile("[A-Z0-9]{1,10}");

    private final ZoneBlockMappingRepository mappingRepository;
    private final FieldZoneRepository zoneRepository;
    private final FieldRepository fieldRepository;
    private final FarmRepository farmRepository;

    public ZoneBlockMappingService(ZoneBlockMappingRepository mappingRepository,
                                   FieldZoneRepository zoneRepository,
                                   FieldRepository fieldRepository,
                                   FarmRepository farmRepository) {
        this.mappingRepository = mappingRepository;
        this.zoneRepository = zoneRepository;
        this.fieldRepository = fieldRepository;
        this.farmRepository = farmRepository;
    }

    public List<ZoneBlockMappingResponse> findAll(UUID zoneId, String blockCode, UUID farmId) {
        String normalizedBlock = normalizeBlockCode(blockCode);
        return mappingRepository.findAll().stream()
                .filter(mapping -> zoneId == null || zoneId.equals(mapping.getZoneId()))
                .filter(mapping -> farmId == null || farmId.equals(mapping.getFarmId()))
                .filter(mapping -> normalizedBlock == null || normalizedBlock.equals(mapping.getBlockCode()))
                .map(ZoneBlockMappingResponse::from)
                .toList();
    }

    @Transactional
    public ZoneBlockMappingResponse create(ZoneBlockMappingCreateRequest request) {
        ZoneBlockMapping mapping = new ZoneBlockMapping();
        apply(mapping, request.zoneId(), request.blockCode(), request.farmId());
        return ZoneBlockMappingResponse.from(mappingRepository.saveAndFlush(mapping));
    }

    public ZoneBlockMappingResponse get(UUID mappingId) {
        return ZoneBlockMappingResponse.from(getMappingEntity(mappingId));
    }

    @Transactional
    public ZoneBlockMappingResponse update(UUID mappingId, ZoneBlockMappingUpdateRequest request) {
        ZoneBlockMapping mapping = getMappingEntity(mappingId);
        apply(mapping,
                request.zoneId() == null ? mapping.getZoneId() : request.zoneId(),
                request.blockCode() == null ? mapping.getBlockCode() : request.blockCode(),
                request.farmId() == null ? mapping.getFarmId() : request.farmId());
        return ZoneBlockMappingResponse.from(mappingRepository.saveAndFlush(mapping));
    }

    @Transactional
    public void delete(UUID mappingId) {
        mappingRepository.delete(getMappingEntity(mappingId));
    }

    @Override
    public List<String> blockCodesForZone(UUID zoneId) {
        if (zoneId == null) {
            return List.of();
        }
        return mappingRepository.findByZoneId(zoneId).stream()
                .map(ZoneBlockMapping::getBlockCode)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public Map<UUID, List<String>> blockCodesForZones(Collection<UUID> zoneIds) {
        List<UUID> ids = zoneIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mappingRepository.findByZoneIdIn(ids).stream()
                .collect(Collectors.groupingBy(ZoneBlockMapping::getZoneId, Collectors.collectingAndThen(
                        Collectors.mapping(ZoneBlockMapping::getBlockCode, Collectors.toCollection(TreeSet::new)),
                        List::copyOf)));
    }

    @Override
    public List<UUID> zoneIdsForBlock(UUID farmId, String blockCode) {
        String normalizedBlock = requireBlockCode(blockCode);
        List<ZoneBlockMapping> mappings = farmId == null
                ? mappingRepository.findByBlockCode(normalizedBlock)
                : mappingRepository.findByFarmIdAndBlockCode(farmId, normalizedBlock);
        return mappings.stream().map(ZoneBlockMapping::getZoneId).distinct().toList();
    }

    /** Farms owning a block (several when the same code exists on two farms). */
    public List<UUID> farmIdsForBlock(UUID farmId, String blockCode) {
        String normalizedBlock = requireBlockCode(blockCode);
        List<ZoneBlockMapping> mappings = farmId == null
                ? mappingRepository.findByBlockCode(normalizedBlock)
                : mappingRepository.findByFarmIdAndBlockCode(farmId, normalizedBlock);
        return mappings.stream().map(ZoneBlockMapping::getFarmId).distinct().toList();
    }

    /** Upper-case block code, the form stored in {@code zone_block_mapping}; {@code null} for a blank value. */
    public static String normalizeBlockCode(String blockCode) {
        if (blockCode == null || blockCode.isBlank()) {
            return null;
        }
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    private void apply(ZoneBlockMapping mapping, UUID zoneId, String blockCode, UUID farmId) {
        UUID resolvedFarmId = requireFarm(farmId);
        Zone zone = requireZone(zoneId);
        Field field = fieldRepository.findById(zone.getFieldId()).orElseThrow(() -> new NotFoundException("Field"));
        if (!resolvedFarmId.equals(field.getFarmId())) {
            throw new IllegalArgumentException("farmId must match the farm of the mapped zone");
        }

        String block = requireBlockCode(blockCode);
        // Plants: a zone may cover several blocks, but a block belongs to only one zone of its farm.
        mappingRepository.findByFarmIdAndBlockCode(resolvedFarmId, block).stream()
                .filter(existing -> !Objects.equals(existing.getId(), mapping.getId()))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, existing.getZoneId().equals(zoneId)
                            ? "This zone is already linked to block " + block
                            : "Block " + block + " is already linked to another zone: a block belongs to only one zone");
                });

        mapping.setZoneId(zoneId);
        mapping.setBlockCode(block);
        mapping.setFarmId(resolvedFarmId);
    }

    private UUID requireFarm(UUID farmId) {
        if (farmId == null || farmRepository.findById(farmId).isEmpty()) {
            throw new NotFoundException("Farm");
        }
        return farmId;
    }

    private Zone requireZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone");
        }
        return zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone"));
    }

    private ZoneBlockMapping getMappingEntity(UUID mappingId) {
        return mappingRepository.findById(mappingId).orElseThrow(() -> new NotFoundException("Zone block mapping"));
    }

    private String requireBlockCode(String blockCode) {
        String normalized = normalizeBlockCode(blockCode);
        if (normalized == null || !BLOCK_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("blockCode must be 1 to 10 letters or digits, without prefix or space (for example A or B2)");
        }
        return normalized;
    }
}
