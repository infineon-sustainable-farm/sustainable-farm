import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';

/** Active alerts per region (top five), as bars relative to the busiest region. */
export function AnomalyHotspots({ rows }) {
  const { t } = useTranslation('salesmarketing');
  const max = rows[0]?.[1] || 1;
  return (
    <Card title={t('alerts.hotspots')} titleAs="h2">
      {rows.length === 0 ? (
        <p className="text-sm text-text-muted">{t('alerts.noHotspots')}</p>
      ) : (
        <ul className="space-y-2">
          {rows.map(([region, count]) => (
            <li key={region} className="flex items-center justify-between py-2 text-sm">
              <span>
                {region.includes('German') || region === 'Germany' ? t('alerts.euRegion', { region }) : region}
              </span>
              <div className="flex items-center gap-2">
                <div className="h-2 w-16 overflow-hidden rounded-pill bg-neutral-bg" aria-hidden="true">
                  <div className="h-full bg-danger" style={{ width: `${Math.max(12, (count / max) * 100)}%` }} />
                </div>
                <span className="font-semibold text-error-fg">{count}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

/** Last five alerts flagged or resolved, newest first. */
export function ResolutionActivity({ items }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <Card title={t('alerts.activity')} titleAs="h2">
      {items.length === 0 ? (
        <p className="text-sm text-text-muted">{t('alerts.noActivity')}</p>
      ) : (
        <ol className="space-y-4">
          {items.map((item) => (
            <li key={item.id} className="flex gap-3">
              <span
                aria-hidden="true"
                className={`mt-1 h-3 w-3 shrink-0 rounded-pill ${item.primary ? 'bg-primary' : 'bg-neutral-bg'}`}
              />
              <div>
                <p className="mb-1 text-sm">{item.text}</p>
                <p className="text-xs text-text-muted">{item.when}</p>
              </div>
            </li>
          ))}
        </ol>
      )}
    </Card>
  );
}
