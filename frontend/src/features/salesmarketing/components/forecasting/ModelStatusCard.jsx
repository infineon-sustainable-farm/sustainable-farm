import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';
import { useForecastFormat } from './forecastFormat';

function Figure({ label, value }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-text-muted">{label}</dt>
      <dd className="font-semibold">{value}</dd>
    </div>
  );
}

/** Status of the latest model run and the volume of each scenario. */
function ModelStatusCard({ latestRun, runCount, totals }) {
  const { t } = useTranslation('salesmarketing');
  const format = useForecastFormat();
  const tons = (value) => t('forecasting.tons', { value: format.tons(value) });

  return (
    <Card className="bg-primary-tint">
      <h2 className="text-sm font-medium text-text-muted">{t('forecasting.status.title')}</h2>
      <p className="text-lg font-bold">{latestRun?.modelStatus || t('forecasting.coldStart')}</p>
      <p className="mt-2 text-sm text-text-muted">
        {runCount > 0
          ? t('forecasting.status.runs', { count: runCount, id: latestRun?.runId })
          : t('forecasting.status.noAccuracy')}
      </p>
      <dl className="mt-4 grid grid-cols-2 gap-2">
        <Figure label={t('forecasting.status.pessimistic')} value={tons(totals.pessimistic)} />
        <Figure label={t('forecasting.status.base')} value={tons(totals.base)} />
        <Figure label={t('forecasting.status.optimistic')} value={tons(totals.optimistic)} />
        <Figure label={t('forecasting.status.regionalBase')} value={t('forecasting.status.noData')} />
      </dl>
      <dl className="mt-6">
        <Figure label={t('forecasting.status.revenue')} value={format.none} />
      </dl>
    </Card>
  );
}

export default ModelStatusCard;
