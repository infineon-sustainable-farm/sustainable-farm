package com.infineonbit.sustainablefarm.core.schema;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Adds the {@link CrossModuleForeignKey} constraints declared by the modules.
 *
 * <p>It runs once every singleton is created, so after Hibernate has built the
 * schema ({@code ddl-auto}) and before the application and command line runners,
 * the seeders among them, write any row. A constraint that already exists is left
 * as it is, so a restart on the same database adds nothing. A constraint whose
 * tables do not exist is skipped with a warning.
 *
 * <p>The SQL is standard ({@code ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY}),
 * and existing constraints are read from the JDBC metadata, so it runs the same on
 * PostgreSQL and on H2. Once the schema comes from the Flyway baseline (T46), the
 * baseline declares these constraints and this class finds them in place.
 */
@Component
public class CrossModuleForeignKeyInstaller implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CrossModuleForeignKeyInstaller.class);

    private final DataSource dataSource;
    private final List<CrossModuleForeignKey> foreignKeys;

    /**
     * @param dataSource           the application database
     * @param entityManagerFactory not used: it makes this bean wait for Hibernate
     *                             to build the schema
     * @param foreignKeys          the foreign keys declared by the modules, possibly none
     */
    public CrossModuleForeignKeyInstaller(DataSource dataSource,
                                          @SuppressWarnings("unused") EntityManagerFactory entityManagerFactory,
                                          ObjectProvider<CrossModuleForeignKey> foreignKeys) {
        this.dataSource = dataSource;
        this.foreignKeys = foreignKeys.orderedStream().toList();
    }

    @Override
    public void afterSingletonsInstantiated() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (CrossModuleForeignKey foreignKey : foreignKeys) {
            install(jdbc, foreignKey);
        }
    }

    private void install(JdbcTemplate jdbc, CrossModuleForeignKey foreignKey) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!tableExists(metaData, foreignKey.table()) || !tableExists(metaData, foreignKey.referencedTable())) {
                log.warn("Foreign key {} skipped: table {} or {} does not exist",
                        foreignKey.name(), foreignKey.table(), foreignKey.referencedTable());
                return;
            }
            if (constraintExists(metaData, foreignKey)) {
                return;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read the schema for the foreign key " + foreignKey.name(), e);
        }
        jdbc.execute("ALTER TABLE " + foreignKey.table()
                + " ADD CONSTRAINT " + foreignKey.name()
                + " FOREIGN KEY (" + foreignKey.column() + ")"
                + " REFERENCES " + foreignKey.referencedTable() + " (" + foreignKey.referencedColumn() + ")");
        log.info("Added the foreign key {} on {}({}) to {}({})", foreignKey.name(), foreignKey.table(),
                foreignKey.column(), foreignKey.referencedTable(), foreignKey.referencedColumn());
    }

    private static boolean tableExists(DatabaseMetaData metaData, String table) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, identifier(metaData, table), new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private static boolean constraintExists(DatabaseMetaData metaData, CrossModuleForeignKey foreignKey)
            throws SQLException {
        try (ResultSet keys = metaData.getImportedKeys(null, null, identifier(metaData, foreignKey.table()))) {
            while (keys.next()) {
                String name = keys.getString("FK_NAME");
                if (name != null && name.equalsIgnoreCase(foreignKey.name())) {
                    return true;
                }
            }
            return false;
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
