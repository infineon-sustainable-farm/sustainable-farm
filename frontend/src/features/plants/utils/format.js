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

export function formatMillimeters(value) {
    return isMissing(value) ? NO_VALUE : `${Number(value).toLocaleString("en-US")} mm`;
}

/** Several values are all shown, comma-separated; none is picked. */
export function formatList(values) {
    if (!Array.isArray(values)) return NO_VALUE;
    const present = values.filter((value) => !isMissing(value)).map((value) => String(value).trim());
    return present.length === 0 ? NO_VALUE : present.join(", ");
}

/**
 * Calendar date sent by the API as "YYYY-MM-DD", shown as DD/MM/YYYY.
 * Parsed from the string, not through Date, so no timezone can shift the day.
 */
export function formatDate(value) {
    if (isMissing(value)) return NO_VALUE;
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value).trim());
    if (!match) return NO_VALUE;
    const [, year, month, day] = match;
    return `${day}/${month}/${year}`;
}

/** Tree age as computed by the API: completed years and remaining months. */
export function formatAge(years, months) {
    if (isMissing(years) || isMissing(months)) return NO_VALUE;
    const yearLabel = Number(years) === 1 ? "yr" : "yrs";
    return `${years} ${yearLabel} ${months} mo`;
}

/**
 * Growth phase and its year band, both computed by the API. The phase scale lives
 * only in the backend: nothing here knows a threshold, it only presents the text.
 */
export function formatGrowthPhase(phase, yearsBand) {
    if (isMissing(phase)) return NO_VALUE;
    const text = String(phase).trim();
    const label = text.charAt(0).toUpperCase() + text.slice(1);
    return isMissing(yearsBand) ? label : `${label} (${String(yearsBand).trim()})`;
}
