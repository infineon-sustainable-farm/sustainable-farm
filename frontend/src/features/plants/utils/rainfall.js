/** The number of days the Growth Calendar reads the rain over, today included. */
export const RAINFALL_WINDOW_DAYS = 30;

/**
 * The window the rain is read over: the last 30 days, today included, as
 * "YYYY-MM-DD" days in UTC, which is how the rain route counts its days.
 * `now` is a Date.
 */
export function rainfallWindow(now) {
    const today = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
    const day = (time) => new Date(time).toISOString().slice(0, 10);
    return { from: day(today - (RAINFALL_WINDOW_DAYS - 1) * 86_400_000), to: day(today) };
}

/**
 * What the Rainfall cell of a block shows, from the query that read its rain:
 * - `loading` while the answer is awaited;
 * - `value`, with `rainfallMm`, when at least one rain gauge was read;
 * - `none` when the block is linked to no rain gauge (the route answers 404),
 *   or when the answer holds no rainfall or counts no gauge: a rain of 0 mm is
 *   only shown when a gauge measured it;
 * - `unavailable` for any other failure.
 * A block without a query, such as a row with no block, reads `none`.
 */
export function rainfallState(query) {
    if (!query) return { kind: "none" };
    if (query.isPending) return { kind: "loading" };
    if (query.isError) return { kind: query.error?.status === 404 ? "none" : "unavailable" };
    const data = query.data;
    const rainfallMm = data?.rainfallMm;
    if (rainfallMm === null || rainfallMm === undefined || rainfallMm === "" || Number.isNaN(Number(rainfallMm))) {
        return { kind: "none" };
    }
    if (data.gaugeCount === 0) return { kind: "none" };
    return { kind: "value", rainfallMm: Number(rainfallMm) };
}
