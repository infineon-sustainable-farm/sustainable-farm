package com.infineonbit.sustainablefarm.core.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The Flyway migrations build the schema the entities expect (docs/adr/0002, AC-12).
 *
 * <p>Runs on PostgreSQL only (the migrations are PostgreSQL SQL): skipped unless
 * {@code SPRING_DATASOURCE_URL} points to PostgreSQL, as in the CI. It starts the application
 * as it runs for real: Flyway on, {@code ddl-auto=validate}, cross-module foreign keys verified,
 * profile {@code dev} so that every seeder writes its rows. The context starting is the main
 * assertion: a schema that differs from the entities, or a declared foreign key missing from
 * the migration, fails the startup.</p>
 *
 * <p>The other tests build their schema in {@code public} with {@code ddl-auto=create}, on the
 * same database. This one uses its own schema, dropped first, so that Flyway always starts
 * from an empty one.</p>
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=" + BaselineMigrationTest.SCHEMA,
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.schema.foreign-keys=verify"
})
@ActiveProfiles("dev")
class BaselineMigrationTest {

    static final String SCHEMA = "baseline_check";

    /**
     * The entities of the application only. The default scan covers the whole package, test
     * classes included, and would ask the schema for test-only entities such as the
     * {@code money_probe} of MoneyEmbeddableTest, which no migration creates.
     */
    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackages = "com.infineonbit.sustainablefarm.modules")
    static class ApplicationEntitiesOnly {
    }

    private static final String URL = System.getenv("SPRING_DATASOURCE_URL");

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void onPostgresqlOnly_startFromAnEmptySchema() throws SQLException {
        assumeTrue(URL != null && URL.startsWith("jdbc:postgresql:"),
                "the migrations are PostgreSQL SQL: set SPRING_DATASOURCE_URL to a PostgreSQL database");
        try (Connection connection = DriverManager.getConnection(URL,
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @DynamicPropertySource
    static void ownSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
    }

    @Test
    void flywayAppliesTheBaselineOnAnEmptySchema() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");

        assertThat(history).isNotEmpty();
        assertThat(history.get(0)).containsEntry("version", "1");
        assertThat(history).allSatisfy(row -> assertThat(row).containsEntry("success", true));
    }

    @Test
    void theSchemaHoldsTheTablesOfTheModel() {
        Integer tables = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'"
                        + " AND table_name <> 'flyway_schema_history'", Integer.class, SCHEMA);

        assertThat(tables).isGreaterThan(90);
    }

    @Test
    void theReferenceDataAndTheDevSeedsAreLoaded() {
        for (String table : List.of("variety_reference", "person", "farm", "block", "zone", "water_sources",
                "energy_component", "equipment", "batch", "time_slot", "health_issue_reference")) {
            Integer rows = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
            assertThat(rows).as("rows in %s", table).isPositive();
        }
    }
}
