import i18n from "../../../shared/i18n/i18n";

/*
 * Formatting of the Plants screens, in the active interface language (read from i18n at each call,
 * so a screen that re-renders on a language change reads the new language). English keeps the
 * formats the module had before the translation, word for word: en-US numbers ("1,076"), dates as
 * "6 Nov 2026" with the module's own month names (Intl's en-GB writes September "Sept"). German and
 * French use Intl with de-DE and fr-FR. The words (units, "Block", "in 3 days") come from
 * src/locales/<language>/plants.json, under `format`.
 */
const NUMBER_LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };

const MONTHS_LONG_EN = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
];
const MONTHS_SHORT_EN = MONTHS_LONG_EN.map((name) => name.slice(0, 3));

function language() {
    const resolved = i18n.resolvedLanguage ?? "en";
    return NUMBER_LOCALES[resolved] ? resolved : "en";
}

function locale() {
    return NUMBER_LOCALES[language()];
}

function t(key, options) {
    return i18n.t(key, { ns: "plants", ...options });
}

/**
 * Shown wherever the source data has no value. Never replaced by 0 or a guess. The same dash in
 * every language, read once from the locale file.
 */
export const NO_VALUE = i18n.t("plants:common.none");

function isMissing(value) {
    return value === null || value === undefined || String(value).trim() === "";
}

/** A number in the active language: "1,076" in English, "1.076" in German, "1 076" in French. */
function localeNumber(value, options) {
    return Number(value).toLocaleString(locale(), options);
}

export function formatText(value) {
    return isMissing(value) ? NO_VALUE : String(value).trim();
}

export function formatNumber(value) {
    return isMissing(value) ? NO_VALUE : localeNumber(value);
}

export function formatKilograms(value) {
    return isMissing(value) ? NO_VALUE : t("format.kilograms", { value: localeNumber(value) });
}

export function formatMeters(value) {
    return isMissing(value) ? NO_VALUE : t("format.meters", { value: localeNumber(value) });
}

export function formatDensity(value) {
    return isMissing(value) ? NO_VALUE : t("format.density", { value: localeNumber(value) });
}

/** Both spacings are needed to state a spacing; one alone is not a value. */
export function formatSpacing(interRow, intraRow) {
    if (isMissing(interRow) || isMissing(intraRow)) return NO_VALUE;
    return t("format.spacing", { interRow: formatNumber(interRow), intraRow: formatNumber(intraRow) });
}

/**
 * The database stores the bare block value ("A"). The "Block " prefix belongs
 * to the display only and is never sent back to the API.
 */
export function formatBlock(value) {
    return isMissing(value) ? NO_VALUE : t("format.block", { block: String(value).trim() });
}

/** Name of a month index from 0, long or short, in the active language. */
function monthLabel(index, style) {
    if (language() === "en") return (style === "long" ? MONTHS_LONG_EN : MONTHS_SHORT_EN)[index];
    return new Intl.DateTimeFormat(locale(), { month: style, timeZone: "UTC" }).format(Date.UTC(2000, index, 1));
}

/** Instant sent by the API, in the browser's time zone: "6 Nov 2026, 14:05". */
export function formatTimestamp(value) {
    if (isMissing(value)) return NO_VALUE;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return NO_VALUE;
    if (language() !== "en") {
        return new Intl.DateTimeFormat(locale(), {
            day: "numeric",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }).format(date);
    }
    const time = [date.getHours(), date.getMinutes()].map((part) => String(part).padStart(2, "0")).join(":");
    return `${date.getDate()} ${MONTHS_SHORT_EN[date.getMonth()]} ${date.getFullYear()}, ${time}`;
}

export function formatMillimeters(value) {
    return isMissing(value) ? NO_VALUE : t("format.millimeters", { value: localeNumber(value) });
}

/**
 * Enum code sent by the API, in sentence case: "MINERAL" is shown as
 * "Mineral", "ADJUSTMENT_IN" as "Adjustment in". The code itself is what goes
 * back to the API. This is the English fallback of `formatEnum`.
 */
