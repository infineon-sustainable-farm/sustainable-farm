package com.infineonbit.sustainablefarm.modules.producttransformation.block;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from producttransformation to the blocks of the site referential (ADR 0004), added
 * by {@code core.schema.CrossModuleForeignKeyInstaller}: a block that a lot, a harvest event or a
 * raw intake comes from cannot be deleted.
 */
@Configuration
public class ProductTransformationBlockForeignKeys {

    @Bean
    CrossModuleForeignKey batchBlockForeignKey() {
        return toBlock("fk_batch_block", "batch", "block_id");
    }

    @Bean
    CrossModuleForeignKey harvestEventBlockForeignKey() {
        return toBlock("fk_harvest_event_block", "harvest_event", "block_id");
    }

    @Bean
    CrossModuleForeignKey rawIntakeSourceBlockForeignKey() {
        return toBlock("fk_raw_intake_source_block", "raw_intake", "source_block_id");
    }

    private static CrossModuleForeignKey toBlock(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, FarmDirectory.BLOCK_TABLE, FarmDirectory.KEY_COLUMN);
    }
}
