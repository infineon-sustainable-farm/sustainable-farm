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

const fcfaFormat = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 });

/**
 * A price in FCFA, e.g. "5 000 FCFA". The API sends prices as numbers; a
 * missing or non-numeric value renders as an em dash.
 */
export function formatFcfa(value) {
    if (value === null || value === undefined) return "—";
    const amount = Number(value);
    if (Number.isNaN(amount)) return "—";
    return `${fcfaFormat.format(amount)} FCFA`;
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

const dayMonthFormat = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short" });
const weekdayFormat = new Intl.DateTimeFormat("en-GB", { weekday: "long" });
const shortDateFormat = new Intl.DateTimeFormat("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
});

/*
 * All date helpers below work on local calendar dates only. They never go
 * through toISOString(), which would shift the day around midnight for users
 * east or west of UTC — and a slot's date must be the farm's date, not UTC.
 */

/** Midnight of the Monday starting the week of `date`. */
export function startOfWeek(date) {
    const day = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    day.setDate(day.getDate() - ((day.getDay() + 6) % 7));
    return day;
}

/** A new local date shifted by `days`, leaving the input untouched. */
export function addDays(date, days) {
    const shifted = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    shifted.setDate(shifted.getDate() + days);
    return shifted;
}

/** The API's date format: "YYYY-MM-DD", in local time. */
export function toIsoDate(date) {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
}

/** "Monday" for a day selector. */
export function formatWeekday(date) {
    return weekdayFormat.format(date);
}

/**
 * Parses the API's date format ("YYYY-MM-DD") as a local calendar date.
 * `new Date("2026-09-21")` would be UTC midnight and could display the
 * previous day for users west of UTC, so the parts are read explicitly.
 */
export function parseIsoDate(value) {
    if (!value) return null;
    const [year, month, day] = value.split("-").map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
}

/** "Mon 21 Sep" for a registration's slot date. */
export function formatDateShort(value) {
    const date = parseIsoDate(value);
    if (!date) return "—";
    return shortDateFormat.format(date);
}

/**
 * The mockup's week heading, e.g. "Week 38 · 14–20 Sep 2026". The week shown
 * runs Monday to Saturday, matching the farm's opening days.
 */
export function formatWeekLabel(weekStart) {
    const weekEnd = addDays(weekStart, 5);
    const sameMonth = weekStart.getMonth() === weekEnd.getMonth();
    const startLabel = sameMonth ? String(weekStart.getDate()) : dayMonthFormat.format(weekStart);
    return `Week ${getIsoWeekNumber(weekStart)} · ${startLabel}–${dayMonthFormat.format(weekEnd)} ${weekEnd.getFullYear()}`;
}

/** A date as the dashboard's compact event date, e.g. "10 Oct". */
export function formatDayMonth(date) {
    if (!date) return "—";
    const value = typeof date === "string" ? new Date(date) : date;
    if (Number.isNaN(value.getTime())) return "—";
    return dayMonthFormat.format(value);
}

/**
 * The dashboard week selector's range, e.g. "Sep 21 – Sep 27, 2026": Monday
 * to Sunday of the week containing `date`, year shown once at the end.
 */
export function formatWeekRange(date) {
    const start = startOfWeek(date);
    const end = addDays(start, 6);
    return `${dayMonthFormat.format(start)} – ${dayMonthFormat.format(end)}, ${end.getFullYear()}`;
}

/**
 * A time from the API ("09:00:00") as "9am", "2pm" or "9:30am". Invalid or
 * missing values render as an em dash.
 */
export function formatTime(value) {
    if (!value) return "—";
    const [hours, minutes] = value.split(":").map(Number);
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return "—";
    const suffix = hours < 12 ? "am" : "pm";
    const hour12 = hours % 12 === 0 ? 12 : hours % 12;
    return minutes === 0 ? `${hour12}${suffix}` : `${hour12}:${String(minutes).padStart(2, "0")}${suffix}`;
}

/**
 * A slot's time range in the mockup's compact style: "9–11am" when both ends
 * share the same half of the day, "2–4pm" likewise, and the full form
 * ("9:30am–11am") otherwise.
 */
export function formatTimeRange(start, end) {
    const startLabel = formatTime(start);
    const endLabel = formatTime(end);
    if (startLabel.endsWith("am") && endLabel.endsWith("am")) {
        return `${startLabel.slice(0, -2)}–${endLabel}`;
    }
    if (startLabel.endsWith("pm") && endLabel.endsWith("pm")) {
        return `${startLabel.slice(0, -2)}–${endLabel}`;
    }
    return `${startLabel}–${endLabel}`;
}