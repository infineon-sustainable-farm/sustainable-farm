package com.infineonbit.sustainablefarm.core.config;

import javax.sql.DataSource;
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

/**
 * Loads the one demo data set of every module, {@code seed/demo-data.sql}, at startup in the
 * {@code dev} profile only: never in production nor in the tests.
 *
 * <p>It runs last among the startup runners, after the reference data that every profile
 * loads (the plants variety catalogue, the IoT system person), which the data set references.
 * The script runs in one transaction, and only when the {@code farm} table is empty: a restart,
 * or a database where someone entered a farm, gets no row from it. It is PostgreSQL SQL.</p>
 */
@Component
@Profile("dev")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoDataSeeder implements ApplicationRunner {

    /** The demo data set. */
    public static final Resource SCRIPT = new ClassPathResource("seed/demo-data.sql");

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public DemoDataSeeder(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean loaded = transaction.execute(status -> {
            Integer farms = jdbc.queryForObject("SELECT count(*) FROM farm", Integer.class);
            if (farms != null && farms > 0) {
                return false;
            }
            new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
            return true;
        });
        if (Boolean.TRUE.equals(loaded)) {
            log.info("Demo data set loaded ({})", SCRIPT.getFilename());
        } else {
            log.info("Demo data set not loaded: the database already has a farm");
        }
    }
}
