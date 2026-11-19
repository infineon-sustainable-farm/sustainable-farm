import { useMemo } from "react";
import { useTranslation } from "react-i18next";

// Numbers and dates follow the interface language. English keeps what the screens showed before
// the translation: plain numbers without grouping ("1234.5"), toFixed decimals ("12.0"), grouped
// numbers of toLocaleString ("12,345.6"), ISO dates ("2026-11-18", the en-CA order) and ISO
// timestamps as the API sends them.
const NUMBER_LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };
const DATE_LOCALES = { en: "en-CA", de: "de-DE", fr: "fr-FR" };
const MAX_FRACTION_DIGITS = 20;

/** Old label of an enum value: "UNDER_MAINTENANCE" as "Under Maintenance", "PV_ARRAY" as "PV Array". */
export function formatEnumValue(value) {
  return String(value)
    .toLowerCase()
    .split("_")
    .map((word) => (word === "pv" ? "PV" : word.charAt(0).toUpperCase() + word.slice(1)))
    .join(" ");
}

const STATUS_TONES = { OPERATIONAL: "success", UNDER_MAINTENANCE: "warning", OUT_OF_SERVICE: "error" };
const SEVERITY_TONES = { CRITICAL: "error", WARNING: "warning", INFO: "info" };

/** Badge tone of a component status (also the generator status of the summary). */
export function statusTone(status) {
  return STATUS_TONES[status] ?? "neutral";
}

/** Badge tone of an alert severity; an unknown severity looks like INFO, as before. */
export function severityTone(severity) {
  return SEVERITY_TONES[severity] ?? "info";
}

/** Today as a local "yyyy-mm-dd" date, the value of the date inputs. */
export function todayAsLocalDate() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function parseIsoDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
  if (!match) return null;
  return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}

function parseIsoDateTime(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/.exec(String(value));
  if (!match) return null;
  const [, year, month, day, hours, minutes, seconds = "0"] = match;
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hours), Number(minutes), Number(seconds));
}

function toFinite(value) {
  if (value === null || value === undefined || value === "") return null;
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : null;
}

/**
 * Formatters of the energy supply screens for one language.
 *
 * @param {string} language - "en", "de" or "fr".
 */
export function createFormatters(language) {
  const numberLocale = NUMBER_LOCALES[language] ?? NUMBER_LOCALES.en;
  const dateLocale = DATE_LOCALES[language] ?? DATE_LOCALES.en;
  const plain = new Intl.NumberFormat(numberLocale, { maximumFractionDigits: MAX_FRACTION_DIGITS, useGrouping: false });
  const grouped = new Intl.NumberFormat(numberLocale);
  const numericDate = new Intl.DateTimeFormat(dateLocale, { year: "numeric", month: "2-digit", day: "2-digit" });
  const dateTime = new Intl.DateTimeFormat(dateLocale, { dateStyle: "short", timeStyle: "short" });
  const fixed = new Map();

  function fixedFormat(digits) {
    if (!fixed.has(digits)) {
      fixed.set(digits, new Intl.NumberFormat(numberLocale, {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits,
        useGrouping: false,
      }));
    }
    return fixed.get(digits);
  }

  return {
    /** Number as it came from the API ("1234.5"); a non-number is shown as is. */
    number(value) {
      const amount = toFinite(value);
      return amount === null ? String(value ?? "") : plain.format(amount);
    },
    /** Number with a fixed count of decimals, as toFixed did. */
    fixed(value, digits = 1) {
      const amount = toFinite(value);
      return amount === null ? String(value ?? "") : fixedFormat(digits).format(amount);
    },
    /** Number with thousands separators, as toLocaleString did. */
    grouped(value) {
      const amount = toFinite(value);
      return amount === null ? String(value ?? "") : grouped.format(amount);
    },
    /** ISO date as a numeric date (2026-11-18, 18.11.2026, 18/11/2026); other values as is. */
    date(value) {
      const date = parseIsoDate(value);
      return date ? numericDate.format(date) : String(value ?? "");
    },
    /** ISO timestamp of the API; English keeps it as sent, German and French get a local date and time. */
    dateTime(value) {
      if (language === "en") return String(value ?? "");
      const date = parseIsoDateTime(value);
      return date ? dateTime.format(date) : String(value ?? "");
    },
  };
}

/** Formatters for the active interface language. */
export function useEnergyFormat() {
  const { i18n } = useTranslation("energysupply");
  const language = i18n.resolvedLanguage ?? "en";
  return useMemo(() => createFormatters(language), [language]);
}

/**
 * Translated labels of the energy enums: `labels.status("UNDER_MAINTENANCE")`. An empty value is
 * "—"; an unknown one keeps its old "Under Maintenance" style label.
 */
export function useEnumLabels() {
  const { t } = useTranslation("energysupply");
  return useMemo(() => {
    function label(group, value) {
      if (!value) return t("common.none");
      return t(`enums.${group}.${value}`, { defaultValue: formatEnumValue(value) });
    }
    return {
      category: (value) => label("category", value),
      status: (value) => label("status", value),
      loadCategory: (value) => label("loadCategory", value),
      source: (value) => label("source", value),
      severity: (value) => label("severity", value),
      alertStatus: (value) => label("alertStatus", value),
    };
  }, [t]);
}
