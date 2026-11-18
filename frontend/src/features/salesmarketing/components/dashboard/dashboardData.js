import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

export function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.fieldErrors && typeof data.fieldErrors === 'object') {
      const details = Object.values(data.fieldErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

export const emptyForecastForm = {
  productId: '',
  forecastMonth: '',
  scenario: 'base',
  forecastedVolumeT: '',
  lowerBoundT: '',
  upperBoundT: '',
  capacityRisk: 'low',
  modelVersion: 'v1',
};

export const FORECAST_EXPORT_HEADERS = [
  'forecastId',
  'productId',
  'forecastMonth',
  'scenario',
  'forecastedVolumeT',
  'lowerBoundT',
  'upperBoundT',
  'capacityRisk',
  'modelVersion',
];

/** CSV of the forecast rows, as the dashboard export downloads it. */
export function forecastsToCsv(forecasts) {
  const rows = forecasts.map((f) =>
    FORECAST_EXPORT_HEADERS.map((h) => {
      const val = f[h] ?? '';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    }).join(',')
  );
  return [FORECAST_EXPORT_HEADERS.join(','), ...rows].join('\n');
}

/**
 * Points of the forecast chart: at most 9 months in date order, the first 60 % drawn as history,
 * the rest as forecast with its confidence band. Null when there are fewer than 2 points.
 */
export function buildChartData(forecasts, monthLabel) {
  const points = [...forecasts]
    .filter((f) => f.forecastedVolumeT != null)
    .sort((a, b) => new Date(a.forecastMonth) - new Date(b.forecastMonth))
    .slice(0, 9);

  if (points.length < 2) return null;

  const splitAt = Math.max(1, Math.floor(points.length * 0.6));
  const values = points.map((p) => Number(p.forecastedVolumeT));
  const maxY = Math.max(...points.map((p) => Number(p.upperBoundT ?? p.forecastedVolumeT)), ...values, 1);

  return {
    todayIndex: splitAt,
    maxLabel: Math.ceil(maxY / 1000) * 1000 || 1000,
    data: points.map((p, i) => ({
      key: i,
      label: monthLabel(p.forecastMonth),
      historical: i <= splitAt ? values[i] : null,
      forecast: i >= splitAt - 1 ? values[i] : null,
      band:
        i >= splitAt
          ? [Number(p.lowerBoundT ?? p.forecastedVolumeT), Number(p.upperBoundT ?? p.forecastedVolumeT)]
          : null,
    })),
  };
}

/** Number formats of the dashboard, in the active language. */
export function useDashboardFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(() => {
    const none = t('dashboard.none');
    const plain = (value) => format.number(value, { maximumFractionDigits: 0 });
    return {
      none,
      plainTons(value) {
        if (value == null || Number.isNaN(Number(value))) return none;
        return plain(value);
      },
      tons(value) {
        if (value == null || Number.isNaN(Number(value))) return none;
        return t('dashboard.tons', { value: plain(value) });
      },
      euroMillions(tons, priceEurPerKg) {
        if (tons == null || Number.isNaN(Number(tons)) || priceEurPerKg == null || Number.isNaN(Number(priceEurPerKg))) {
          return none;
        }
        const eur = Number(tons) * 1000 * Number(priceEurPerKg);
        return t('dashboard.euroMillions', {
          value: format.number(eur / 1_000_000, { minimumFractionDigits: 1, maximumFractionDigits: 1, useGrouping: false }),
        });
      },
      fixed(value, digits) {
        return format.number(value, { minimumFractionDigits: digits, maximumFractionDigits: digits, useGrouping: false });
      },
      number(value) {
        return format.number(value);
      },
      month(value) {
        if (!value) return '';
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return String(value).slice(0, 7);
        return format.date(d, { month: 'short' });
      },
      tick(n) {
        return n >= 1000 ? t('dashboard.thousands', { value: Math.round(n / 1000) }) : String(Math.round(n));
      },
    };
  }, [t, format]);
}
