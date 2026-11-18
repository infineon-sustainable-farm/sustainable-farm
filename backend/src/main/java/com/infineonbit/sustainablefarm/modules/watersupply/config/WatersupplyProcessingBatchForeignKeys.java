package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey.OnDelete;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.ProcessingBatchLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from a water consumption to the product transformation batch its lot designates
 * ({@code water_consumption.processing_batch_id}, nullable), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller}. The consumption keeps its own
 * {@code lot_id}, so deleting the batch only clears the link, as product transformation can do it
 * today.
 */
@Configuration
public class WatersupplyProcessingBatchForeignKeys {

    @Bean
    CrossModuleForeignKey waterConsumptionProcessingBatchForeignKey() {
        return new CrossModuleForeignKey("fk_water_consumption_processing_batch", "water_consumption",
                "processing_batch_id", ProcessingBatchLookup.TABLE, ProcessingBatchLookup.KEY_COLUMN,
                OnDelete.SET_NULL);
    }
}
