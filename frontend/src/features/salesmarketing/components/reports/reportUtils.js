import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { todayIsoDate } from '../../../../shared/utils/todayIsoDate';
import { localIsoDate, useSalesFormat } from '../../format';

export const REPORT_TYPES = ['Forecasting', 'Regional', 'Risk Analysis', 'Sales Performance', 'Inventory'];
// Translation keys of the report types, whose English names are the values sent to the API.
export const REPORT_TYPE_KEYS = {
  Forecasting: 'forecasting',
  Regional: 'regional',
  'Risk Analysis': 'risk',
  'Sales Performance': 'sales',
  Inventory: 'inventory',
};
export const STATUS_OPTIONS = ['ready', 'processing', 'scheduled', 'failed'];
export const SCHEDULE_FREQUENCIES = ['Daily', 'Weekly', 'Monthly', 'Quarterly'];

/** Message of a failed request; field errors of the API are appended to its message. */
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

export function emptyReportForm() {
  const today = new Date();
  const nextMonth = new Date(today);
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  return {
    reportType: 'Forecasting',
    periodStart: localIsoDate(today),
    periodEnd: localIsoDate(nextMonth),
    totalForecastedVolumeT: '',
    totalActualVolumeT: '',
    variancePct: '',
    status: 'ready',
    isScheduled: false,
    nextRunAt: '',
    scheduleFrequency: '',
    summary: '',
    generatedByPersonId: '',
    generatedByName: '',
  };
}

export function resolveCategory(reportType = '') {
  const type = reportType.toLowerCase();
  if (type.includes('region') || type.includes('germany') || type.includes('regional')) return 'regional';
  if (type.includes('risk') || type.includes('alert')) return 'risk';
  if (type.includes('forecast') || type.includes('scenario') || type.includes('demand')) return 'forecasting';
  return 'forecasting';
}

/** Status of a report as filtered and shown in the table. */
export function reportStatus(report) {
  return report.status || (report.isScheduled ? 'scheduled' : 'ready');
}

/** Title, summary and period of a report, in the active language. */
export function useReportText() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(
    () => ({
      title(report) {
        if (report.title) return report.title;
        if (report.reportType) return t('reports.typeReport', { type: report.reportType });
        return t('reports.numbered', { id: report.reportId });
      },
      summary(report) {
        if (report.summary) return report.summary;
        const volume =
          report.totalForecastedVolumeT != null
            ? t('reports.summaryVolume', { value: format.number(report.totalForecastedVolumeT) })
            : null;
        const variance =
          report.variancePct != null
            ? t('reports.summaryVariance', {
                value: format.number(report.variancePct, { maximumFractionDigits: 20, useGrouping: false }),
              })
            : null;
        return [volume, variance].filter(Boolean).join(' · ') || t('reports.summaryDefault');
      },
      period(report) {
        if (report.isScheduled && report.nextRunAt) {
          const date = new Date(report.nextRunAt);
          if (!Number.isNaN(date.getTime())) {
            return t('reports.nextRun', {
              date: format.dateTime(date, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
            });
          }
        }
        if (report.generatedAt) {
          const date = new Date(report.generatedAt);
          if (!Number.isNaN(date.getTime())) {
            return format.date(date, { month: 'short', day: 'numeric', year: 'numeric' });
          }
        }
        if (report.periodStart && report.periodEnd) {
          return t('reports.periodRange', { start: report.periodStart, end: report.periodEnd });
        }
        return t('reports.currentPeriod');
      },
    }),
    [t, format]
  );
}

/** Downloads one report as a CSV file built in the browser. */
export function downloadReportCsv(report) {
  const headers = [
    'reportId', 'reportType', 'periodStart', 'periodEnd',
    'totalForecastedVolumeT', 'totalActualVolumeT', 'variancePct',
    'status', 'summary',
  ];
  const row = headers
    .map((header) => {
      const value = report[header] ?? '';
      return `"${String(value).replace(/"/g, '""')}"`;
    })
    .join(',');
  const csv = [headers.join(','), row].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `report-${report.reportId || 'export'}-${todayIsoDate()}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}
