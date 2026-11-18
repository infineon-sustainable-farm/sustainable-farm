// Pure helpers of the demand forecasting screen: error text, month-over-month growth, chart
// points, scenario totals, actual sales and accuracy, CSV export.

export const SCENARIOS = ['base', 'optimistic', 'pessimistic'];
export const RISKS = ['low', 'medium', 'high'];

// Sent to the API when no product is selected, and stored as the status of a new model run.
export const DEFAULT_CROP_VARIETY = 'Dried Mango';
export const COLD_START_STATUS = 'Cold-Start Forecast';

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

/** Message of a failed request, with the field errors of the API when it sends some. */
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

/**
 * Adds to each row (sorted by month) its growth over the previous one: `momGrowth` is the text of
 * the CSV export ("-", "0%", "+∞", "+12.5%"), `momValue` the number (null for the first row).
 */
export function computeMomGrowth(sortedRows) {
  return sortedRows.map((row, idx) => {
    if (idx === 0) return { ...row, momGrowth: '-', momValue: null };
    const prev = Number(sortedRows[idx - 1].forecastedVolumeT || 0);
    const curr = Number(row.forecastedVolumeT || 0);
    if (prev === 0) {
      return curr === 0
        ? { ...row, momGrowth: '0%', momValue: 0 }
        : { ...row, momGrowth: '+∞', momValue: Infinity };
    }
    const pct = ((curr - prev) / prev) * 100;
    const sign = pct > 0 ? '+' : '';
    return { ...row, momGrowth: `${sign}${pct.toFixed(1)}%`, momValue: pct };
  });
}

/**
 * Points of the projection chart: the first 10 dated rows; the first 55 % are drawn as history,
 * the rest as forecast with its confidence band. Null when fewer than 2 points.
 */
export function buildProjectionData(rows) {
  const points = [...rows]
    .filter((r) => r.forecastedVolumeT != null)
    .sort((a, b) => new Date(a.forecastMonth) - new Date(b.forecastMonth))
    .slice(0, 10);
  if (points.length < 2) return null;
  const splitAt = Math.max(1, Math.floor(points.length * 0.55));
  return {
    splitAt,
    points: points.map((p, i) => {
      const value = Number(p.forecastedVolumeT);
      const lower = Number(p.lowerBoundT ?? p.forecastedVolumeT);
      const upper = Number(p.upperBoundT ?? p.forecastedVolumeT);
      return {
        index: i,
        forecastMonth: p.forecastMonth,
        historical: i <= splitAt ? value : null,
        forecast: i >= splitAt ? value : null,
        band: i >= splitAt ? [lower, upper] : null,
      };
    }),
  };
}

function sumBy(rows, read) {
  return rows.reduce((s, f) => s + Number(read(f) ?? 0), 0);
}

/** Total volume of the base, pessimistic and optimistic scenarios (bounds when a scenario has none). */
export function scenarioTotals(forecasts) {
  const byScenario = (name) =>
    sumBy(
      forecasts.filter((f) => (f.scenario || '').toLowerCase().includes(name)),
      (f) => f.forecastedVolumeT || 0
    );
  const base =
    byScenario('base') ||
    sumBy(
      forecasts.filter((f) => {
        const s = (f.scenario || '').toLowerCase();
        return !s || s === 'base';
      }),
      (f) => f.forecastedVolumeT || 0
    );
  return {
    base,
    pessimistic: byScenario('pessimistic') || sumBy(forecasts, (f) => f.lowerBoundT),
    optimistic: byScenario('optimistic') || sumBy(forecasts, (f) => f.upperBoundT),
  };
}

/** Sold tons by "productId-YYYY-MM", from the orders and their items (kg). */
export function actualSalesByProductMonth(orders, orderItems) {
  const orderById = new Map(orders.map((order) => [Number(order.orderId), order]));
  const totals = new Map();
  orderItems.forEach((item) => {
    const order = orderById.get(Number(item.orderId));
    if (!order || !order.orderDate || !item.productId) return;
    const quantityKg = Number(item.quantityKg ?? 0);
    if (!Number.isFinite(quantityKg) || quantityKg <= 0) return;
    const month = String(order.orderDate).slice(0, 7);
    const key = `${Number(item.productId)}-${month}`;
    totals.set(key, (totals.get(key) || 0) + quantityKg / 1000);
  });
  return totals;
}

/**
 * Base forecasts compared with the actual sales of their month. Without actual sales the accuracy
 * is null: it is never invented for future months.
 */
export function forecastAccuracy(forecasts, actualSales) {
  return forecasts
    .filter((forecast) => (forecast.scenario || '').toLowerCase() === 'base')
    .map((forecast) => {
      const key = `${Number(forecast.productId)}-${String(forecast.forecastMonth).slice(0, 7)}`;
      const actualVolumeT = actualSales.get(key);
      const forecastVolumeT = Number(forecast.forecastedVolumeT ?? 0);
      if (actualVolumeT === undefined || !Number.isFinite(forecastVolumeT) || forecastVolumeT <= 0) {
        return { ...forecast, actualVolumeT: null, accuracy: null };
      }
      const absolutePercentageError = Math.abs(actualVolumeT - forecastVolumeT) / forecastVolumeT;
      return { ...forecast, actualVolumeT, accuracy: Math.max(0, (1 - absolutePercentageError) * 100) };
    });
}

const CSV_HEADERS = [
  'forecastId',
  'productId',
  'forecastMonth',
  'scenario',
  'forecastedVolumeT',
  'lowerBoundT',
  'upperBoundT',
  'momGrowth',
  'capacityRisk',
  'modelVersion',
];

/** CSV text of the monthly forecast rows. */
export function forecastsCsv(rows) {
  return [
    CSV_HEADERS.join(','),
    ...rows.map((row) => CSV_HEADERS.map((h) => `"${String(row[h] ?? '').replace(/"/g, '""')}"`).join(',')),
  ].join('\n');
}

/** Downloads the rows as monthly-forecasts-YYYY-MM-DD.csv. */
export function downloadForecastsCsv(rows) {
  const blob = new Blob([forecastsCsv(rows)], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `monthly-forecasts-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
