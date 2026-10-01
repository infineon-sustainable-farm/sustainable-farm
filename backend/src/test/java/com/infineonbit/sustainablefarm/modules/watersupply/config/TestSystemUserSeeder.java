package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.User;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the technical IoT account under the {@code test} profile.
 *
 * <p>In production this row comes from the Flyway migrations
 * ({@code V2}/{@code V4}). Tests run with {@code spring.flyway.enabled=false}
 * because those migrations are PostgreSQL-only SQL that H2 rejects, so the row
 * is created here instead. Without it, {@code notifications.user_id} and
 * {@code irrigation_schedules.created_by} have no valid target and the
 * automatic-alert tests fail with "User not found".
 *
 * <p>Never active outside tests: the {@code test} profile is the only trigger.
 */
@Component
@Profile("test")
public class TestSystemUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    public TestSystemUserSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findById(SystemUsers.IOT_SYSTEM_USER_ID).isPresent()) {
            return;
        }
        User system = new User();
        system.setId(SystemUsers.IOT_SYSTEM_USER_ID);
        system.setFirstName("Systeme");
        system.setLastName("IoT");
        system.setEmail("systeme.iot@watersupply.local");
        system.setPasswordHash("NO_LOGIN_SYSTEM_ACCOUNT");
        system.setStatus(false);
        userRepository.save(system);
    }
}
