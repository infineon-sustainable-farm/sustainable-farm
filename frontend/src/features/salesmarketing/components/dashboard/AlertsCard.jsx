import { Clock, OctagonAlert, TriangleAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';

/** First five alerts; a click asks to delete the alert. */
function AlertsCard({ alerts, onSelect }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <TriangleAlert size={20} aria-hidden="true" className="text-error-fg" />
          {t('dashboard.alerts.title')}
        </span>
      }
    >
      <ul className="flex flex-col gap-2">
        {alerts.length === 0 ? (
          <li className="text-sm text-text-muted">{t('dashboard.alerts.empty')}</li>
        ) : (
          alerts.slice(0, 5).map((alert) => {
            const high = (alert.severity || '').toUpperCase() === 'HIGH';
            const Icon = high ? OctagonAlert : Clock;
            return (
              <li key={alert.alertId}>
                <button
                  type="button"
                  onClick={() => onSelect(alert)}
                  title={t('dashboard.alerts.clickToDelete')}
                  className={`flex w-full items-start gap-3 rounded-control border-l-2 p-2 text-left transition duration-(--duration-fast) ease-standard hover:bg-surface-muted ${
                    high ? 'border-error-fg' : 'border-warning-fg'
                  }`}
                >
                  <span
                    className={`mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-pill ${
                      high ? 'bg-error-tint text-error-fg' : 'bg-warning-tint text-warning-fg'
                    }`}
                  >
                    <Icon size={16} aria-hidden="true" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium">{alert.alertCode || t('dashboard.alerts.fallbackCode', { id: alert.alertId })}</span>
                    <span className="mt-1 line-clamp-2 block text-sm text-text-muted">{alert.message}</span>
                  </span>
                </button>
              </li>
            );
          })
        )}
      </ul>
      <Link
        to="/demand-alerts"
        className="mt-4 block rounded-control py-2 text-center text-sm font-medium text-text-accent transition duration-(--duration-fast) ease-standard hover:bg-primary-tint"
      >
        {t('dashboard.alerts.viewAll')}
      </Link>
    </Card>
  );
}

export default AlertsCard;
