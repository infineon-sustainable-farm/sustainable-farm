package com.infineonbit.sustainablefarm.modules.salesmarketing.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from the variety of a product to the plants variety catalogue,
 * added by {@code core.schema.CrossModuleForeignKeyInstaller}.
 */
@Configuration
public class SalesVarietyForeignKeys {

    @Bean
    CrossModuleForeignKey productVarietyForeignKey() {
        return new CrossModuleForeignKey("fk_products_variety", "products", "variety_key",
                VarietyCatalog.TABLE, VarietyCatalog.KEY_COLUMN);
    }
}
