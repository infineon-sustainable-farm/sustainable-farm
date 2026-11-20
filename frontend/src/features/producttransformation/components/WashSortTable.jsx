import { useTranslation } from 'react-i18next';
import { Table } from '../../../shared/ui';
import { formatDateTime, formatNumber, formatWeight, notAvailable } from '../utils/format';
import { equipmentLabelOf, operatorLabelOf } from './processRun';
import RowActions from './RowActions';

const isMissing = (value) => value === null || value === undefined;

/** Table of the wash & sort records, with Edit and Delete on each row. */
export default function WashSortTable({ records = [], equipment = [], operators = [], onEdit, onDelete, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'recordId', header: t('washSort.columns.record'), className: 'whitespace-nowrap' },
    { key: 'batchId', header: t('washSort.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'quantities',
      header: t('washSort.columns.quantities'),
      className: 'whitespace-nowrap',
      cell: (row) => (
        <>
          <div>
            {t('washSort.outputOfInput', {
              output: formatWeight(row.outputQuantityKg),
              input: formatWeight(row.inputQuantityKg),
            })}
          </div>
          <div className="text-xs text-text-muted">{t('washSort.waste', { value: formatWeight(row.wasteQuantityKg) })}</div>
        </>
      ),
    },
    {
      key: 'yield',
      header: t('washSort.columns.yield'),
      className: 'whitespace-nowrap',
      cell: (row) =>
        isMissing(row.yieldPercentage)
          ? notAvailable()
          : t('format.percent', { value: formatNumber(row.yieldPercentage, 1) }),
    },
    {
      key: 'water',
      header: t('washSort.columns.water'),
      className: 'whitespace-nowrap',
      cell: (row) =>
        isMissing(row.waterUsageLiters)
          ? notAvailable()
          : t('washSort.liters', { value: formatNumber(row.waterUsageLiters, 0) }),
    },
    {
      key: 'period',
      header: t('washSort.columns.period'),
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
      header: t('washSort.columns.assignment'),
      className: 'whitespace-nowrap',
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
      caption={t('washSort.title')}
      columns={columns}
      rows={records}
      getRowKey={(row) => row.recordId}
      loading={loading}
      rowActions={(record) => <RowActions onEdit={() => onEdit(record)} onDelete={() => onDelete(record)} />}
    />
  );
}
