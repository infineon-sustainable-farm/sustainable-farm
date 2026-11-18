package com.infineonbit.sustainablefarm.modules.salesmarketing.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey.OnDelete;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLotLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from a sales batch to the product transformation packaged lot it designates
 * ({@code product_batches.packaging_record_id}, nullable), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller}. The batch keeps its own code, so deleting
 * the packaged lot only clears the link, as product transformation can do it today.
 */
@Configuration
public class SalesPackagedLotForeignKeys {

    @Bean
    CrossModuleForeignKey productBatchPackagingRecordForeignKey() {
        return new CrossModuleForeignKey("fk_product_batches_packaging_record", "product_batches",
                "packaging_record_id", PackagedLotLookup.TABLE, PackagedLotLookup.KEY_COLUMN, OnDelete.SET_NULL);
    }
}
