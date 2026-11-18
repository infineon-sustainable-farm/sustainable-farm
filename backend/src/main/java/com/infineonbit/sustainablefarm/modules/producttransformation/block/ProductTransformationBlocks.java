package com.infineonbit.sustainablefarm.modules.producttransformation.block;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The farm and the block that lots, harvest events and raw intakes come from, read and written
 * through the site referential ({@code farm.api}, docs/adr/0004).
 *
 * <p>The API keeps its text fields: the block text is a block code ({@code farm.block.code}), the
 * farm text is the name of the block's farm. A row stores only the id of the block, which gives
 * the farm.</p>
 *
 * <p>Write rule ({@link #resolve}): the block text is normalized (trimmed, upper case). A block
 * that has this code is used as it is, whatever the farm text says. A new code creates the block
 * ({@link BlockRegistry#findOrCreateBlock}) under the farm the farm text designates, a farm id or
 * a farm name ignoring case, or under the default farm when the text designates no farm.</p>
 */
@Component
public class ProductTransformationBlocks {

    /** The API view of a block: the name of its farm and its code. */
    public record Label(String farm, String block) {
    }

    private final FarmDirectory farmDirectory;
    private final BlockRegistry blockRegistry;

    public ProductTransformationBlocks(FarmDirectory farmDirectory, BlockRegistry blockRegistry) {
        this.farmDirectory = farmDirectory;
        this.blockRegistry = blockRegistry;
    }

    /**
     * The id of the block a farm text and a block text designate, created when the code is new.
     *
     * @throws BadRequestException (400) when the block text is blank
     */
    public Long resolve(String farmText, String blockText) {
        String code = FarmDirectory.normalizeBlockCode(blockText);
        if (code == null) {
            throw new BadRequestException("Block ID is required");
        }
        Optional<BlockDto> existing = farmDirectory.findBlockByCode(code);
        if (existing.isPresent()) {
            return existing.get().id();
        }
        Long farmId = findFarm(farmText).map(FarmDto::id).orElse(null);
        return blockRegistry.findOrCreateBlock(farmId, code).id();
    }

    /** Like {@link #resolve}, but {@code null} for a blank block text, for the updates that keep the block. */
    public Long resolveOrNull(String farmText, String blockText) {
        return FarmDirectory.normalizeBlockCode(blockText) == null ? null : resolve(farmText, blockText);
    }

    /** The label of one block; both texts are {@code null} for a missing or unknown block. */
    public Label label(Long blockId) {
        return labels(blockId == null ? List.of() : List.of(blockId)).getOrDefault(blockId, new Label(null, null));
    }

    /** The labels of several blocks in one lookup; unknown ids are absent from the map. */
    public Map<Long, Label> labels(Collection<Long> blockIds) {
        List<BlockDto> blocks = farmDirectory.listBlocks(blockIds);
        Map<Long, String> farmNames = new HashMap<>();
        blocks.stream().map(BlockDto::farmId).filter(Objects::nonNull).distinct()
                .forEach(farmId -> farmNames.put(farmId, farmDirectory.findFarm(farmId).map(FarmDto::name).orElse(null)));
        return blocks.stream().collect(Collectors.toMap(BlockDto::id,
                block -> new Label(farmNames.get(block.farmId()), block.code()), (a, b) -> a));
    }

    /** The labels of the blocks of several rows, in one lookup. */
    public <T> Map<Long, Label> labelsOf(Collection<T> rows, Function<T, Long> blockId) {
        return labels(rows.stream().map(blockId).filter(Objects::nonNull).distinct().toList());
    }

    /** The ids of the blocks of the farm a farm text designates; empty for an unknown farm. */
    public List<Long> blockIdsOfFarm(String farmText) {
        return findFarm(farmText)
                .map(farm -> farmDirectory.listBlocksOfFarm(farm.id()).stream().map(BlockDto::id).toList())
                .orElse(List.of());
    }

    /** The id of the block of a code, without creating it; empty for an unknown code. */
    public Optional<Long> findBlockId(String blockText) {
        return farmDirectory.findBlockByCode(blockText).map(BlockDto::id);
    }

    /** The farm a text designates: a farm id when the text is a number of an existing farm, else a farm name. */
    private Optional<FarmDto> findFarm(String farmText) {
        if (farmText == null || farmText.isBlank()) {
            return Optional.empty();
        }
        String text = farmText.trim();
        if (text.chars().allMatch(Character::isDigit) && text.length() < 19) {
            Optional<FarmDto> byId = farmDirectory.findFarm(Long.valueOf(text));
            if (byId.isPresent()) {
                return byId;
            }
        }
        return farmDirectory.findFarmByName(text);
    }
}
