package com.infineonbit.sustainablefarm.modules.producttransformation.lot;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from a harvest event to the plants harvest record it was imported from
 * ({@code harvest_event.plants_harvest_id}, nullable), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller}. Plants never deletes a harvest record;
 * the key refuses it while a harvest event references it.
 */
@Configuration
public class ProductTransformationHarvestForeignKeys {

    @Bean
    CrossModuleForeignKey harvestEventPlantsHarvestForeignKey() {
        return new CrossModuleForeignKey("fk_harvest_event_plants_harvest", "harvest_event", "plants_harvest_id",
                HarvestLookup.TABLE, HarvestLookup.KEY_COLUMN);
    }
}
