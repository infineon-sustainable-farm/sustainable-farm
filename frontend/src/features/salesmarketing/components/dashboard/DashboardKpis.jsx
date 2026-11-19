import { BellRing, Euro, MapPin, TrendingUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { KpiCard } from '../../../../shared/ui';
import { useDashboardFormat } from './dashboardData';

/** The four key figures of the dashboard. */
function DashboardKpis({ hasForecasts, totalBase, totalLower, totalUpper, germanyDemandT, alertCount, degraded, priceEurPerKg }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDashboardFormat();
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      <KpiCard
        icon={TrendingUp}
        label={t('dashboard.kpi.forecasted')}
        value={hasForecasts ? format.tons(totalBase) : t('dashboard.noData')}
        hint={
          hasForecasts
            ? t('dashboard.kpi.range', { lower: format.plainTons(totalLower), upper: format.tons(totalUpper) })
            : t('dashboard.kpi.awaiting')
        }
      />
      <KpiCard
        icon={MapPin}
        tone="warning"
        label={t('dashboard.kpi.germany')}
        value={germanyDemandT > 0 ? t('dashboard.kpi.metricTons', { value: format.fixed(germanyDemandT, 1) }) : t('dashboard.noData')}
        hint={t('dashboard.kpi.noRegional')}
      />
      <KpiCard
        icon={BellRing}
        tone="info"
        label={t('dashboard.kpi.activeAlerts')}
        value={t('dashboard.kpi.activeCount', { number: alertCount })}
        hint={degraded ? t('dashboard.kpi.degraded') : t('dashboard.kpi.operational')}
      />
      <KpiCard
        icon={Euro}
        tone="error"
        label={t('dashboard.kpi.revenue')}
        value={hasForecasts ? format.euroMillions(totalBase, priceEurPerKg) : t('dashboard.noData')}
        hint={
          priceEurPerKg != null
            ? t('dashboard.kpi.latestPrice', { price: format.fixed(priceEurPerKg, 2) })
            : t('dashboard.kpi.noPricing')
        }
      />
    </div>
  );
}

export default DashboardKpis;
