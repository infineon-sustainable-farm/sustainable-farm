import { BellRing, Timer, TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { KpiCard } from '../../../../shared/ui';
import { useAlertText } from './alertUtils';

/** Base case volume, high priority alerts and revenue potential. */
function AlertKpis({ hasForecasts, totalBase, activeCount, highActiveCount, revenuePotentialEur }) {
  const { t } = useTranslation('salesmarketing');
  const text = useAlertText();
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <KpiCard
        icon={BellRing}
        label={t('alerts.kpi.baseCase')}
        value={hasForecasts ? text.tons(totalBase) : t('alerts.kpi.noData')}
        hint={t('alerts.kpi.activeHint', { count: activeCount })}
      />
      <KpiCard
        icon={TriangleAlert}
        tone="error"
        label={t('alerts.kpi.germanyBaseCase')}
        value={t('alerts.kpi.noData')}
        hint={highActiveCount > 0 ? t('alerts.kpi.highPriority', { count: highActiveCount }) : t('alerts.kpi.noHigh')}
      />
      <KpiCard
        icon={Timer}
        tone="warning"
        label={t('alerts.kpi.revenue')}
        value={revenuePotentialEur > 0 ? text.euros(revenuePotentialEur) : text.none}
        hint={t('alerts.kpi.revenueHint')}
      />
    </div>
  );
}

export default AlertKpis;
