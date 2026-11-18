package com.infineonbit.sustainablefarm.modules.plants.support;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabel;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantsBlocks;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory farm referential for the plants unit tests: {@link FarmDirectory} and
 * {@link BlockRegistry} with the rule of the farm module (a code is unique, a new code is created
 * under the requested farm or farm {@link #DEFAULT_FARM}).
 *
 * <p>Ids are stable: block {@code "A"} is {@code idOf("A")} in every test, so a repository stub
 * can name the block of a code before any write.</p>
 */
public final class TestBlocks implements FarmDirectory, BlockRegistry {

    /** Farm of the blocks created without a farm. */
    public static final long DEFAULT_FARM = 1L;

    private final Map<String, BlockDto> byCode = new LinkedHashMap<>();
    private final java.util.Set<Long> farms = new java.util.HashSet<>(List.of(DEFAULT_FARM, 2L, 3L));

    /** Stable id of a code: A=1 ... Z=26, other codes from 100 on, by their hash. */
    public static long idOf(String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        if (normalized.length() == 1 && Character.isLetter(normalized.charAt(0))) {
            return normalized.charAt(0) - 'A' + 1L;
        }
        return 100L + Math.floorMod(normalized.hashCode(), 1_000_000);
    }

    /** The ids a list filter binds when no block matches (see {@code PlantsBlocks.BlockFilter}). */
    public static final List<Long> NO_IDS = List.of(-1L);

    /** The block ids of codes, as a list filter binds them. */
    public static List<Long> idsOf(String... codes) {
        return java.util.Arrays.stream(codes).map(TestBlocks::idOf).toList();
    }

    /** A referential whose blocks of these codes all belong to one farm, in this order. */
    public static TestBlocks onFarm(long farmId, String... codes) {
        TestBlocks blocks = new TestBlocks();
        for (String code : codes) {
            blocks.with(code, farmId);
        }
        return blocks;
    }

    /** A {@link PlantsBlocks} on a new empty referential. */
    public static PlantsBlocks plantsBlocks() {
        TestBlocks blocks = new TestBlocks();
        return new PlantsBlocks(blocks, blocks);
    }

    /** A {@link PlantsBlocks} on this referential. */
    public PlantsBlocks asPlantsBlocks() {
        return new PlantsBlocks(this, this);
    }

    /** Adds the block of a code to a farm, as if the farm module had it already. */
    public TestBlocks with(String code, long farmId) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        farms.add(farmId);
        byCode.put(normalized, new BlockDto(idOf(normalized), farmId, normalized, normalized, 0.0,
                null, null, null, null));
        return this;
    }

    /** Label of the block of a code under the default farm. */
    public static BlockLabel label(String code) {
        return new BlockLabel(DEFAULT_FARM, FarmDirectory.normalizeBlockCode(code));
    }

    @Override
    public BlockDto findOrCreateBlock(Long farmId, String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        if (normalized == null) {
            throw new BadRequestException("blockCode must not be blank");
        }
        BlockDto existing = byCode.get(normalized);
        if (existing != null) {
            if (farmId != null && !farmId.equals(existing.farmId())) {
                throw new BadRequestException("Block " + normalized + " belongs to farm " + existing.farmId());
            }
            return existing;
        }
        if (farmId != null && !farms.contains(farmId)) {
            throw new BadRequestException("Farm " + farmId + " not found");
        }
        with(normalized, farmId == null ? DEFAULT_FARM : farmId);
        return byCode.get(normalized);
    }

    @Override
    public Optional<FarmDto> findFarm(Long farmId) {
        return farmExists(farmId)
                ? Optional.of(new FarmDto(farmId, "Farm " + farmId, null, null, null, null, null, null))
                : Optional.empty();
    }

    @Override
    public boolean farmExists(Long farmId) {
        return farmId != null && farms.contains(farmId);
    }

    @Override
    public List<FarmDto> listFarms() {
        return farms.stream().sorted().map(id -> findFarm(id).orElseThrow()).toList();
    }

    @Override
    public Optional<BlockDto> findBlock(Long blockId) {
        return byCode.values().stream().filter(block -> block.id().equals(blockId)).findFirst();
    }

    @Override
    public Optional<BlockDto> findBlockByCode(String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        return normalized == null ? Optional.empty() : Optional.ofNullable(byCode.get(normalized));
    }

    @Override
    public List<BlockDto> listBlocks(Collection<Long> blockIds) {
        return byCode.values().stream().filter(block -> blockIds.contains(block.id())).toList();
    }

    @Override
    public Optional<FarmDto> findFarmByName(String name) {
        return Optional.empty();
    }

    @Override
    public List<BlockDto> listBlocksOfFarm(Long farmId) {
        return byCode.values().stream().filter(block -> block.farmId().equals(farmId)).toList();
    }

    @Override
    public Optional<ZoneDto> findZone(Long zoneId) {
        return Optional.empty();
    }

    @Override
    public boolean zoneExists(Long zoneId) {
        return false;
    }

    @Override
    public List<ZoneDto> listZones() {
        return List.of();
    }

    @Override
    public List<ZoneDto> listZones(Collection<Long> zoneIds) {
        return List.of();
    }

    @Override
    public List<ZoneDto> listZonesOfBlock(Long blockId) {
        return List.of();
    }

    @Override
    public List<ZoneDto> listZonesOfFarm(Long farmId) {
        return List.of();
    }
}
