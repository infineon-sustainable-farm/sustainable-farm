package com.infineonbit.sustainablefarm.modules.producttransformation.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Loads the product transformation demo dataset at startup, {@code dev} profile only,
 * like {@code WatersupplyDemoDataSeeder}.
 *
 * <p>The data is synthetic (every identifier carries {@code DEMO}) and must not reach
 * production, which is why it is a seed script and not a Flyway migration.</p>
 *
 * <p>The script is idempotent: every row has a fixed key and every insert ends with
 * {@code ON CONFLICT DO NOTHING}, so it is replayed at every startup without duplicating
 * rows. It requires PostgreSQL ({@code generate_series}, {@code interval}).</p>
 */
@Component
@Profile("dev")
public class ProductTransformationDemoDataSeeder implements ApplicationRunner {

    static final Resource SCRIPT = new ClassPathResource("seed/producttransformation-demo-seed.sql");

    private final DataSource dataSource;

    public ProductTransformationDemoDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
    }
}
