package com.infineonbit.sustainablefarm.modules.farm.config;

import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Loads the demo site referential (farm 1, blocks A, B and C) at startup, {@code dev} profile only,
 * like the other demo seeders.
 *
 * <p>It runs first ({@link FarmDirectory#DEMO_SEED_ORDER}): the seeds of Water Supply, Plants and
 * Product Transformation reference these rows, so the demo block A is the same {@code block} row
 * for the three modules. The script is idempotent ({@code ON CONFLICT DO NOTHING}) and requires
 * PostgreSQL ({@code pg_get_serial_sequence}).</p>
 */
@Component
@Profile("dev")
@Order(FarmDirectory.DEMO_SEED_ORDER)
public class FarmDemoDataSeeder implements ApplicationRunner {

    private static final Resource SCRIPT = new ClassPathResource("seed/farm-demo-data.sql");

    private final DataSource dataSource;

    public FarmDemoDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
    }
}
