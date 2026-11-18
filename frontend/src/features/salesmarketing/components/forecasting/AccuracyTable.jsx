import { useTranslation } from 'react-i18next';
import { Card, Table } from '../../../../shared/ui';
import { useForecastFormat } from './forecastFormat';

/** Base forecasts next to the actual sales of their month. */
function AccuracyTable({ rows }) {
  const { t } = useTranslation('salesmarketing');
  const format = useForecastFormat();
  const columns = [
    { key: 'forecastMonth', header: t('forecasting.columns.month'), cell: (row) => format.month(row.forecastMonth) },
    {
      key: 'forecastedVolumeT',
      header: t('forecasting.columns.forecastMt'),
      cell: (row) => format.metricTons(Number(row.forecastedVolumeT ?? 0)),
    },
    {
      key: 'actualVolumeT',
      header: t('forecasting.columns.actualMt'),
      cell: (row) => (row.actualVolumeT === null ? t('forecasting.notAvailable') : format.metricTons(row.actualVolumeT)),
    },
    {
      key: 'accuracy',
      header: t('forecasting.columns.accuracy'),
      className: 'font-semibold',
      cell: (row) => (row.accuracy === null ? t('forecasting.notAvailable') : format.percent(row.accuracy)),
    },
  ];

  return (
    <Card title={t('forecasting.accuracy.title')} subtitle={t('forecasting.accuracy.subtitle')}>
      <Table
        caption={t('forecasting.accuracy.title')}
        columns={columns}
        rows={rows}
        getRowKey={(row) => `accuracy-${row.forecastId}`}
        emptyMessage={t('forecasting.accuracy.empty')}
      />
    </Card>
  );
}

export default AccuracyTable;
