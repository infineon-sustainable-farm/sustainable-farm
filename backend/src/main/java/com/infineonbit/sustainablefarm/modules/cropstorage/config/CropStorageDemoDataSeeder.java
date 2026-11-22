package com.infineonbit.sustainablefarm.modules.cropstorage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Loads the fictional demo data of Crop Storage, {@code seed/cropstorage-demo-data.sql}, in the
 * {@code dev} profile only, like core.config.DemoDataSeeder does for the shared data set. It
 * references only the Plants variety catalogue, loaded before any runner, so its order among
 * the runners does not matter. One transaction, and only when storage_zone is empty: a restart
 * or a database where someone entered a zone gets no row from it.
 */
@Component
@Profile("dev")
@Order(Ordered.LOWEST_PRECEDENCE)
public class CropStorageDemoDataSeeder implements ApplicationRunner {

    /** The Crop Storage demo data set. */
    public static final Resource SCRIPT = new ClassPathResource("seed/cropstorage-demo-data.sql");

    private static final Logger log = LoggerFactory.getLogger(CropStorageDemoDataSeeder.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public CropStorageDemoDataSeeder(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean loaded = transaction.execute(status -> {
            Integer zones = jdbc.queryForObject("SELECT count(*) FROM storage_zone", Integer.class);
            if (zones != null && zones > 0) {
                return false;
            }
            new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
            return true;
        });
        if (Boolean.TRUE.equals(loaded)) {
            log.info("Crop Storage demo data loaded ({})", SCRIPT.getFilename());
        } else {
            log.info("Crop Storage demo data not loaded: the database already has a storage zone");
        }
    }
}
