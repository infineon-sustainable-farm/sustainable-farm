package com.infineonbit.sustainablefarm.modules.plants.entity;

import java.util.Objects;

/**
 * The block of a plants row, as plants shows it: the row stores the id of a {@code farm.block}
 * row, its farm and code are read from the farm referential (docs/adr/0004).
 *
 * <p>Not persisted. The label is set by the service that assigns the block, or read once,
 * on first use, from the {@link BlockLabelSource} that {@link BlockLabelListener} gives to every
 * loaded row, so a code renamed in the farm referential shows on the next read. A label set by
 * hand without an id (a row built in memory, before its block is resolved) stays as it is; such
 * a row cannot be saved ({@link #requireResolved}).</p>
 */
public final class BlockLink {
    private BlockLabelSource source;
    private BlockLabel label;
    /** Block id the label belongs to; {@code null} for a label set by hand. */
    private Long labelledId;

    void useSource(BlockLabelSource source) {
        this.source = source;
    }

    /** Label of the block {@code blockId}; {@link BlockLabel#NONE} when there is neither id nor label. */
    BlockLabel label(Long blockId) {
        if (label != null && (labelledId == null || labelledId.equals(blockId))) {
            return label;
        }
        if (blockId == null) {
            return BlockLabel.NONE;
        }
        if (source == null) {
            throw new IllegalStateException("The block " + blockId + " of this plants row was not read from the farm referential");
        }
        label = Objects.requireNonNull(source.labelOf(blockId));
        labelledId = blockId;
        return label;
    }

    /** Sets the label of the block {@code blockId}, or a label by hand when {@code blockId} is null. */
    void set(Long blockId, BlockLabel label) {
        this.label = label;
        this.labelledId = blockId;
    }

    /** Sets the code by hand, keeping the farm already set by hand. */
    void setCode(String code) {
        BlockLabel current = label == null ? BlockLabel.NONE : label;
        set(null, new BlockLabel(current.farmId(), code));
    }

    /** Sets the farm by hand, keeping the code already set by hand. */
    void setFarmId(Long farmId) {
        BlockLabel current = label == null ? BlockLabel.NONE : label;
        set(null, new BlockLabel(farmId, current.code()));
    }

    /** Refuses to save a row whose block code was set by hand, without resolving it to a block. */
    void requireResolved(String row) {
        if (label != null && labelledId == null && label.code() != null) {
            throw new IllegalStateException("The block " + label.code() + " of this " + row
                    + " was not resolved to a block of the farm referential");
        }
    }
}
