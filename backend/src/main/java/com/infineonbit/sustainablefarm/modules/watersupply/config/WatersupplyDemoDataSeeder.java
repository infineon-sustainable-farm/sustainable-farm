package com.infineonbit.sustainablefarm.modules.watersupply.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Loads the water supply demo dataset at startup, {@code dev} profile only.
 *
 * <p>Why this bean exists: the Flyway migrations must carry neither accounts nor
 * demo data (PR-33) - Flyway has no profile mechanism, those values would ship to
 * production. The script therefore lives in {@code resources/seed/} and only runs under
 * the {@code dev} profile, like {@code DevDataSeeder} and
 * {@code PlantsDataSeeder}.
 *
 * <p>The script is idempotent (clauses {@code ON CONFLICT ... DO NOTHING}): it can be
 * replayed at every startup without duplicating rows. It requires PostgreSQL, like the
 * rest of the watersupply module migrations.
 */
@Component
@Profile("dev")
public class WatersupplyDemoDataSeeder implements ApplicationRunner {

    private static final Resource SCRIPT = new ClassPathResource("seed/watersupply-demo-data.sql");

    private final DataSource dataSource;

    public WatersupplyDemoDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
    }
}
