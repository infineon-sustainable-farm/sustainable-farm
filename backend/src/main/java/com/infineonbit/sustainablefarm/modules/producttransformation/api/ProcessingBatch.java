package com.infineonbit.sustainablefarm.modules.producttransformation.api;

import java.time.LocalDate;

/**
 * A processing batch of product transformation, as the other modules see it.
 *
 * @param id          key of the batch ({@link ProcessingBatchLookup#KEY_COLUMN}), the link the
 *                    other modules keep
 * @param batchId     business code of the batch, for example {@code "LOT-DEMO-001"}
 * @param harvestDate date of the harvest the batch comes from
 * @param varietyKey  catalogue key of its variety, for example {@code KEITT}
 */
public record ProcessingBatch(Long id, String batchId, LocalDate harvestDate, String varietyKey) {
}
