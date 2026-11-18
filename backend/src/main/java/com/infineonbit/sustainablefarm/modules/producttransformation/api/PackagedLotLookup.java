package com.infineonbit.sustainablefarm.modules.producttransformation.api;

import java.util.Optional;

/**
 * Read access to the packaged lots of product transformation (the packaging records and their
 * lot codes), for the other modules (docs/adr/0003-communication-entre-modules.md).
 *
 * <p>A module that designates a packaged lot by its lot code keeps that code as it is and, next
 * to it, a nullable column holding the {@link PackagedLot#recordId() record id} of the lot with
 * that code, with a foreign key to {@link #TABLE}({@link #KEY_COLUMN}). The link is resolved when
 * the module writes its row; nothing is synchronised afterwards.
 */
@FunctionalInterface
public interface PackagedLotLookup {

    /** Table of the packaged lots, the target of the foreign keys of the other modules. */
    String TABLE = "packaging_record";

    /** Primary key column of {@link #TABLE}, referenced by the other modules. */
    String KEY_COLUMN = "record_id";

    /** Maximum length of a {@link #KEY_COLUMN} value, for the columns that store one. */
    int KEY_LENGTH = 50;

    /**
     * Finds the packaged lot of a lot code.
     *
     * @param lotCode the exact lot code, for example {@code "LOT-DEMO-003-PKG"}, surrounding
     *                spaces ignored; possibly {@code null}
     * @return the lot, or empty when no packaging record has this code or the code is blank
     */
    Optional<PackagedLot> findByLotCode(String lotCode);
}
