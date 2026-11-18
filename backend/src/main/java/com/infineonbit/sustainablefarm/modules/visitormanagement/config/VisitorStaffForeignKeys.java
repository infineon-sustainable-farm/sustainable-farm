package com.infineonbit.sustainablefarm.modules.visitormanagement.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from visitor management to the people referential ({@code person}, ADR 0004),
 * added by {@code core.schema.CrossModuleForeignKeyInstaller}: a person still referenced by a
 * staff row, a briefing or a workshop cannot be deleted.
 */
@Configuration
public class VisitorStaffForeignKeys {

    @Bean
    CrossModuleForeignKey staffPersonForeignKey() {
        return toPerson("fk_staff_person", "staff", "person_id");
    }

    @Bean
    CrossModuleForeignKey briefingStaffMemberPersonForeignKey() {
        return toPerson("fk_briefing_staff_member_person", "briefing", "staff_member_person_id");
    }

    @Bean
    CrossModuleForeignKey workshopFacilitatorPersonForeignKey() {
        return toPerson("fk_workshop_facilitator_person", "workshop", "facilitator_person_id");
    }

    private static CrossModuleForeignKey toPerson(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
