import { Globe, MapPin, SearchCheck, TrendingDown, TrendingUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Button } from '../../../../shared/ui';
import { alertKind, isActiveStatus, useAlertText } from './alertUtils';

const KINDS = {
  high: { border: 'border-l-danger', tile: 'bg-error-tint text-error-fg', badge: 'error', icon: TrendingUp, action: 'alerts.reviewProduction' },
  low: { border: 'border-l-warning-fg', tile: 'bg-warning-tint text-warning-fg', badge: 'warning', icon: TrendingDown, action: 'alerts.reviewStrategy' },
  data: { border: 'border-l-primary', tile: 'bg-primary-tint text-primary', badge: 'info', icon: SearchCheck, action: 'alerts.checkData' },
};

/** One alert with its resolve, edit and dismiss actions. */
function AlertCard({ alert, resolving, onResolve, onEdit, onDismiss }) {
  const { t } = useTranslation('salesmarketing');
  const text = useAlertText();
  const kind = KINDS[alertKind(alert.severity, alert.alertType)];
  const Icon = kind.icon;
  const resolved = !isActiveStatus(alert.status);

  return (
    <li
      className={`rounded-card border border-l-4 border-border-subtle bg-surface-raised p-5 shadow-raised ${kind.border} ${
        resolved ? 'opacity-70' : ''
      }`}
    >
      <div className="flex flex-col items-start justify-between gap-6 sm:flex-row">
        <div className="flex w-full gap-4 sm:w-3/4">
          <div className={`mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-pill ${kind.tile}`}>
            <Icon size={18} aria-hidden="true" />
          </div>
          <div>
            <div className="mb-1 flex flex-wrap items-center gap-2 text-xs text-text-muted">
              <Badge tone={kind.badge}>{alert.alertType || alert.severity || t('alerts.alertBadge')}</Badge>
              <span>{alert.alertCode || t('alerts.defaultCode', { id: alert.alertId })}</span>
              <span aria-hidden="true">·</span>
              <span>{text.relativeTime(alert.createdAt)}</span>
              {resolved && <Badge tone="neutral">{alert.status}</Badge>}
            </div>
            <h3 className="mb-1 text-lg font-semibold">{text.title(alert)}</h3>
            <p className="line-clamp-2 text-sm text-text-muted">{alert.message}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Badge size="md" icon={MapPin}>
                {t('alerts.region', { region: alert.region || text.none })}
              </Badge>
              <Badge size="md" icon={Globe}>
                {t('alerts.origin', { origin: alert.origin || text.none })}
              </Badge>
            </div>
          </div>
        </div>
        <div className="flex w-full shrink-0 justify-end gap-2 sm:w-auto sm:flex-col sm:justify-start">
          {!resolved && (
            <Button size="sm" disabled={resolving} onClick={() => onResolve(alert)}>
              {resolving ? t('alerts.resolving') : t(kind.action)}
            </Button>
          )}
          <Button size="sm" variant="secondary" onClick={() => onEdit(alert)}>
            {t('common:actions.edit')}
          </Button>
          <Button size="sm" variant="ghost" onClick={() => onDismiss(alert)}>
            {t('common:actions.dismiss')}
          </Button>
        </div>
      </div>
    </li>
  );
}

export default AlertCard;
