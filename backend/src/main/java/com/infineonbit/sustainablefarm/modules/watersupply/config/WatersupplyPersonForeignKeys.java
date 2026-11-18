package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from the person columns of Water Supply to the staff referential,
 * added by {@code core.schema.CrossModuleForeignKeyInstaller} (docs/adr/0004).
 */
@Configuration
public class WatersupplyPersonForeignKeys {

    @Bean
    CrossModuleForeignKey notificationUserPersonForeignKey() {
        return toPerson("fk_notifications_user_person", "notifications", "user_id");
    }

    @Bean
    CrossModuleForeignKey irrigationScheduleCreatedByPersonForeignKey() {
        return toPerson("fk_irrigation_schedules_created_by_person", "irrigation_schedules", "created_by");
    }

    @Bean
    CrossModuleForeignKey iotCommandRequestedByPersonForeignKey() {
        return toPerson("fk_iot_commands_requested_by_person", "iot_commands", "requested_by");
    }

    @Bean
    CrossModuleForeignKey dripMaintenanceLogPerformedByPersonForeignKey() {
        return toPerson("fk_drip_maintenance_logs_performed_by_person", "drip_maintenance_logs", "performed_by_person_id");
    }

    private static CrossModuleForeignKey toPerson(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
