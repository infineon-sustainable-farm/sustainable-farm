package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Reads the {@link BlockLabel} of a {@code farm.block} row, for the plants rows that reference it.
 * Implemented by {@code service.PlantsBlocks} over {@code farm.api}.
 */
public interface BlockLabelSource {

    /**
     * @param blockId id of the block
     * @return its label, {@link BlockLabel#NONE} for an unknown block
     */
    BlockLabel labelOf(Long blockId);
}
