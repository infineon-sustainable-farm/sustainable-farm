export function getBatchDisplayId(batch) {
    const batchCode = batch.batchCode ?? batch.batch_code;
    if (batchCode != null && String(batchCode).trim()) return String(batchCode);
    return batch.id == null ? "—" : `BT${String(batch.id).padStart(3, "0")}`;
}

export function getBatchProductLabel(batch) {
    return (
        batch.productName ??
        batch.product?.name ??
        batch.productType ??
        batch.product_type ??
        batch.cropType ??
        batch.mangoType ??
        "—"
    );
}

export function findMatchingBatches(batches, search) {
    const query = search.trim().toLowerCase();
    if (!query) return batches;

    const exactIdMatches = batches.filter((batch) =>
        [getBatchDisplayId(batch), batch.id, batch.batchCode, batch.batch_code]
            .filter((value) => value != null)
            .some((value) => String(value).trim().toLowerCase() === query),
    );
    if (exactIdMatches.length > 0) return exactIdMatches;

    return batches.filter((batch) => {
        const searchableText = [
            getBatchDisplayId(batch),
            batch.id,
            batch.batchCode,
            batch.batch_code,
            getBatchProductLabel(batch),
        ]
            .filter((value) => value != null)
            .join(" ")
            .toLowerCase();
        return searchableText.includes(query);
    });
}
