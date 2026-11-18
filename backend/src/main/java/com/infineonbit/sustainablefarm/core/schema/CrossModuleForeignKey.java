package com.infineonbit.sustainablefarm.core.schema;

/**
 * A foreign key from the table of one module to the table of another.
 *
 * <p>Hibernate builds the schema from the entities (docs/adr/0002), and declares a
 * foreign key only on an association to another entity. A module never maps the
 * entity of another module (docs/adr/0003): it stores the key of the referenced row
 * in a plain column. Such a column declares its foreign key as a bean of this type,
 * and {@link CrossModuleForeignKeyInstaller} adds the constraint once Hibernate has
 * built the tables.
 *
 * @param name             name of the constraint, for example {@code fk_batch_variety}
 * @param table            the referencing table
 * @param column           the referencing column
 * @param referencedTable  the referenced table, owned by another module
 * @param referencedColumn the referenced column: the primary key or a unique column
 * @param onDelete         what the database does to the referencing rows when the
 *                         referenced row is deleted
 */
public record CrossModuleForeignKey(String name, String table, String column,
                                    String referencedTable, String referencedColumn,
                                    OnDelete onDelete) {

    public CrossModuleForeignKey {
        if (onDelete == null) {
            onDelete = OnDelete.NO_ACTION;
        }
    }

    /** A foreign key that refuses the deletion of a referenced row while rows reference it. */
    public CrossModuleForeignKey(String name, String table, String column,
                                 String referencedTable, String referencedColumn) {
        this(name, table, column, referencedTable, referencedColumn, OnDelete.NO_ACTION);
    }

    /** The {@code ON DELETE} action of the constraint. */
    public enum OnDelete {
        /** The deletion of a referenced row fails while rows reference it. */
        NO_ACTION("NO ACTION"),
        /** The referencing rows are deleted with the referenced row, for a 1:1 extension. */
        CASCADE("CASCADE");

        private final String sql;

        OnDelete(String sql) {
            this.sql = sql;
        }

        public String sql() {
            return sql;
        }
    }
}
