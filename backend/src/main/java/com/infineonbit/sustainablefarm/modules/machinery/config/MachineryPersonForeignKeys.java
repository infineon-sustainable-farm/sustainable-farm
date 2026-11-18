package com.infineonbit.sustainablefarm.modules.machinery.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from machinery to the people referential of the staff module (ADR 0004), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller}: a person still assigned to a machine or
 * named in a log cannot be deleted.
 */
@Configuration
public class MachineryPersonForeignKeys {

    @Bean
    CrossModuleForeignKey operatorAssignmentPersonForeignKey() {
        return toPerson("fk_operator_assignment_person", "operator_assignment", "person_id");
    }

    @Bean
    CrossModuleForeignKey maintenanceScheduleOperatorForeignKey() {
        return toPerson("fk_maintenance_schedule_operator_person", "maintenance_schedule", "operator_person_id");
    }

    @Bean
    CrossModuleForeignKey usageLogOperatorForeignKey() {
        return toPerson("fk_usage_log_operator_person", "usage_log", "operator_person_id");
    }

    @Bean
    CrossModuleForeignKey repairLogTechnicianForeignKey() {
        return toPerson("fk_repair_log_technician_person", "repair_log", "technician_person_id");
    }

    private static CrossModuleForeignKey toPerson(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
