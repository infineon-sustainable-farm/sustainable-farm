package com.infineonbit.sustainablefarm.core.schema;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks, or adds, the {@link CrossModuleForeignKey} constraints declared by the modules.
 *
 * <p>It runs once every singleton is created, so after the schema is in place and before the
 * application and command line runners, the seeders among them, write any row. Two modes, set
 * by {@code app.schema.foreign-keys}:</p>
 *
 * <ul>
 *   <li>{@code verify} (the application): the Flyway migrations create the schema, these
 *       constraints included. Each declared foreign key must exist on its column and point to
 *       the declared table and column; otherwise the startup fails with the list of the missing
 *       or different keys, as {@code ddl-auto=validate} does for the tables. A declared key
 *       without its migration is caught at the first startup, not in production data.</li>
 *   <li>{@code install} (the tests): Hibernate builds the schema from the entities
 *       ({@code ddl-auto=create}) and declares no foreign key between two modules, since a
 *       module never maps the entity of another one. The missing keys are added
 *       ({@code ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY}); an existing one is left as it
 *       is, a key whose tables do not exist is skipped with a warning.</li>
 * </ul>
 *
 * <p>Existing constraints are read from the JDBC metadata of the current schema of the
 * connection, so it runs the same on PostgreSQL and on H2.</p>
 */
@Component
public class CrossModuleForeignKeyInstaller implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CrossModuleForeignKeyInstaller.class);

    /** What to do with the declared foreign keys. */
    public enum Mode {
        /** Fail the startup when a declared foreign key is missing or different. */
        VERIFY,
        /** Add the declared foreign keys that are missing. */
        INSTALL
    }

    private static final String MISSING = "missing";
    private static final String NO_TABLE = "table missing";

    private final DataSource dataSource;
    private final List<CrossModuleForeignKey> foreignKeys;
    private final Mode mode;

    /**
     * @param dataSource           the application database
     * @param entityManagerFactory not used: it makes this bean wait for Hibernate
     *                             (and Flyway before it) to set up the schema
     * @param foreignKeys          the foreign keys declared by the modules, possibly none
     * @param mode                 {@code verify} (default) or {@code install}
     */
    public CrossModuleForeignKeyInstaller(DataSource dataSource,
                                          @SuppressWarnings("unused") EntityManagerFactory entityManagerFactory,
                                          ObjectProvider<CrossModuleForeignKey> foreignKeys,
                                          @Value("${app.schema.foreign-keys:verify}") String mode) {
        this.dataSource = dataSource;
        this.foreignKeys = foreignKeys.orderedStream().toList();
        this.mode = Mode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
    }

    @Override
    public void afterSingletonsInstantiated() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> problems = new ArrayList<>();
        for (CrossModuleForeignKey foreignKey : foreignKeys) {
            String problem = check(foreignKey);
            if (problem == null) {
                continue;
            }
            if (mode == Mode.INSTALL && problem.equals(MISSING)) {
                install(jdbc, foreignKey);
            } else if (mode == Mode.INSTALL && problem.equals(NO_TABLE)) {
                log.warn("Foreign key {} skipped: table {} or {} does not exist",
                        foreignKey.name(), foreignKey.table(), foreignKey.referencedTable());
            } else {
                problems.add(foreignKey.name() + " on " + foreignKey.table() + "(" + foreignKey.column()
                        + ") to " + foreignKey.referencedTable() + "(" + foreignKey.referencedColumn() + "): "
                        + problem);
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Cross-module foreign keys do not match the schema, add a migration: "
                    + String.join("; ", problems));
        }
        if (mode == Mode.VERIFY) {
            log.info("{} cross-module foreign keys verified", foreignKeys.size());
        }
    }

    /** Null when the key exists as declared, otherwise what is wrong. */
    private String check(CrossModuleForeignKey foreignKey) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            if (!tableExists(metaData, schema, foreignKey.table())
                    || !tableExists(metaData, schema, foreignKey.referencedTable())) {
                return NO_TABLE;
            }
            try (ResultSet keys = metaData.getImportedKeys(null, schema, identifier(metaData, foreignKey.table()))) {
                while (keys.next()) {
                    String name = keys.getString("FK_NAME");
                    if (name == null || !name.equalsIgnoreCase(foreignKey.name())) {
                        continue;
                    }
                    boolean same = keys.getString("FKCOLUMN_NAME").equalsIgnoreCase(foreignKey.column())
                            && keys.getString("PKTABLE_NAME").equalsIgnoreCase(foreignKey.referencedTable())
                            && keys.getString("PKCOLUMN_NAME").equalsIgnoreCase(foreignKey.referencedColumn());
                    return same ? null : "exists with another column or target";
                }
                return MISSING;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read the schema for the foreign key " + foreignKey.name(), e);
        }
    }

    private void install(JdbcTemplate jdbc, CrossModuleForeignKey foreignKey) {
        jdbc.execute("ALTER TABLE " + foreignKey.table()
                + " ADD CONSTRAINT " + foreignKey.name()
                + " FOREIGN KEY (" + foreignKey.column() + ")"
                + " REFERENCES " + foreignKey.referencedTable() + " (" + foreignKey.referencedColumn() + ")"
                + " ON DELETE " + foreignKey.onDelete().sql());
        log.info("Added the foreign key {} on {}({}) to {}({})", foreignKey.name(), foreignKey.table(),
                foreignKey.column(), foreignKey.referencedTable(), foreignKey.referencedColumn());
    }

    private static boolean tableExists(DatabaseMetaData metaData, String schema, String table) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, schema, identifier(metaData, table), new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    /** An unquoted identifier as the database stores it: upper case on H2, lower case on PostgreSQL. */
    private static String identifier(DatabaseMetaData metaData, String name) throws SQLException {
        if (metaData.storesUpperCaseIdentifiers()) {
            return name.toUpperCase(Locale.ROOT);
        }
        if (metaData.storesLowerCaseIdentifiers()) {
            return name.toLowerCase(Locale.ROOT);
        }
        return name;
    }
}
