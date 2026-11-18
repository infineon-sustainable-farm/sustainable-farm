package com.infineonbit.sustainablefarm.modules.producttransformation.api;

/**
 * A packaged lot of product transformation, as the other modules see it.
 *
 * @param id       key of the packaging record ({@link PackagedLotLookup#KEY_COLUMN}), the link the
 *                 other modules keep
 * @param recordId business code of the packaging record, for example {@code "PKG-DEMO-001"}
 * @param lotCode  unique lot code of the packaged lot, for example {@code "LOT-DEMO-003-PKG"}
 * @param batchId  code of the processing batch the lot was packaged from, for example
 *                 {@code "LOT-DEMO-003"}
 */
public record PackagedLot(Long id, String recordId, String lotCode, String batchId) {
}
