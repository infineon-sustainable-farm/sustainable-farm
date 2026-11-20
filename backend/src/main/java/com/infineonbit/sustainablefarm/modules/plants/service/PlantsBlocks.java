package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
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
 * The blocks of plants, read through {@code farm.api} (docs/adr/0004).
 *
 * <p>A plants block code designates the {@code farm.block} row of that code. Plants never creates
 * a block: blocks and their codes are owned by the Farms &amp; fields screen of Water Supply (farm
 * module). Writing a row on a code that no block has, or with a requested farm that does not exist
 * or does not own the block, is refused with a 400. Reading with a code or a farm that does not
 * exist finds nothing, as before.</p>
 */
@Component
public class PlantsBlocks implements BlockLabelSource {

    /** Id that no block has (identities start at 1): the filter that matches no row. */
    static final Long NO_BLOCK = -1L;

    private final FarmDirectory farmDirectory;

    public PlantsBlocks(FarmDirectory farmDirectory) {
        this.farmDirectory = farmDirectory;
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
     * The existing block a row is written on.
     *
     * @param farmId    requested farm, or {@code null} for any
     * @param blockCode block code, compared after trimming and upper-casing
     * @return the block
     * @throws BadRequestException (400) when the code is blank, no block has it, the farm does not
     *         exist, or the block belongs to another farm
     */
    public BlockDto resolve(Long farmId, String blockCode) {
        String code = FarmDirectory.normalizeBlockCode(blockCode);
        if (code == null) {
            throw new BadRequestException("blockCode must not be blank");
        }
        if (farmId != null && !farmDirectory.farmExists(farmId)) {
            throw new BadRequestException("Farm " + farmId + " not found");
        }
        BlockDto block = farmDirectory.findBlockByCode(code).orElseThrow(() -> new BadRequestException(
                "Unknown block " + code + ": pick a field of the Farms & fields screen (Water Supply)"));
        if (farmId != null && !farmId.equals(block.farmId())) {
            throw new BadRequestException("Block " + code + " belongs to farm " + block.farmId()
                    + ", not to farm " + farmId);
        }
        return block;
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
