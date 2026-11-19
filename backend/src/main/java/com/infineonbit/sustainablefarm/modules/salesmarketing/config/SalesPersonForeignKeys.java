package com.infineonbit.sustainablefarm.modules.salesmarketing.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from the author of a demand report to the people directory of the staff module,
 * added by {@code core.schema.CrossModuleForeignKeyInstaller}.
 */
@Configuration
public class SalesPersonForeignKeys {

    @Bean
    CrossModuleForeignKey demandReportGeneratedByForeignKey() {
        return new CrossModuleForeignKey("fk_demand_reports_generated_by_person", "demand_reports",
                "generated_by_person_id", PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
