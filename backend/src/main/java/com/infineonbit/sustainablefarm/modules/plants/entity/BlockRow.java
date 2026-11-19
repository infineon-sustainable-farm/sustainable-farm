package com.infineonbit.sustainablefarm.modules.plants.entity;

/** A plants row that references blocks of the farm referential through {@link BlockLink}. */
public interface BlockRow {

    /** Gives the source of the block labels to the row, once loaded. */
    void useBlockLabels(BlockLabelSource source);
}
