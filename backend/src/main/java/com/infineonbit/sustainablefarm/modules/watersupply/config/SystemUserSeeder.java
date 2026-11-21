package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the technical IoT person at application startup, in every profile.
 *
 * <p>Why this bean exists: {@code irrigation_schedules.created_by} is mandatory and
 * {@code notifications.user_id} references {@code person(id)} (staff referential,
 * docs/adr/0004). The watersupply module has no authentication endpoint - it is delegated
 * to the global software - so this person has no login credentials and cannot be used to log in.
 *
 * <p>This is not demo data: the row is required in production and in the tests alike.
 * It runs first among the runners so that the seeders after it can reference it.
 *
 * <p>Idempotent execution: the person is found by its email {@link SystemUsers#IOT_SYSTEM_EMAIL}
 * and created only once.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemUserSeeder implements ApplicationRunner {

    private final SystemUsers systemUsers;

    public SystemUserSeeder(SystemUsers systemUsers) {
        this.systemUsers = systemUsers;
    }

    @Override
    public void run(ApplicationArguments args) {
        systemUsers.iotSystemUserId();
    }
}
