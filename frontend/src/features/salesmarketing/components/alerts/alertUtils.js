import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

export { getErrorMessage } from '../reports/reportUtils';

export const PAGE_SIZE = 5;

// Default values of a new alert, sent as is to the API.
export const emptyAlertForm = {
  alertCode: '',
  alertType: 'High Demand',
  severity: 'critical',
  message: '',
  region: 'Germany',
  origin: 'Burkina Faso',
  status: 'open',
};

export const SEVERITIES = ['critical', 'warning', 'info'];
export const STATUSES = ['open', 'acknowledged', 'resolved', 'dismissed'];

export function isActiveStatus(status) {
  const s = (status || 'open').toUpperCase();
  return s === 'OPEN' || s === 'ACKNOWLEDGED' || s === '';
}

export function isResolvedStatus(status) {
  const s = (status || '').toUpperCase();
  return s === 'RESOLVED' || s === 'DISMISSED' || s === 'CLOSED';
}

export function severityBucket(alert) {
  const sev = (alert.severity || '').toUpperCase();
  const type = (alert.alertType || '').toLowerCase();
  if (sev === 'CRITICAL' || type.includes('high')) return 'high';
  if (sev === 'WARNING' || type.includes('low')) return 'medium';
  if (sev === 'INFO' || type.includes('deviation') || type.includes('data')) return 'data';
  return 'data';
}

/** Kind of an alert card: high demand, low demand, or data check. */
export function alertKind(severity, type) {
  const sev = (severity || '').toUpperCase();
  if (sev === 'CRITICAL' || type === 'High Demand') return 'high';
  if (sev === 'WARNING' || type === 'Low Demand') return 'low';
  return 'data';
}

/** Relative times, tons and titles of the alerts screen, in the active language. */
export function useAlertText() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(
    () => ({
      none: t('alerts.none'),
      relativeTime(dateStr) {
        if (!dateStr) return t('alerts.none');
        const date = new Date(dateStr);
        if (Number.isNaN(date.getTime())) return String(dateStr);
        const mins = Math.floor((Date.now() - date.getTime()) / 60000);
        if (mins < 1) return t('alerts.justNow');
        if (mins < 60) return t('alerts.minutesAgo', { count: mins });
        const hours = Math.floor(mins / 60);
        if (hours < 24) return t('alerts.hoursAgo', { count: hours });
        return t('alerts.daysAgo', { count: Math.floor(hours / 24) });
      },
      tons(value) {
        if (value == null || Number.isNaN(Number(value))) return t('alerts.none');
        return t('alerts.tons', { value: format.number(value, { maximumFractionDigits: 0 }) });
      },
      euros(value) {
        return t('alerts.euros', { value: format.number(Math.round(value)) });
      },
      title(alert) {
        if (alert.region) return t('alerts.regionTitle', { region: alert.region });
        return alert.alertType || alert.alertCode || t('alerts.numbered', { id: alert.alertId });
      },
    }),
    [t, format]
  );
}
