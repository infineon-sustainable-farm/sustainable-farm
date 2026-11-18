package com.infineonbit.sustainablefarm.modules.producttransformation.operator;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from product transformation to the people referential of the staff module
 * (ADR 0004), added by {@code core.schema.CrossModuleForeignKeyInstaller}.
 *
 * <ul>
 *   <li>{@code operator}, the product transformation role of a person, refuses the deletion of
 *       its person: the drying, wash and sort, packaging, QC and compliance records reference
 *       the operator.</li>
 *   <li>The intake operator and the harvest supervisor, free text before, name a person.</li>
 * </ul>
 */
@Configuration
public class ProductTransformationPersonForeignKeys {

    @Bean
    CrossModuleForeignKey operatorPersonForeignKey() {
        return toPerson("fk_operator_person", "operator", "person_id");
    }

    @Bean
    CrossModuleForeignKey rawIntakeOperatorForeignKey() {
        return toPerson("fk_raw_intake_operator_person", "raw_intake", "intake_operator_id");
    }

    @Bean
    CrossModuleForeignKey harvestEventSupervisorForeignKey() {
        return toPerson("fk_harvest_event_supervisor_person", "harvest_event", "harvest_supervisor_id");
    }

    private static CrossModuleForeignKey toPerson(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
