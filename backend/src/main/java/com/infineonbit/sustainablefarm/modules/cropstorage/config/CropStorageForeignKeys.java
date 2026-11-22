package com.infineonbit.sustainablefarm.modules.cropstorage.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from Crop Storage to the concepts other modules own (ADR 0004), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller} and created by the migration
 * V2__cropstorage_init.sql: the variety of a batch is a key of the Plants catalogue, its
 * harvest a Plants harvest record, the inspector of a quality check a person of the staff
 * directory. Crop Storage never maps the entities of those modules.
 */
@Configuration
public class CropStorageForeignKeys {

    @Bean
    CrossModuleForeignKey cropStorageBatchVarietyForeignKey() {
        return new CrossModuleForeignKey("fk_crop_storage_batch_variety", "crop_storage_batch", "variety_key",
                VarietyCatalog.TABLE, VarietyCatalog.KEY_COLUMN);
    }

    @Bean
    CrossModuleForeignKey cropStorageBatchHarvestForeignKey() {
        return new CrossModuleForeignKey("fk_crop_storage_batch_harvest", "crop_storage_batch", "harvest_id",
                HarvestLookup.TABLE, HarvestLookup.KEY_COLUMN);
    }

    @Bean
    CrossModuleForeignKey qualityCheckInspectorForeignKey() {
        return new CrossModuleForeignKey("fk_quality_check_inspector_person", "quality_check",
                "inspector_person_id", PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
