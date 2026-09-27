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

/** A "YYYY-MM" month as the first day of that month in UTC, or null if malformed. */
function parseMonth(value) {
    if (isMissing(value)) return null;
    const match = /^(\d{4})-(\d{2})$/.exec(String(value).trim());
    if (!match) return null;
    return new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, 1));
}

/**
 * Month sent by the API as "YYYY-MM", shown as "Apr 2027", or "April 2027"
 * with the long style. Read and formatted in UTC, so no timezone can shift it.
 */
export function formatMonth(value, style = "short") {
    const date = parseMonth(value);
    if (!date) return NO_VALUE;
    return date.toLocaleString("en-US", { month: style, year: "numeric", timeZone: "UTC" });
}

/** Month name alone, "Apr", for a chart axis that shows the year apart. */
export function formatMonthName(value) {
    const date = parseMonth(value);
    if (!date) return NO_VALUE;
    return date.toLocaleString("en-US", { month: "short", timeZone: "UTC" });
}

/** Tree age as computed by the API: completed years and remaining months. */
export function formatAge(years, months) {
    if (isMissing(years) || isMissing(months)) return NO_VALUE;
    const yearLabel = Number(years) === 1 ? "yr" : "yrs";
    return `${years} ${yearLabel} ${months} mo`;
}

/**
 * Year band of the growth phase, as computed and named by the API — "0–2 yrs".
 *
 * The band is what the column shows, like the mock-up. The phase name arrives in
 * the same response and is spelled out once, in the legend under the table. No
 * threshold is known here: this only picks which of the two given strings to
 * show, and falls back to the phase name if the API sent no band.
 */
export function formatPhaseBand(phase, yearsBand) {
    if (!isMissing(yearsBand)) return String(yearsBand).trim();
    if (isMissing(phase)) return NO_VALUE;
    const text = String(phase).trim();
    return text.charAt(0).toUpperCase() + text.slice(1);
}
