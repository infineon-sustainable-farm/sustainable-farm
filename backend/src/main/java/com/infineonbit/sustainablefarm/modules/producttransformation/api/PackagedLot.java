package com.infineonbit.sustainablefarm.modules.producttransformation.api;

/**
 * A packaged lot of product transformation, as the other modules see it.
 *
 * @param recordId identifier of the packaging record, the link the other modules keep
 * @param lotCode  unique lot code of the packaged lot, for example {@code "LOT-DEMO-003-PKG"}
 * @param batchId  processing batch the lot was packaged from, for example {@code "LOT-DEMO-003"}
 */
public record PackagedLot(String recordId, String lotCode, String batchId) {
}
