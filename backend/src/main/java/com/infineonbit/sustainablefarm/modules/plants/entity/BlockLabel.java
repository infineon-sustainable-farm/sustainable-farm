package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * How plants shows a block of the farm referential: its farm and its code.
 *
 * @param farmId farm of the block, or {@code null} for no block
 * @param code   block code, for example {@code "A"}, or {@code null} for no block
 */
public record BlockLabel(Long farmId, String code) {

    /** The label of a row without a block. */
    public static final BlockLabel NONE = new BlockLabel(null, null);
}
