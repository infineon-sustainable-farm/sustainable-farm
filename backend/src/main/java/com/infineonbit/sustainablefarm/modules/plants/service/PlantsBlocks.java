package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabel;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabelSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * The blocks of plants, read and created through {@code farm.api} (docs/adr/0004).
 *
 * <p>A plants block code designates the {@code farm.block} row of that code. Writing a row on a
 * code that no block has yet creates the block ({@link BlockRegistry}), under the requested farm or,
 * without one, the default farm, so every code accepted before the farm referential is still
 * accepted. A requested farm must exist and own the block (400 otherwise). Reading with a code or
 * a farm that does not exist finds nothing, as before.</p>
 */
@Component
public class PlantsBlocks implements BlockLabelSource {

    /** Id that no block has (identities start at 1): the filter that matches no row. */
    static final Long NO_BLOCK = -1L;

    private final FarmDirectory farmDirectory;
    private final BlockRegistry blockRegistry;

    public PlantsBlocks(FarmDirectory farmDirectory, BlockRegistry blockRegistry) {
        this.farmDirectory = farmDirectory;
        this.blockRegistry = blockRegistry;
    }

    /**
     * Filter of a list query on the blocks of the rows: every block, or the blocks among
     * {@code ids}. {@code ids} is never empty, so it can be bound to an {@code IN} clause.
     */
    public record BlockFilter(boolean all, List<Long> ids) {
        static final BlockFilter ALL = new BlockFilter(true, List.of(NO_BLOCK));
        static final BlockFilter NONE = new BlockFilter(false, List.of(NO_BLOCK));
    }

    @Override
    public BlockLabel labelOf(Long blockId) {
        return farmDirectory.findBlock(blockId).map(PlantsBlocks::label).orElse(BlockLabel.NONE);
    }

    /**
     * The block a row is written on, created when no block has the code.
     *
     * @param farmId    requested farm, or {@code null} for any
     * @param blockCode block code, trimmed and upper-cased by the farm referential
     * @return the block
     */
    public BlockDto resolve(Long farmId, String blockCode) {
        return blockRegistry.findOrCreateBlock(farmId, blockCode);
    }

    /**
     * The block of an existing code, without creating it.
     *
     * @param farmId    farm the block must belong to, or {@code null} for any
     * @param blockCode block code, compared after trimming and upper-casing
     * @return the block, or empty when no block of that farm has the code
     */
    public Optional<BlockDto> find(Long farmId, String blockCode) {
        return farmDirectory.findBlockByCode(blockCode)
                .filter(block -> farmId == null || farmId.equals(block.farmId()));
    }

    /**
     * The filter of the optional farm and block parameters of a list route.
     *
     * @param farmId    farm, or {@code null} for every farm
     * @param blockCode block code, or {@code null} or blank for every block
     * @return the filter; {@link BlockFilter#NONE} when nothing matches
     */
    public BlockFilter filter(Long farmId, String blockCode) {
        if (FarmDirectory.normalizeBlockCode(blockCode) != null) {
            return find(farmId, blockCode).map(block -> new BlockFilter(false, List.of(block.id())))
                    .orElse(BlockFilter.NONE);
        }
        if (farmId == null) {
            return BlockFilter.ALL;
        }
        List<Long> ids = farmDirectory.listBlocksOfFarm(farmId).stream().map(BlockDto::id).toList();
        return ids.isEmpty() ? BlockFilter.NONE : new BlockFilter(false, ids);
    }

    /**
     * Whether a farm exists, for the farm of a row that has no block (a nursery batch).
     *
     * @param farmId the farm
     * @return true when the farm exists
     */
    public boolean farmExists(Long farmId) {
        return farmDirectory.farmExists(farmId);
    }

    /**
     * Sorts rows by block code, keeping the order of the rows of a block: the order of the list
     * routes, by block code then by their own order. A row without a block comes last, as with an
     * {@code ORDER BY} on PostgreSQL.
     *
     * @param rows      rows in their order within a block
     * @param blockCode code of the block of a row
     * @return the rows sorted, a new list
     */
    public static <T> List<T> sortedByBlockCode(List<T> rows, Function<T, String> blockCode) {
        List<T> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing(blockCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    /** The label plants shows for a block. */
    public static BlockLabel label(BlockDto block) {
        return new BlockLabel(block.farmId(), block.code());
    }
}
