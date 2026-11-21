package com.infineonbit.sustainablefarm.modules.producttransformation.api;

import java.util.Optional;

/**
 * Read access to the processing batches of product transformation (the lots that go through
 * intake, washing, drying and packaging), for the other modules
 * (docs/adr/0003-communication-entre-modules.md).
 *
 * <p>A module that designates a batch by its code keeps that code as it is and, next to it, a
 * nullable {@code bigint} column holding the {@link ProcessingBatch#id() id} of the batch when
 * such a batch exists, with a foreign key to {@link #TABLE}({@link #KEY_COLUMN}). The link is resolved
 * when the module writes its row; nothing is synchronised afterwards.
 */
@FunctionalInterface
public interface ProcessingBatchLookup {

    /** Table of the processing batches, the target of the foreign keys of the other modules. */
    String TABLE = "batch";

    /** Primary key column of {@link #TABLE} (a {@code Long}), referenced by the other modules. */
    String KEY_COLUMN = "id";

    /**
     * Finds a processing batch.
     *
     * @param batchId the exact batch code, for example {@code "LOT-DEMO-001"}, surrounding spaces
     *                ignored; possibly {@code null}
     * @return the batch, or empty when no batch has this code or the code is blank
     */
    Optional<ProcessingBatch> findBatch(String batchId);
}
