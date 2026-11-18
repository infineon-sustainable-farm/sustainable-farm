package com.infineonbit.sustainablefarm.modules.plants.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from the block and farm columns of plants to the farm referential, added by
 * {@code core.schema.CrossModuleForeignKeyInstaller} (docs/adr/0004). A block or a farm that
 * plants rows still reference cannot be deleted.
 */
@Configuration
public class PlantsFarmForeignKeys {

    @Bean
    CrossModuleForeignKey varietyBlockForeignKey() {
        return toBlock("fk_varietes_block", "varietes", "block_id");
    }

    @Bean
    CrossModuleForeignKey growthCalendarBlockForeignKey() {
        return toBlock("fk_calendrier_croissance_block", "calendrier_croissance", "block_id");
    }

    @Bean
    CrossModuleForeignKey healthInspectionBlockForeignKey() {
        return toBlock("fk_health_inspection_block", "health_inspection", "block_id");
    }

    @Bean
    CrossModuleForeignKey healthTreatmentBlockForeignKey() {
        return toBlock("fk_health_treatment_block", "health_treatment", "block_id");
    }

    @Bean
    CrossModuleForeignKey fertilizerMovementBlockForeignKey() {
        return toBlock("fk_fertilizer_movement_block", "fertilizer_movement", "block_id");
    }

    @Bean
    CrossModuleForeignKey nurseryEventBlockForeignKey() {
        return toBlock("fk_nursery_event_block", "nursery_event", "block_id");
    }

    @Bean
    CrossModuleForeignKey nurseryBatchPlannedBlockForeignKey() {
        return toBlock("fk_nursery_batch_planned_block", "nursery_batch", "planned_block_id");
    }

    @Bean
    CrossModuleForeignKey nurseryBatchFarmForeignKey() {
        return new CrossModuleForeignKey("fk_nursery_batch_farm", "nursery_batch", "farm_id",
                FarmDirectory.FARM_TABLE, FarmDirectory.KEY_COLUMN);
    }

    private static CrossModuleForeignKey toBlock(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, FarmDirectory.BLOCK_TABLE, FarmDirectory.KEY_COLUMN);
    }
}
