package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.User;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the technical IoT account at application startup.
 *
 * <p>Why this bean exists: {@code irrigation_schedules.created_by} is mandatory and
 * {@code notifications.user_id} points to {@code users(id)}. The watersupply module has
 * no authentication endpoint - it is delegated to the global software - so this account
 * has no login credentials and cannot be used to log in.
 *
 * <p>This is not demo data: the row is required in production, hence the
 * {@code !test} profile (not {@code dev}). The Flyway migrations only carry the schema
 * (PR-33): the two V2 constraints toward {@code users} are added {@code NOT VALID}
 * precisely because this row does not exist yet when Flyway runs.
 *
 * <p>Under the {@code test} profile, it is {@code TestSystemUserSeeder} that provides the
 * same row: the tests disable Flyway and work on H2.
 *
 * <p>Idempotent execution: the row is created only once.
 */
@Component
@Profile("!test")
public class SystemUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    public SystemUserSeeder(UserRepository userRepository) {
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
        system.setStatus(false);
        userRepository.save(system);
    }
}
