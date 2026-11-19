package com.infineonbit.sustainablefarm.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The demo data set ({@code seed/demo-data.sql}) loads on the baseline schema, fills every
 * table of the integrated modules, links the modules through the same rows, and is loaded once.
 *
 * <p>PostgreSQL only, like {@code BaselineMigrationTest} (the script and the migration are
 * PostgreSQL SQL): skipped unless {@code SPRING_DATASOURCE_URL} points to PostgreSQL. The
 * application starts as in development: Flyway on its own empty schema, {@code ddl-auto=validate},
 * profile {@code dev}, so {@link DemoDataSeeder} runs at startup.</p>
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=" + DemoDataSetTest.SCHEMA,
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.schema.foreign-keys=verify",
        "app.irrigation.auto-trigger-enabled=false"
})
@ActiveProfiles("dev")
class DemoDataSetTest {

    static final String SCHEMA = "demo_data_check";

    /** The entities of the application only, as in BaselineMigrationTest. */
    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackages = "com.infineonbit.sustainablefarm.modules")
    static class ApplicationEntitiesOnly {
    }

    private static final String URL = System.getenv("SPRING_DATASOURCE_URL");

    /**
     * Tables of the integrated modules that the data set leaves empty on purpose: the IoT valve
     * commands are written by the valve gateway, the growth phase shares are corrections of
     * defaults the code holds. The excluded modules (sitesecurity, cropstorage) are not checked.
     */
    private static final List<String> EMPTY_ON_PURPOSE = List.of("iot_commands", "growth_phase_yield_share");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DemoDataSeeder seeder;

    @BeforeAll
    static void onPostgresqlOnly_startFromAnEmptySchema() throws SQLException {
        assumeTrue(URL != null && URL.startsWith("jdbc:postgresql:"),
                "the data set is PostgreSQL SQL: set SPRING_DATASOURCE_URL to a PostgreSQL database");
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
    void everyTableOfTheIntegratedModules_hasRows() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'"
                        + " AND table_name <> 'flyway_schema_history' AND table_name NOT LIKE 'site_security%'"
                        + " AND table_name <> 'zone_entity_requirements' ORDER BY table_name",
                String.class, SCHEMA);
        assertThat(tables).hasSizeGreaterThan(80);

        for (String table : tables) {
            Integer rows = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
            if (EMPTY_ON_PURPOSE.contains(table)) {
                assertThat(rows).as("rows in %s", table).isZero();
            } else {
                assertThat(rows).as("rows in %s", table).isPositive();
            }
        }
    }

    @Test
    void theModules_shareTheSameFarmPeopleMachinesAndLots() {
        // One farm, every block in it, every zone on a block.
        assertThat(count("SELECT count(*) FROM farm")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM block b JOIN farm f ON f.id = b.farm_id")).isEqualTo(3);

        // Every seeded person is in the staff directory (shown on the Staff screen).
        assertThat(count("SELECT count(*) FROM person p WHERE p.email LIKE '%@ferme-banfora.example'"
                + " AND NOT EXISTS (SELECT 1 FROM staff s WHERE s.person_id = p.id)")).isZero();

        // A product transformation batch per plants harvest lot, with the same code and block.
        assertThat(count("SELECT count(*) FROM batch")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM batch b JOIN harvest_record h ON h.lot_number = b.code"
                + " JOIN harvest_event e ON e.batch_code = b.code AND e.plants_harvest_id = h.id"
                + " JOIN block_variety bv ON bv.id = h.block_variety_id AND bv.block_id = b.block_id")).isEqualTo(5);

        // Each raw intake: a block of the farm, a catalogue variety, a person of the directory.
        assertThat(count("SELECT count(*) FROM raw_intake r JOIN block b ON b.id = r.source_block_id"
                + " JOIN variety_reference v ON v.variety_key = r.received_variety_key"
                + " JOIN person p ON p.id = r.intake_operator_id")).isEqualTo(5);

        // The drying runs are on machinery dryers, washing and packaging on processing machines.
        assertThat(count("SELECT count(*) FROM drying_run d JOIN equipment e ON e.id = d.equipment_id"
                + " AND e.stage = 'DRYING'")).isEqualTo(count("SELECT count(*) FROM drying_run"));
        assertThat(count("SELECT count(*) FROM wash_sort_record w JOIN equipment e ON e.id = w.equipment_id"
                + " AND e.stage = 'PROCESSING'")).isEqualTo(count("SELECT count(*) FROM wash_sort_record"));

        // The sales stock is the packaged lots of product transformation.
        assertThat(count("SELECT count(*) FROM product_batches pb JOIN packaging_record pr"
                + " ON pr.id = pb.packaging_record_id AND pr.lot_code = pb.batch_code"))
                .isEqualTo(count("SELECT count(*) FROM product_batches")).isPositive();

        // The washing water of a lot points to its batch.
        assertThat(count("SELECT count(*) FROM water_consumption w JOIN batch b ON b.id = w.processing_batch_id"
                + " AND b.code = w.lot_id WHERE w.activity = 'WASHING'"))
                .isEqualTo(count("SELECT count(*) FROM wash_sort_record"));

        // The plant protection treatment: a machinery sprayer, applied by a person of the directory.
        assertThat(count("SELECT count(*) FROM health_treatment t JOIN equipment e ON e.id = t.equipment_id"
                + " AND e.name = t.equipment JOIN person p ON p.id = t.applicator_person_id")).isEqualTo(1);

        // Machinery operators and product transformation operators are people of the directory.
        assertThat(count("SELECT count(*) FROM operator o JOIN staff s ON s.person_id = o.person_id"))
                .isEqualTo(count("SELECT count(*) FROM operator"));
    }

    @Test
    void theSeeder_loadsTheDataSetOnce() {
        Map<String, Integer> before = counts();

        seeder.run(new DefaultApplicationArguments());

        assertThat(counts()).isEqualTo(before);
    }

    private Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : List.of("person", "farm", "block", "zone", "equipment", "harvest_record", "batch",
                "energy_component", "water_sources", "products", "customers", "visitor", "alert")) {
            counts.put(table, count("SELECT count(*) FROM " + table));
        }
        return counts;
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
