import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

// The season and demand labels are stored with each applied price: they stay in English, as the
// API has always received them. The screen shows their translation.
export function seasonKey(v) {
  if (v > 80) return 'peak';
  if (v > 40) return 'mid';
  return 'early';
}

const SEASON_LABELS = { peak: 'Peak', mid: 'Mid-Season', early: 'Early' };

export function seasonFromValue(v) {
  return SEASON_LABELS[seasonKey(v)];
}

export function demandLabel(v) {
  const idx = (v / 100).toFixed(2);
  return `${idx}${v > 105 ? ' (High)' : ' (Norm)'}`;
}

export function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  if (data.message) return data.message;
  return fallback;
}

/** The last 30 prices of one product's history, oldest first, with their range; null under 2. */
export function buildTrend(history) {
  const points = history
    .filter((item) => item.priceEurPerKg != null)
    .sort((a, b) => new Date(a.priceDate) - new Date(b.priceDate))
    .slice(-30);
  if (points.length < 2) return null;
  const prices = points.map((item) => Number(item.priceEurPerKg));
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  // Keep a little visual padding around the real values.
  const padding = Math.max((max - min) * 0.2, 0.05);
  return { points, min, max, domain: [min - padding, max + padding] };
}

/** Prices, signed amounts, percentages and month names of the pricing screen. */
export function usePricingFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(() => {
    const fixed = (value, digits) =>
      format.number(value, { minimumFractionDigits: digits, maximumFractionDigits: digits, useGrouping: false });
    return {
      none: t('pricing.none'),
      fixed,
      /** "€5.80", none without a value. */
      price(value) {
        return value != null ? t('pricing.price', { value: fixed(Number(value), 2) }) : t('pricing.none');
      },
      /** "+0.12 (2.0%)". */
      delta(delta, pct) {
        return t('pricing.delta', {
          sign: delta >= 0 ? '+' : '',
          value: fixed(delta, 2),
          pct: fixed(pct, 1),
        });
      },
      /** "+€0.30". */
      priceDelta(delta) {
        return t('pricing.priceDelta', { sign: delta >= 0 ? '+' : '', value: fixed(delta, 2) });
      },
      demand(v) {
        return t(v > 105 ? 'pricing.demandHigh' : 'pricing.demandNorm', { index: fixed(v / 100, 2) });
      },
      number(value) {
        return format.number(value);
      },
      month(value) {
        return format.date(new Date(value), { month: 'short' });
      },
    };
  }, [t, format]);
}
