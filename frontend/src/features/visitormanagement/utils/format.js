const dateTimeFormat = new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
});

/**
 * Formats an ISO-8601 datetime from the API to "20 Sep 2026, 18:30".
 *
 * The backend sends LocalDateTime as "2026-09-20T18:30:00" (no offset) and
 * Instant as "2026-09-20T18:30:00Z". Both are accepted by Date; offset-less
 * values are read in the visitor's own timezone. A missing or unparseable
 * value renders as an em dash instead of throwing.
 */
export function formatDateTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return dateTimeFormat.format(date);
}

/**
 * Formats a whole number with thousands separators. A missing value renders
 * as an em dash.
 */
export function formatNumber(value) {
    if (value === null || value === undefined) return "—";
    return new Intl.NumberFormat("en-US").format(value);
}

/**
 * Formats the average satisfaction rating to one decimal, following the
 * mockup's "4.2/5". Zero is rendered as an em dash: before any feedback exists
 * the API reports it as 0 and we should not show "0.0/5" as if people rated us
 * zero.
 */
export function formatSatisfaction(value) {
    if (value === null || value === undefined || value === 0) return "—";
    return `${value.toFixed(1)}/5`;
}

/**
 * ISO-8601 week number of the given date, the kind of "Week 34" label the
 * mockup uses. Monday-based, computed without a date library.
 */
export function getIsoWeekNumber(date) {
    const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNumber = (target.getUTCDay() + 6) % 7;
    target.setUTCDate(target.getUTCDate() - dayNumber + 3);
    const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
    const firstDayNumber = (firstThursday.getUTCDay() + 6) % 7;
    firstThursday.setUTCDate(firstThursday.getUTCDate() - firstDayNumber + 3);
    return 1 + Math.round((target - firstThursday) / (7 * 24 * 3600 * 1000));
}