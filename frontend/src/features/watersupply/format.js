import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'

// Numbers and dates follow the interface language. English keeps the output the module had
// before the translation: plain numbers without grouping (2500, 12.5) and instants written
// "2026-10-20 08:00" in UTC.
const LOCALES = { en: 'en-US', de: 'de-DE', fr: 'fr-FR' }

function toFinite(value) {
  if (value === null || value === undefined || value === '') return null
  const amount = Number(value)
  return Number.isFinite(amount) ? amount : null
}

/**
 * Formatters of the water supply screens for one language.
 *
 * @param {string} language - "en", "de" or "fr".
 * @param {(key: string, options?: object) => string} t - translator of the watersupply namespace.
 */
export function createFormatters(language, t) {
  const locale = LOCALES[language] ?? LOCALES.en
  const none = t('common.none')
  const formats = new Map()
  const instantFormat = new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short', timeZone: 'UTC' })
  const dateTimeFormat = new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'medium' })
  const monthYearFormat = new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' })

  function numberFormat(minimumFractionDigits, maximumFractionDigits) {
    const key = `${minimumFractionDigits}-${maximumFractionDigits}`
    if (!formats.has(key)) {
      formats.set(key, new Intl.NumberFormat(locale, { minimumFractionDigits, maximumFractionDigits, useGrouping: false }))
    }
    return formats.get(key)
  }

  /** Number as a template string printed it (12.5, 2500), `none` when missing. */
  function number(value, maxFractionDigits = 20) {
    const amount = toFinite(value)
    return amount === null ? none : numberFormat(0, maxFractionDigits).format(amount)
  }

  /** Number with a fixed count of decimals, as toFixed did. */
  function fixed(value, fractionDigits) {
    const amount = toFinite(value)
    return amount === null ? none : numberFormat(fractionDigits, fractionDigits).format(amount)
  }

  return {
    none,
    number,
    fixed,
    /** "2500 L". */
    liters: (value) => t('units.liters', { value: number(value) }),
    /** Volume with a readable unit: litres, m³ from 10,000 L ("12.5 m³", "800 L"). */
    volume(value, decimals = 0) {
      const amount = toFinite(value) ?? 0
      if (Math.abs(amount) >= 10000) return t('units.cubicMeters', { value: fixed(amount / 1000, 1) })
      return t('units.liters', { value: fixed(amount, decimals) })
    },
    /** "12.5 mm". */
    mm: (value) => t('units.mm', { value: number(value) }),
    /** "45%". */
    percent: (value) => t('units.percent', { value: number(value) }),
    /** "30 min". */
    minutes: (value) => t('units.minutes', { value: number(value) }),
    /** Instant of the API in UTC: "2026-10-20 08:00" in English, the short local form otherwise. */
    instant(iso) {
      if (!iso) return none
      const date = new Date(iso)
      if (Number.isNaN(date.getTime())) return String(iso)
      if (locale === LOCALES.en) return String(iso).replace('T', ' ').slice(0, 16)
      return instantFormat.format(date)
    },
    /** Date and time in the browser's time zone, as toLocaleString() did. */
    dateTime(value) {
      if (!value) return none
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? String(value) : dateTimeFormat.format(date)
    },
    /** "October 2026" for a year and a month (1 to 12). */
    monthYear(year, month) {
      return monthYearFormat.format(new Date(Number(year), Number(month) - 1, 1))
    },
  }
}

/** Formatters for the active interface language. */
export function useWaterFormat() {
  const { t, i18n } = useTranslation('watersupply')
  const language = i18n.resolvedLanguage ?? 'en'
  return useMemo(() => createFormatters(language, t), [language, t])
}