export function formatCode(value) {
    if (isMissing(value)) return NO_VALUE;
    const text = String(value).trim().toLowerCase().replaceAll("_", " ");
    return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * Translated label of an enum code sent by the API: `formatEnum("severity", "CRITICAL")` reads
 * plants:enums.severity.CRITICAL. A code without a translation keeps its sentence case label
 * (`formatCode`), so a value the API adds later is still shown, never dropped.
 */
export function formatEnum(group, value) {
    if (isMissing(value)) return NO_VALUE;
    const code = String(value).trim();
    return t(`enums.${group}.${code}`, { defaultValue: formatCode(code) });
}

/** Symbols of the fertilizer units, as the API writes them in its own messages. */
const UNIT_SYMBOLS = { KG: "kg", L: "L" };

/** Symbol of a fertilizer unit code, "KG" as "kg". An unknown code is shown as received. */
export function formatUnit(unit) {
    if (isMissing(unit)) return NO_VALUE;
    const code = String(unit).trim();
    return UNIT_SYMBOLS[code] ?? code;
}

/**
 * A quantity in the unit of its fertilizer: "1,000 kg", "12.5 L". Up to three
 * decimals, as the API stores them.
 */
export function formatQuantity(value, unit) {
    if (isMissing(value)) return NO_VALUE;
    const number = localeNumber(value);
    return isMissing(unit) ? number : t("format.quantity", { value: number, unit: formatUnit(unit) });
}

/** An amount in CFA francs, already rounded to the franc by the API: "78,715 FCFA". */
export function formatXof(value) {
    return isMissing(value) ? NO_VALUE : t("format.xof", { value: localeNumber(value) });
}

/** An amount in euros, already rounded to the cent by the API: "€228.67", "€120.00". */
export function formatEur(value) {
    return isMissing(value) ? NO_VALUE : localeNumber(value, { style: "currency", currency: "EUR" });
}

/** A percentage as computed by the API, with the one decimal it keeps: "92%", "94.5%". */
export function formatPercent(value) {
    return isMissing(value)
        ? NO_VALUE
        : t("format.percent", { value: localeNumber(value, { maximumFractionDigits: 1 }) });
}

/** Origin code of a nursery batch, "IN_HOUSE" as "In-house production". An unknown code is shown as received. */
export function formatOrigin(origin) {
    if (isMissing(origin)) return NO_VALUE;
    const code = String(origin).trim();
    return t(`enums.origin.${code}`, { defaultValue: code });
}

/** Several values are all shown, comma-separated; none is picked. */
export function formatList(values) {
    if (!Array.isArray(values)) return NO_VALUE;
    const present = values.filter((value) => !isMissing(value)).map((value) => String(value).trim());
    return present.length === 0 ? NO_VALUE : present.join(t("format.listSeparator"));
}

/**
 * Calendar date sent by the API, or chosen in a date field, as "YYYY-MM-DD",
 * shown as "6 Nov 2026": the month in letters leaves no doubt between day and
 * month. Parsed from the string, not through Date, so no timezone can shift
 * the day.
 */
export function formatDate(value) {
    if (isMissing(value)) return NO_VALUE;
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value).trim());
    if (!match) return NO_VALUE;
    const [, year, month, day] = match.map(Number);
    if (month < 1 || month > 12 || day < 1 || day > 31) return NO_VALUE;
    if (language() !== "en") {
        return new Intl.DateTimeFormat(locale(), {
            day: "numeric",
            month: "short",
            year: "numeric",
            timeZone: "UTC",
        }).format(Date.UTC(year, month - 1, day));
    }
    return `${day} ${MONTHS_SHORT_EN[month - 1]} ${year}`;
}

/**
 * Days left before a date, as counted by the API: "in 1 day", "in 1,076 days".
 * Null on the day itself or once it has passed, when there is nothing left to
 * wait for.
 */
export function formatDaysAhead(days) {
    if (isMissing(days)) return null;
    const count = Number(days);
    if (!Number.isInteger(count) || count <= 0) return null;
    return t("format.daysAhead", { count, days: localeNumber(count) });
}

/** A "YYYY-MM" month as its year and its month index from 0, or null if malformed. */
function parseMonth(value) {
    if (isMissing(value)) return null;
    const match = /^(\d{4})-(\d{2})$/.exec(String(value).trim());
    if (!match) return null;
    const month = Number(match[2]);
    if (month < 1 || month > 12) return null;
    return { year: Number(match[1]), index: month - 1 };
}

/**
 * Month sent by the API as "YYYY-MM", shown as "Apr 2027", or "April 2027"
 * with the long style.
 */
export function formatMonth(value, style = "short") {
    const month = parseMonth(value);
    if (!month) return NO_VALUE;
    if (language() !== "en") {
        return new Intl.DateTimeFormat(locale(), { month: style, year: "numeric", timeZone: "UTC" }).format(
            Date.UTC(month.year, month.index, 1)
        );
    }
    return `${monthLabel(month.index, style)} ${month.year}`;
}

/** Month name alone, "Apr", for a chart axis that shows the year apart. */
export function formatMonthName(value) {
    const month = parseMonth(value);
    if (!month) return NO_VALUE;
    return monthLabel(month.index, "short");
}

/** Full name of a month number from 1 to 12, "May"; null for anything else. */
export function monthName(month) {
    const number = Number(month);
    if (!Number.isInteger(number) || number < 1 || number > 12) return null;
    return monthLabel(number - 1, "long");
}

/**
 * Harvest season from its first and last month numbers: "May to July", with the
 * typographic dash of the locale file. A season over the new year reads in its
 * own order, "November to February".
 */
export function formatSeason(startMonth, endMonth) {
    const start = monthName(startMonth);
    const end = monthName(endMonth);
    if (!start || !end) return NO_VALUE;
    return start === end ? start : t("format.range", { start, end });
}

/** A yield share from 0 to 1, with the up to 3 decimals the API keeps: "0.25", "1". */
export function formatShare(value) {
    return isMissing(value) ? NO_VALUE : localeNumber(value, { maximumFractionDigits: 3 });
}

/** Tree age as computed by the API: completed years and remaining months, "3 yrs 4 mo". */
export function formatAge(years, months) {
    if (isMissing(years) || isMissing(months)) return NO_VALUE;
    return t("format.age", { count: Number(years), years, months });
}

/**
 * Growth phase as computed and named by the API, with its year band:
 * "Full production · 6+ yrs".
 *
 * The name is the API's, only capitalised, and the band is the API's too: no
 * threshold is known here. Either part is shown alone when the API sent only
 * that one, and a dash when it sent neither.
 */
export function formatPhaseBand(phase, yearsBand) {
    const name = isMissing(phase) ? "" : String(phase).trim();
    const band = isMissing(yearsBand) ? "" : String(yearsBand).trim();
    const label = name.charAt(0).toUpperCase() + name.slice(1);
    if (label && band) return t("format.phaseBand", { phase: label, band });
    return label || band || NO_VALUE;
}
