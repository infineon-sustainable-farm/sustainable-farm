package com.infineonbit.sustainablefarm.core.schema;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitConstraintNameSource;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Readable names for the constraints an entity does not name itself.
 *
 * <p>Hibernate names an unnamed foreign key or unique key with a hash
 * ({@code fk1vnklqq8v94sbcadhpw5yixcj}), which says nothing in an error message and
 * differs from the names in the Flyway baseline. This strategy derives the name from the
 * table and the columns instead:</p>
 *
 * <ul>
 *   <li>foreign key: {@code fk_<table>_<column without _id>}, for example
 *       {@code fk_booking_time_slot};</li>
 *   <li>unique key: {@code uk_<table>_<columns>}, for example {@code uk_spare_part_name};</li>
 *   <li>index: {@code idx_<table>_<columns>}.</li>
 * </ul>
 *
 * <p>A name given on the entity ({@code @ForeignKey}, {@code @UniqueConstraint},
 * {@code @Index}) is kept. A derived name longer than the 63 characters PostgreSQL allows
 * falls back to the Hibernate hash. Everything else is {@link SpringImplicitNamingStrategy}.
 * Enabled by {@code spring.jpa.hibernate.naming.implicit-strategy}.</p>
 */
public class ReadableConstraintNamingStrategy extends SpringImplicitNamingStrategy {

    private static final int MAX_IDENTIFIER_LENGTH = 63;

    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        if (source.getUserProvidedIdentifier() != null) {
            return super.determineForeignKeyName(source);
        }
        String columns = joined(source.getColumnNames());
        if (source.getColumnNames().size() == 1 && columns.endsWith("_id")) {
            columns = columns.substring(0, columns.length() - "_id".length());
        }
        return derived("fk", source, columns, () -> super.determineForeignKeyName(source));
    }

    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
        if (source.getUserProvidedIdentifier() != null) {
            return super.determineUniqueKeyName(source);
        }
        return derived("uk", source, joined(source.getColumnNames()), () -> super.determineUniqueKeyName(source));
    }

    @Override
    public Identifier determineIndexName(ImplicitIndexNameSource source) {
        if (source.getUserProvidedIdentifier() != null) {
            return super.determineIndexName(source);
        }
        return derived("idx", source, joined(source.getColumnNames()), () -> super.determineIndexName(source));
    }

    private static Identifier derived(String prefix, ImplicitConstraintNameSource source, String columns,
                                      Supplier<Identifier> fallback) {
        String name = prefix + "_" + source.getTableName().getText() + "_" + columns;
        if (name.length() > MAX_IDENTIFIER_LENGTH) {
            return fallback.get();
        }
        return Identifier.toIdentifier(name.toLowerCase(Locale.ROOT));
    }

    private static String joined(List<Identifier> columns) {
        return columns.stream().map(Identifier::getText).collect(Collectors.joining("_"));
    }
}
