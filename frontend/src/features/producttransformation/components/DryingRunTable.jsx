import { useTranslation } from 'react-i18next';
import { Table } from '../../../shared/ui';
import { formatDateTime, formatNumber } from '../utils/format';
import { equipmentLabelOf, operatorLabelOf } from './processRun';
import RowActions from './RowActions';

/** Table of the drying runs, with Edit and Delete on each row. */
export default function DryingRunTable({ runs = [], equipment = [], operators = [], onEdit, onDelete, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'runId', header: t('drying.columns.run'), className: 'whitespace-nowrap' },
    { key: 'batchId', header: t('drying.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'duration',
      header: t('drying.columns.duration'),
      className: 'whitespace-nowrap',
      cell: (row) => t('drying.hours', { value: formatNumber(row.durationHours, 2) }),
    },
    {
      key: 'temperature',
      header: t('drying.columns.temperature'),
      className: 'whitespace-nowrap',
      cell: (row) => (
        <>
          {t('drying.celsius', { value: formatNumber(row.actualTemperatureC, 1) })}
          <span className="text-text-muted">
            {' '}
            {t('drying.targetTemperature', {
              value: t('drying.celsius', { value: formatNumber(row.targetTemperatureC, 1) }),
            })}
          </span>
        </>
      ),
    },
    {
      key: 'moisture',
      header: t('drying.columns.moisture'),
      className: 'whitespace-nowrap',
      cell: (row) =>
        t('drying.moistureLoss', {
          start: t('format.percent', { value: formatNumber(row.startMoisturePct, 1) }),
          end: t('format.percent', { value: formatNumber(row.endMoisturePct, 1) }),
        }),
    },
    {
      key: 'energy',
      header: t('drying.columns.energy'),
      className: 'whitespace-nowrap',
      cell: (row) => t('drying.kwh', { value: formatNumber(row.energyUsageKwh, 2) }),
    },
    {
      key: 'period',
      header: t('drying.columns.period'),
      className: 'whitespace-nowrap',
      cell: (row) => (
        <>
          <div>{formatDateTime(row.startTime)}</div>
          <div className="text-xs text-text-muted">{t('washSort.periodEnd', { end: formatDateTime(row.endTime) })}</div>
        </>
      ),
    },
    {
      key: 'assignment',
      header: t('drying.columns.assignment'),
      cell: (row) => (
        <>
          <div>{equipmentLabelOf(equipment, row.equipmentId)}</div>
          <div className="text-xs text-text-muted">{operatorLabelOf(operators, row.operatorId)}</div>
        </>
      ),
    },
  ];

  return (
    <Table
      caption={t('drying.title')}
      columns={columns}
      rows={runs}
      getRowKey={(row) => row.runId}
      loading={loading}
      rowActions={(run) => <RowActions onEdit={() => onEdit(run)} onDelete={() => onDelete(run)} />}
    />
  );
}
