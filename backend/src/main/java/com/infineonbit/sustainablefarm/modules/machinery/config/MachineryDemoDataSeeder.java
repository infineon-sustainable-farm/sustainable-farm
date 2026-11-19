package com.infineonbit.sustainablefarm.modules.machinery.config;

import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentLookup;
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
 * Loads the demo machines of the equipment register at startup, {@code dev} profile only, like
 * the other demo seeders.
 *
 * <p>Machinery owns the register: the processing and drying machines the product transformation
 * demo data uses are written here, and the product transformation seed only adds their
 * attributes. It runs before it ({@link EquipmentLookup#DEMO_SEED_ORDER}). The script is
 * idempotent ({@code ON CONFLICT DO NOTHING}).</p>
 */
@Component
@Profile("dev")
@Order(EquipmentLookup.DEMO_SEED_ORDER)
public class MachineryDemoDataSeeder implements ApplicationRunner {

    static final Resource SCRIPT = new ClassPathResource("seed/machinery-demo-data.sql");

    private final DataSource dataSource;

    public MachineryDemoDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
    }
}
