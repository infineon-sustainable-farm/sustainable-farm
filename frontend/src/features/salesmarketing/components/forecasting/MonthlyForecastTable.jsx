import { Download, Pencil, Trash2, TrendingUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Card, IconButton, Table } from '../../../../shared/ui';
import { useForecastFormat } from './forecastFormat';
import RiskBadge from './RiskBadge';

/** Filtered forecast rows by month, with their growth, risk, edit and delete. */
function MonthlyForecastTable({ rows, confidenceInterval, onExport, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  const format = useForecastFormat();
  const columns = [
    {
      key: 'forecastMonth',
      header: t('forecasting.columns.month'),
      cell: (row) => (
        <>
          {format.month(row.forecastMonth)}
          {row.scenario ? (
            <span className="ml-2 text-xs font-normal text-text-muted">
              {t('forecasting.scenarioSuffix', { scenario: row.scenario })}
            </span>
          ) : null}
        </>
      ),
    },
    {
      key: 'forecastedVolumeT',
      header: t('forecasting.columns.predicted'),
      className: 'text-lg font-bold',
      cell: (row) => format.volume(row.forecastedVolumeT),
    },
    {
      key: 'lowerBoundT',
      header: t('forecasting.columns.lower', { value: confidenceInterval }),
      className: 'text-text-muted',
      cell: (row) => format.volume(row.lowerBoundT),
    },
    {
      key: 'upperBoundT',
      header: t('forecasting.columns.upper', { value: confidenceInterval }),
      className: 'text-text-muted',
      cell: (row) => format.volume(row.upperBoundT),
    },
    {
      key: 'momGrowth',
      header: t('forecasting.columns.growth'),
      className: 'text-text-muted',
      cell: (row) =>
        row.momGrowth && String(row.momGrowth).includes('+') ? (
          <span className="flex items-center gap-1 font-semibold text-success-fg">
            <TrendingUp size={16} aria-hidden="true" />
            {format.growth(row)}
          </span>
        ) : (
          format.growth(row)
        ),
    },
    { key: 'capacityRisk', header: t('forecasting.columns.risk'), cell: (row) => <RiskBadge risk={row.capacityRisk} /> },
  ];

  return (
    <Card
      title={t('forecasting.monthly.title')}
      actions={
        <Button variant="ghost" icon={Download} onClick={onExport}>
          {t('forecasting.monthly.export')}
        </Button>
      }
    >
      <Table
        caption={t('forecasting.monthly.title')}
        columns={columns}
        rows={rows}
        getRowKey={(row) => row.forecastId}
        emptyMessage={t('forecasting.monthly.empty')}
        rowActions={(row) => (
          <div className="inline-flex items-center gap-1">
            <IconButton icon={Pencil} label={t('common:actions.edit')} onClick={() => onEdit(row)} />
            <IconButton icon={Trash2} tone="danger" label={t('common:actions.delete')} onClick={() => onDelete(row)} />
          </div>
        )}
      />
    </Card>
  );
}

export default MonthlyForecastTable;
