import { varietyLabel } from "./varieties";

export function getBatchDisplayId(batch) {
    const batchCode = batch.batchCode ?? batch.batch_code;
    if (batchCode != null && String(batchCode).trim()) return String(batchCode);
    return batch.id == null ? "—" : `BT${String(batch.id).padStart(3, "0")}`;
}

/**
 * What a batch holds, as shown and searched: its variety (Plants catalogue name) and, with a
 * translation function, the translated product type; the raw values otherwise.
 */
export function getBatchProductLabel(batch, t) {
    const type = batch.productType ?? batch.product_type;
    const typeLabel = type && t ? t(`enums.productType.${type}`, { defaultValue: type }) : type;
    const variety = varietyLabel(t, batch.varietyKey, batch.mangoVariety) ?? batch.productName ?? batch.product?.name ?? batch.cropType;
    const parts = [variety, typeLabel].filter(Boolean);
    return parts.length ? parts.join(" · ") : "—";
}

/** Batches matching the search; with t, the translated labels match too. */
export function findMatchingBatches(batches, search, t) {
    const query = search.trim().toLowerCase();
    if (!query) return batches;

    return batches
        .map((batch, index) => {
            const searchableText = [
                getBatchDisplayId(batch),
                batch.id,
                batch.batchCode,
                batch.batch_code,
                getBatchProductLabel(batch),
                t ? getBatchProductLabel(batch, t) : null,
            ]
                .filter((value) => value != null)
                .join(" ")
                .toLowerCase();
            if (!searchableText.includes(query)) return null;

            const exactIdMatch = [
                getBatchDisplayId(batch),
                batch.id,
                batch.batchCode,
                batch.batch_code,
            ]
                .filter((value) => value != null)
                .some((value) => String(value).trim().toLowerCase() === query);

            return { batch, index, exactIdMatch };
        })
        .filter(Boolean)
        .sort((a, b) => Number(b.exactIdMatch) - Number(a.exactIdMatch) || a.index - b.index)
        .map(({ batch }) => batch);
}
