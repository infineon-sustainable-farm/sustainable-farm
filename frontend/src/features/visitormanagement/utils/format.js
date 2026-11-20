import { useMemo } from "react";
import { useTranslation } from "react-i18next";

// Dates and numbers follow the interface language. English keeps the formats the module had
// before the translation: en-GB dates ("20 Sep 2026, 18:30"), en-US numbers ("1,250"), French
// grouping for FCFA amounts ("5 000 FCFA") and the compact 12-hour times of the mockup ("9am").
const DATE_LOCALES = { en: "en-GB", de: "de-DE", fr: "fr-FR" };
const NUMBER_LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };
const FCFA_LOCALES = { en: "fr-FR", de: "de-DE", fr: "fr-FR" };

/*
 * The pure date helpers below work on local calendar dates only. They never go through
 * toISOString(), which would shift the day around midnight for users east or west of UTC, and a
 * slot's date must be the farm's date, not UTC.
 */

/**
 * ISO-8601 week number of the given date ("Week 34"). Monday-based, computed without a date
 * library.
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

/**
 * Parses the API's date format ("YYYY-MM-DD") as a local calendar date. `new Date("2026-09-21")`
 * would be UTC midnight and could display the previous day for users west of UTC, so the parts
 * are read explicitly.
 */
export function parseIsoDate(value) {
    if (!value) return null;
    const [year, month, day] = value.split("-").map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
}

function parseTime(value) {
    if (!value) return null;
    const [hours, minutes] = value.split(":").map(Number);
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return null;
    return { hours, minutes };
}

/**
 * Formatters of the visitor management screens for one language.
 *
 * @param {string} language - "en", "de" or "fr".
 * @param {(key: string, options?: object) => string} t - translator of the visitormanagement
 *   namespace (missing value, units, ranges).
 */
export function createFormatters(language, t) {
    const dateLocale = DATE_LOCALES[language] ?? DATE_LOCALES.en;
    const numberLocale = NUMBER_LOCALES[language] ?? NUMBER_LOCALES.en;
    const fcfaLocale = FCFA_LOCALES[language] ?? FCFA_LOCALES.en;
    const isEnglish = !(language in DATE_LOCALES) || language === "en";

    const dateTimeFormat = new Intl.DateTimeFormat(dateLocale, {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
    const dayMonthFormat = new Intl.DateTimeFormat(dateLocale, { day: "numeric", month: "short" });
    const weekdayFormat = new Intl.DateTimeFormat(dateLocale, { weekday: "long" });
    const weekdayShortFormat = new Intl.DateTimeFormat(dateLocale, { weekday: "short" });
    const shortDateFormat = new Intl.DateTimeFormat(dateLocale, {
        weekday: "short",
        day: "numeric",
        month: "short",
    });
    const timeFormat = new Intl.DateTimeFormat(dateLocale, { hour: "2-digit", minute: "2-digit" });
    const numberFormat = new Intl.NumberFormat(numberLocale);
    const ratingFormat = new Intl.NumberFormat(numberLocale, {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
    });
    const fcfaFormat = new Intl.NumberFormat(fcfaLocale, { maximumFractionDigits: 0 });

    const none = t("format.none");

    function toDate(value) {
        if (!value) return null;
        const date = typeof value === "string" ? new Date(value) : value;
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function time(value) {
        const parts = parseTime(value);
        if (!parts) return none;
        const { hours, minutes } = parts;
        if (!isEnglish) return timeFormat.format(new Date(2000, 0, 1, hours, minutes));
        const suffix = hours < 12 ? "am" : "pm";
        const hour12 = hours % 12 === 0 ? 12 : hours % 12;
        return minutes === 0
            ? `${hour12}${suffix}`
            : `${hour12}:${String(minutes).padStart(2, "0")}${suffix}`;
    }

    function dayMonth(date) {
        const value = toDate(date);
        return value ? dayMonthFormat.format(value) : none;
    }

    return {
        none,

        /**
         * An ISO-8601 datetime from the API as "20 Sep 2026, 18:30". LocalDateTime values come
         * without an offset and are read in the visitor's own timezone.
         */
        dateTime(value) {
            const date = toDate(value);
            return date ? dateTimeFormat.format(date) : none;
        },

        /** A whole number with thousands separators. */
        number(value) {
            if (value === null || value === undefined) return none;
            return numberFormat.format(value);
        },

        /** A price in FCFA, e.g. "5 000 FCFA". */
        fcfa(value) {
            if (value === null || value === undefined) return none;
            const amount = Number(value);
            if (Number.isNaN(amount)) return none;
            return t("format.fcfa", { amount: fcfaFormat.format(amount) });
        },

        /**
         * The average satisfaction rating to one decimal, "4.2/5". Zero shows the missing value:
         * before any feedback exists the API reports 0, which is not a rating of zero.
         */
        satisfaction(value) {
            if (value === null || value === undefined || value === 0) return none;
            return t("format.outOfFive", { value: ratingFormat.format(value) });
        },

        /** "Monday" for a day selector. */
        weekday(date) {
            return weekdayFormat.format(date);
        },

        /** "Mon" for a grid column header. */
        weekdayShort(date) {
            return weekdayShortFormat.format(date);
        },

        /** "Mon 21 Sep" for a registration's slot date ("YYYY-MM-DD"). */
        dateShort(value) {
            const date = parseIsoDate(value);
            return date ? shortDateFormat.format(date) : none;
        },

        /** A compact event date, e.g. "10 Oct". */
        dayMonth,

        /**
         * The scheduling week heading, e.g. "Week 38 · 14–20 Sep 2026". The week shown runs
         * Monday to Saturday, the farm's opening days.
         */
        weekLabel(weekStart) {
            const weekEnd = addDays(weekStart, 5);
            const sameMonth = weekStart.getMonth() === weekEnd.getMonth();
            const start = sameMonth && isEnglish ? String(weekStart.getDate()) : dayMonth(weekStart);
            return t("format.weekLabel", {
                week: getIsoWeekNumber(weekStart),
                start,
                end: dayMonth(weekEnd),
                year: weekEnd.getFullYear(),
            });
        },

        /** The dashboard week, e.g. "21 Sep – 27 Sep, 2026": Monday to Sunday of `date`. */
        weekRange(date) {
            const start = startOfWeek(date);
            const end = addDays(start, 6);
            return t("format.weekRange", {
                start: dayMonth(start),
                end: dayMonth(end),
                year: end.getFullYear(),
            });
        },

        /** A time from the API ("09:00:00") as "9am", "2pm" or "9:30am" ("09:00" in de and fr). */
        time,

        /**
         * A slot's time range: "9–11am" when both ends share the same half of the day in
         * English, "9:30am–11am" otherwise; "09:00–11:00" in de and fr.
         */
        timeRange(start, end) {
            const startLabel = time(start);
            const endLabel = time(end);
            if (
                isEnglish &&
                ((startLabel.endsWith("am") && endLabel.endsWith("am")) ||
                    (startLabel.endsWith("pm") && endLabel.endsWith("pm")))
            ) {
                return t("format.range", { start: startLabel.slice(0, -2), end: endLabel });
            }
            return t("format.range", { start: startLabel, end: endLabel });
        },
    };
}

/** Formatters for the active interface language. */
export function useVisitorFormat() {
    const { t, i18n } = useTranslation("visitormanagement");
    const language = i18n.resolvedLanguage ?? "en";
    return useMemo(() => createFormatters(language, t), [language, t]);
}
