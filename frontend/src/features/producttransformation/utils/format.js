/**
 * Formatting of the Product Transformation screens, in the active interface language (read from
 * i18n at each call, so a screen that re-renders on a language change reads the new language).
 * English keeps the formats the module had before the translation: en-US dates ("Oct 1, 2026"),
 * numbers with a fixed count of decimals and no grouping, as toFixed wrote them ("1250.00").
 * The words (N/A, units) come from src/locales/<language>/producttransformation.json.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import i18n from '../../../shared/i18n/i18n';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';

const LOCALES = { en: 'en-US', de: 'de-DE', fr: 'fr-FR' };

function locale() {
  return LOCALES[i18n.resolvedLanguage] ?? LOCALES.en;
}

function t(key, options) {
  return i18n.t(key, { ns: 'producttransformation', ...options });
}

/** Text shown for a missing value: "N/A" in English. */
export function notAvailable() {
  return t('common.na');
}

const isMissing = (value) => value === null || value === undefined;

/** Date or instant as "Oct 1, 2026" (en), "1. Okt. 2026" (de), "1 oct. 2026" (fr). */
export const formatDate = (date) => {
  if (!date) return notAvailable();
  return new Date(date).toLocaleDateString(locale(), { year: 'numeric', month: 'short', day: 'numeric' });
};

/** Date-only value (YYYY-MM-DD), read as a local date so it never shifts across time zones. */
export const formatIsoDate = (value) => {
  if (!value) return notAvailable();
  const [year, month, day] = String(value).slice(0, 10).split('-');
  if (!year || !month || !day) return String(value);
  return formatDate(new Date(Number(year), Number(month) - 1, Number(day)));
};

/** Date and time as "Oct 1, 2026, 12:00 PM" (en). */
export const formatDateTime = (date) => {
  if (!date) return notAvailable();
  return new Date(date).toLocaleString(locale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/** Number with a fixed count of decimals, as toFixed: "12.35", "12,35" in German and French. */
export const formatNumber = (num, decimals = 2) => {
  if (isMissing(num)) return notAvailable();
  return new Intl.NumberFormat(locale(), {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
    useGrouping: false,
  }).format(Number(num));
};

/** Weight in kg: "480.50 kg". */
export const formatWeight = (kg) => {
  if (isMissing(kg)) return notAvailable();
  return t('format.kg', { value: formatNumber(kg) });
};

/** Percentage: "92.35%" in English, "92,35 %" in German and French. */
export const formatPercentage = (value) => {
  if (isMissing(value)) return notAvailable();
  return t('format.percent', { value: formatNumber(value) });
};

/**
 * Translated label of an enum code sent by the API: `formatEnum('qcResult', 'PASS')` reads
 * producttransformation:enums.qcResult.PASS. A code without a translation keeps the "Pass" style
 * label, so a value the API adds later is still shown.
 */
export const formatEnum = (group, value) => {
  if (!value) return notAvailable();
  return t(`enums.${group}.${value}`, { defaultValue: formatEnumLabel(String(value)) });
};

/** Badge tone of a batch status. */
export const getStatusTone = (status) =>
  ({
    CREATED: 'neutral',
    INTAKE: 'info',
    WASHING: 'info',
    DRYING: 'warning',
    PACKAGING: 'warning',
    COMPLETED: 'success',
    SHIPPED: 'success',
    REJECTED: 'error',
  })[status] ?? 'neutral';

/** Badge tone of a QC result. */
export const getQcResultTone = (result) =>
  ({ PASS: 'success', FAIL: 'error', REWORK: 'warning', PENDING: 'neutral' })[result] ?? 'neutral';

/** Badge tone of a compliance result. */
export const getComplianceTone = (result) =>
  ({ COMPLIANT: 'success', NON_COMPLIANT: 'error', PENDING: 'neutral' })[result] ?? 'neutral';

/** Truncate text with ellipsis. */
export const truncate = (text, maxLength = 50) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return `${text.substring(0, maxLength)}...`;
};

// Colour classes still read by the QC and Compliance screens until they move to the kit tones.
export const getQcResultColor = (result) =>
  ({ PASS: 'bg-green-100 text-green-800', FAIL: 'bg-red-100 text-red-800', REWORK: 'bg-yellow-100 text-yellow-800' })[result] ??
  'bg-gray-100 text-gray-800';

export const getComplianceColor = (result) =>
  ({ COMPLIANT: 'bg-green-100 text-green-800', NON_COMPLIANT: 'bg-red-100 text-red-800' })[result] ?? 'bg-gray-100 text-gray-800';
