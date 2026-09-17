/** Shown wherever the source data has no value. Never replaced by 0 or a guess. */
export const NO_VALUE = "—";

function isMissing(value) {
    return value === null || value === undefined || String(value).trim() === "";
}

export function formatText(value) {
    return isMissing(value) ? NO_VALUE : String(value).trim();
}

export function formatNumber(value) {
    return isMissing(value) ? NO_VALUE : Number(value).toLocaleString("en-US");
}

export function formatKilograms(value) {
    return isMissing(value) ? NO_VALUE : `${Number(value).toLocaleString("en-US")} kg`;
}

export function formatMeters(value) {
    return isMissing(value) ? NO_VALUE : `${Number(value).toLocaleString("en-US")} m`;
}

export function formatDensity(value) {
    return isMissing(value) ? NO_VALUE : `${Number(value).toLocaleString("en-US")} trees/ha`;
}

/** Both spacings are needed to state a spacing; one alone is not a value. */
export function formatSpacing(interRow, intraRow) {
    if (isMissing(interRow) || isMissing(intraRow)) return NO_VALUE;
    return `${formatNumber(interRow)} × ${formatNumber(intraRow)} m`;
}

/**
 * The database stores the bare block value ("A"). The "Block " prefix belongs
 * to the display only and is never sent back to the API.
 */
export function formatBlock(value) {
    return isMissing(value) ? NO_VALUE : `Block ${String(value).trim()}`;
}

export function formatTimestamp(value) {
    if (isMissing(value)) return NO_VALUE;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return NO_VALUE;
    return date.toLocaleString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}
