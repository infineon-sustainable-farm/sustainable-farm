package com.infineonbit.sustainablefarm.modules.cropstorage;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.regex.Pattern;

/**
 * Refuses to start a Crop Storage test context unless it targets a private,
 * in-memory H2 database. It runs as an ApplicationContextInitializer, i.e. on
 * the fully resolved environment (test properties, environment variables,
 * application.properties) BEFORE any bean exists: no DataSource is created, no
 * connection is opened and no DDL runs against a real or shared database.
 *
 * <p>Use with {@code @ContextConfiguration(initializers = CropStorageH2IsolationGuard.class)}.
 */
public class CropStorageH2IsolationGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Only a named, in-memory H2 database reserved to Crop Storage tests. */
    static final Pattern ISOLATED_URL = Pattern.compile("^jdbc:h2:mem:crop_storage_[A-Za-z0-9_]+(;.*)?$");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        check(context.getEnvironment());
    }

    static void check(ConfigurableEnvironment env) {
        String url = env.getProperty("spring.datasource.url");
        if (url == null || !ISOLATED_URL.matcher(url).matches()) {
            throw new IllegalStateException(
                    "Crop Storage test refused: spring.datasource.url is not an isolated in-memory H2 database");
        }
        if (!"org.h2.Driver".equals(env.getProperty("spring.datasource.driver-class-name"))) {
            throw new IllegalStateException("Crop Storage test refused: the JDBC driver must be org.h2.Driver");
        }
        // Pool-level URLs are bound after the DataSource is built and would override the checked one.
        for (String key : new String[] {"spring.datasource.hikari.jdbc-url", "spring.datasource.jdbc-url"}) {
            String other = env.getProperty(key);
            if (other != null && !ISOLATED_URL.matcher(other).matches()) {
                throw new IllegalStateException("Crop Storage test refused: " + key + " is not isolated");
            }
        }
        if (env.getProperty("spring.flyway.enabled", Boolean.class, true)) {
            throw new IllegalStateException("Crop Storage test refused: Flyway must be disabled");
        }
    }
}
