import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

// Numbers and dates follow the interface language. English keeps the output the screens had
// before the translation: they called toLocaleString() and toLocaleDateString('en-US'), so
// English formats with en-US.
const LOCALES = { en: 'en-US', de: 'de-DE', fr: 'fr-FR' };

/** Intl locale of an interface language ("en" gives "en-US"). */
export function localeOf(language) {
  return LOCALES[language] ?? LOCALES.en;
}

/** "2026-03-05" read as a local calendar date, or null. */
export function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

const pad = (n) => String(n).padStart(2, '0');

/**
 * "2026-03-05" of a date in local time. Date#toISOString gives the UTC day, which is the day
 * before or after near midnight. For today, use shared/utils/todayIsoDate.
 */
export function localIsoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** "2026-03-05T08:00" of a date in local time, the value of a datetime-local input. */
export function localIsoDateTime(date) {
  return `${localIsoDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * Formatters of the sales and marketing screens for one language. They mirror the native calls the
 * screens used: number(value, options) for Number#toLocaleString, date(value, options) for
 * Date#toLocaleDateString, dateTime(value, options) for Date#toLocaleString.
 *
 * @param {string} language - "en", "de" or "fr".
 */
export function createFormatters(language) {
  const locale = localeOf(language);
  const cache = new Map();

  function formatter(kind, options) {
    const key = `${kind}:${JSON.stringify(options ?? {})}`;
    if (!cache.has(key)) {
      cache.set(
        key,
        kind === 'number' ? new Intl.NumberFormat(locale, options) : new Intl.DateTimeFormat(locale, options)
      );
    }
    return cache.get(key);
  }

  function toDate(value) {
    if (value instanceof Date) return value;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  return {
    locale,
    /** Number#toLocaleString(locale, options). */
    number(value, options) {
      return formatter('number', options).format(Number(value));
    },
    /** Date#toLocaleDateString(locale, options); the raw value when it is not a date. */
    date(value, options) {
      const date = toDate(value);
      if (!date) return String(value);
      return formatter('date', options ?? { year: 'numeric', month: 'numeric', day: 'numeric' }).format(date);
    },
    /** Date#toLocaleString(locale, options); the raw value when it is not a date. */
    dateTime(value, options) {
      const date = toDate(value);
      if (!date) return String(value);
      return formatter(
        'date',
        options ?? { year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: '2-digit', second: '2-digit' }
      ).format(date);
    },
  };
}

/** Formatters for the active interface language. */
export function useSalesFormat() {
  const { i18n } = useTranslation('salesmarketing');
  const language = i18n.resolvedLanguage ?? 'en';
  return useMemo(() => createFormatters(language), [language]);
}
