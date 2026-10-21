import { formatBlock, formatNumber, formatPercent } from "../utils/format";

/** "P1 · Keitt (Block E)", or "P1 · Keitt" for a batch with no planned block. */
function cardTitle(batch) {
    const title = `${batch.batchCode} · ${batch.varietyName}`;
    return batch.plannedBlockCode ? `${title} (${formatBlock(batch.plannedBlockCode)})` : title;
}

/**
 * Survival rate of each batch, as in the mock-up's cards: how many of the
 * starting plants survived the nursery, the rate, and a bar as wide as the
 * rate. All three come from the API, which counts a transplanted plant as a
 * survivor.
 *
 * One colour for every bar: the mock-up turns a rate amber below about 90%,
 * but no source sets such a threshold and the API sends none. The bar repeats
 * the rate written next to it, so it is hidden from screen readers.
 * Bar #0a8276 on its track #eef2f1 -> 4.16:1; the mock-up's green gave 2.99:1.
 */
export default function NurserySurvivalCards({ batches }) {
    return (
        <div className="flex flex-col gap-3">
            <h4 id="survival-rate-title" className="font-heading text-sm font-bold text-gray-900">
                Survival rate by batch
            </h4>
            <ul aria-labelledby="survival-rate-title" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {batches.map((batch) => (
                    <li key={batch.id} className="rounded-xl border border-gray-200 bg-white px-4 py-4">
                        <p className="text-sm font-semibold break-words text-gray-900">{cardTitle(batch)}</p>
                        <div className="mt-2 flex items-baseline justify-between gap-3">
                            <span className="text-sm text-gray-600">
                                {`${formatNumber(batch.survivedCount)} of ${formatNumber(batch.initialCount)} survived`}
                            </span>
                            <span className="text-base font-bold text-gray-900">
                                {formatPercent(batch.survivalRatePct)}
                            </span>
                        </div>
                        <div aria-hidden="true" className="mt-2 h-2 overflow-hidden rounded-full bg-[#eef2f1]">
                            <div
                                className="h-full rounded-full bg-primary"
                                style={{ width: `${batch.survivalRatePct}%` }}
                            />
                        </div>
                        <p className="mt-2 text-xs text-gray-600">
                            {`${formatNumber(batch.lossCount)} lost · ${formatNumber(batch.transplantedCount)} transplanted`}
                        </p>
                    </li>
                ))}
            </ul>
        </div>
    );
}
