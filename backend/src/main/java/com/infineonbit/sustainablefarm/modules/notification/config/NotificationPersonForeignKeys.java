package com.infineonbit.sustainablefarm.modules.notification.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The recipient of an alert is a person of the staff referential (docs/adr/0004). */
@Configuration
public class NotificationPersonForeignKeys {

    @Bean
    CrossModuleForeignKey alertRecipientPersonForeignKey() {
        return new CrossModuleForeignKey("fk_alert_recipient_person", AlertBoard.TABLE, "recipient_person_id",
                PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
