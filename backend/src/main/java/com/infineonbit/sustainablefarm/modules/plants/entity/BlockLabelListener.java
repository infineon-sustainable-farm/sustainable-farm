package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.PostLoad;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Gives the {@link BlockLabelSource} to every loaded plants row that references a block, so that
 * its farm and block code are read on first use. Created by Spring (Hibernate's bean container).
 */
public class BlockLabelListener {
    private final ObjectProvider<BlockLabelSource> source;

    public BlockLabelListener(ObjectProvider<BlockLabelSource> source) {
        this.source = source;
    }

    @PostLoad
    void attach(Object row) {
        BlockLabelSource available = source.getIfAvailable();
        if (row instanceof BlockRow blockRow && available != null) {
            blockRow.useBlockLabels(available);
        }
    }
}
