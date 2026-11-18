package com.infineonbit.sustainablefarm.modules.producttransformation.variety;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from the variety columns of producttransformation to the plants
 * variety catalogue, added by {@code core.schema.CrossModuleForeignKeyInstaller}.
 */
@Configuration
public class ProductTransformationVarietyForeignKeys {

    @Bean
    CrossModuleForeignKey batchVarietyForeignKey() {
        return toCatalogue("fk_batch_variety", "batch", "variety_key");
    }

    @Bean
    CrossModuleForeignKey harvestEventVarietyForeignKey() {
        return toCatalogue("fk_harvest_event_variety", "harvest_event", "variety_key");
    }

    @Bean
    CrossModuleForeignKey rawIntakeVarietyForeignKey() {
        return toCatalogue("fk_raw_intake_received_variety", "raw_intake", "received_variety_key");
    }

    @Bean
    CrossModuleForeignKey historicalHarvestVarietyForeignKey() {
        return toCatalogue("fk_historical_harvest_variety", "historical_harvest", "variety_key");
    }

    private static CrossModuleForeignKey toCatalogue(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, VarietyCatalog.TABLE, VarietyCatalog.KEY_COLUMN);
    }
}
