import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { formatEnumLabel } from "../../shared/utils/formatEnumLabel";

// Numbers, amounts and long dates follow the interface language. English keeps the formats
// machinery had before the translation: en-US numbers ("$1234.50", "12.50"), and day/month/year
// numeric dates, which is en-GB ("05/03/2026"). Amounts stay in dollars, as the API stores them.
const NUMBER_LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };
const DATE_LOCALES = { en: "en-GB", de: "de-DE", fr: "fr-FR" };
const CURRENCY = "USD";

/** "2026-03-05" read as a local date, or null. */
export function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = String(value).split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

/**
 * Formatters of the machinery screens for one language.
 *
 * @param {string} language - "en", "de" or "fr".
 * @param {string} none - text shown for a missing value.
 */
export function createFormatters(language, none) {
  const numberLocale = NUMBER_LOCALES[language] ?? NUMBER_LOCALES.en;
  const dateLocale = DATE_LOCALES[language] ?? DATE_LOCALES.en;
  const numericDate = new Intl.DateTimeFormat(dateLocale, { day: "2-digit", month: "2-digit", year: "numeric" });
  const longDate = new Intl.DateTimeFormat(numberLocale, {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });
  const shortMonth = new Intl.DateTimeFormat(numberLocale, { month: "short" });
  const currency = new Intl.NumberFormat(numberLocale, {
    style: "currency",
    currency: CURRENCY,
    currencyDisplay: "narrowSymbol",
    useGrouping: false,
  });
  const currencyTick = new Intl.NumberFormat(numberLocale, {
    style: "currency",
    currency: CURRENCY,
    currencyDisplay: "narrowSymbol",
    useGrouping: false,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
  const decimals = new Map();

  function toFinite(value) {
    if (value === null || value === undefined || value === "") return null;
    const amount = Number(value);
    return Number.isFinite(amount) ? amount : null;
  }

  function decimalFormat(minimumFractionDigits, maximumFractionDigits) {
    const key = `${minimumFractionDigits}-${maximumFractionDigits}`;
    if (!decimals.has(key)) {
      decimals.set(
        key,
        new Intl.NumberFormat(numberLocale, { minimumFractionDigits, maximumFractionDigits, useGrouping: false })
      );
    }
    return decimals.get(key);
  }

  return {
    none,
    /** ISO date as a numeric date (05/03/2026, 05.03.2026), the raw value if it is not a date. */
    date(value) {
      if (!value) return none;
      const date = parseIsoDate(value);
      return date ? numericDate.format(date) : value;
    },
    /** Date of today's dashboard header, e.g. "Tuesday, November 17, 2026". */
    longDate(date) {
      return longDate.format(date);
    },
    /** Short month name of a chart axis, e.g. "Nov". */
    month(date) {
      return shortMonth.format(date);
    },
    /** Number with a fixed count of decimals, as toFixed did. */
    decimal(value, fractionDigits = 2) {
      const amount = toFinite(value);
      return amount === null ? none : decimalFormat(fractionDigits, fractionDigits).format(amount);
    },
    /** Number with at most `maxFractionDigits` decimals, as a plain template string did. */
    number(value, maxFractionDigits = 2) {
      const amount = toFinite(value);
      return amount === null ? String(value) : decimalFormat(0, maxFractionDigits).format(amount);
    },
    /** Amount in dollars with 2 decimals. */
    currency(value) {
      const amount = toFinite(value);
      return amount === null ? none : currency.format(amount);
    },
    /** Amount of a chart axis tick, without trailing zeros. */
    currencyTick(value) {
      const amount = toFinite(value);
      return amount === null ? String(value) : currencyTick.format(amount);
    },
  };
}

/** Formatters for the active interface language. */
export function useMachineryFormat() {
  const { t, i18n } = useTranslation("machinery");
  const language = i18n.resolvedLanguage ?? "en";
  const none = t("common.none");
  return useMemo(() => createFormatters(language, none), [language, none]);
}

/**
 * Translated labels of the machinery enums: `labels.status("UNDER_MAINTENANCE")`. An unknown
 * value keeps the old "Under Maintenance" style label.
 */
export function useEnumLabels() {
  const { t } = useTranslation("machinery");
  return useMemo(() => {
    function label(group, value) {
      if (!value) return t("common.none");
      return t(`enums.${group}.${value}`, { defaultValue: formatEnumLabel(value) });
    }
    return {
      status: (value) => label("status", value),
      category: (value) => label("category", value),
      stage: (value) => label("stage", value),
      maintenanceType: (value) => label("maintenanceType", value),
    };
  }, [t]);
}
