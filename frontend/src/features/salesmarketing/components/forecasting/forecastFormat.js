import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

const ONE_DECIMAL = { minimumFractionDigits: 1, maximumFractionDigits: 1, useGrouping: false };

/** Month, ton and percentage formatters of the forecasting screen, in the active language. */
export function useForecastFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(() => {
    const none = t('forecasting.none');
    return {
      none,
      /** "Mar 2026"; the raw value when it is not a date. */
      month(value) {
        if (!value) return '';
        return format.date(value, { month: 'short', year: 'numeric' });
      },
      /** "Mar", for the chart axis. */
      shortMonth(value) {
        return format.date(value, { month: 'short' });
      },
      /** Whole tons: "1,250". */
      tons(value) {
        if (value == null || Number.isNaN(Number(value))) return none;
        return format.number(value, { maximumFractionDigits: 0 });
      },
      /** Volume as the API sends it: "1,250.5". */
      volume(value) {
        return value != null ? format.number(value) : none;
      },
      /** "12.5 MT". */
      metricTons(value) {
        return t('forecasting.metricTons', { value: format.number(value, ONE_DECIMAL) });
      },
      /** "92.5%". */
      percent(value) {
        return t('forecasting.percent', { value: format.number(value, ONE_DECIMAL) });
      },
      /** Month-over-month growth: "-", "0%", "+∞", "+12.5%". */
      growth(row) {
        if (row.momValue == null) return row.momGrowth || '-';
        if (row.momValue === Infinity) return row.momGrowth;
        if (row.momValue === 0 && row.momGrowth === '0%') return t('forecasting.percent', { value: format.number(0) });
        const sign = row.momValue > 0 ? '+' : '';
        return `${sign}${t('forecasting.percent', { value: format.number(row.momValue, ONE_DECIMAL) })}`;
      },
    };
  }, [t, format]);
}
