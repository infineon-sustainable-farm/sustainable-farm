package com.infineonbit.sustainablefarm.modules.farm.api;

/**
 * The one write the other modules make on the site referential: designating a block by its code
 * (ADR 0004, "Modèle du site").
 *
 * <p>Plants and Product Transformation name a block by a code typed by the user ("A", "B2"). The
 * code designates the {@code farm.block} row of that code; when no block has it yet, the block is
 * created, so that a new code keeps being accepted as before the farm module. The new block is
 * named after its code, with an area of 0 ha, and belongs to the requested farm or, without one,
 * to the default farm: the farm of the lowest id, created under {@link #DEFAULT_FARM_NAME} when
 * there is no farm at all.</p>
 *
 * <p>A new block is committed in a transaction of its own, so two first writes on the same new code
 * sent at the same time both get the block: the one that loses the race on the unique constraint
 * {@link #CODE_CONSTRAINT} reads the block of the other.</p>
 */
public interface BlockRegistry {

    /** Name of the farm created for the first block when the referential has no farm. */
    String DEFAULT_FARM_NAME = "Default farm";

    /** Unique constraint on the block code, the one a concurrent creation of the same code violates. */
    String CODE_CONSTRAINT = "uk_block_code";

    /**
     * The block carrying a code, created when no block carries it.
     *
     * @param farmId farm the block must belong to, or {@code null} for any farm (the default farm
     *               for a new block)
     * @param code   block code, normalized by {@link FarmDirectory#normalizeBlockCode}
     * @return the block
     * @throws com.infineonbit.sustainablefarm.core.exception.BadRequestException (400) when the code
     *         is blank, the farm does not exist, or the block belongs to another farm
     */
    BlockDto findOrCreateBlock(Long farmId, String code);
}
