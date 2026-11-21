import { Link2, PackageCheck } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IconButton, StatusBadge, Table } from '../../../shared/ui';
import { formatIsoDate, formatWeight } from '../utils/format';
import { packageLabel } from './packagingOptions';
import RowActions from './RowActions';

/**
 * Table of the packaging records. Row actions: open the batch's compliance records (when
 * `onOpenCompliance` is given), mark as export ready (records not ready yet), edit and delete.
 */
export default function PackagingTable({
  records = [],
  equipmentName,
  operatorName,
  onOpenCompliance,
  onMarkExportReady,
  onEdit,
  onDelete,
  loading = false,
}) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'recordId', header: t('packaging.columns.record'), className: 'whitespace-nowrap' },
    { key: 'batchId', header: t('packaging.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'packageType',
      header: t('packaging.columns.packageType'),
      className: 'whitespace-nowrap',
      cell: (row) => packageLabel(row.packageType),
    },
    {
      key: 'quantity',
      header: t('packaging.columns.quantity'),
      className: 'whitespace-nowrap',
      cell: (row) => formatWeight(row.packageQuantityKg),
    },
    {
      key: 'lotCode',
      header: t('packaging.columns.lotCode'),
      className: 'whitespace-nowrap',
      cell: (row) => (
        <span
          className="text-xs"
          title={t('packaging.lotTitle', {
            lot: row.lotCode,
            equipment: equipmentName(row.equipmentId),
            operator: operatorName(row.operatorId),
          })}
        >
          {row.lotCode}
        </span>
      ),
    },
    {
      key: 'packagingDate',
      header: t('packaging.columns.packagingDate'),
      className: 'whitespace-nowrap',
      cell: (row) => formatIsoDate(row.packagingDate),
    },
    {
      key: 'export',
      header: t('packaging.columns.export'),
      cell: (row) =>
        row.exportReady ? (
          <StatusBadge tone="success" label={t('packaging.exportReady')} />
        ) : (
          <StatusBadge tone="neutral" label={t('packaging.domestic')} />
        ),
    },
  ];

  return (
    <Table
      caption={t('packaging.title')}
      columns={columns}
      rows={records}
      getRowKey={(row) => row.recordId}
      loading={loading}
      rowActions={(record) => (
        <RowActions onEdit={() => onEdit(record)} onDelete={() => onDelete(record)}>
          {onOpenCompliance && (
            <IconButton
              icon={Link2}
              tone="neutral"
              label={t('packaging.compliance')}
              title={t('packaging.complianceTitle')}
              onClick={() => onOpenCompliance(record.batchId)}
            />
          )}
          {!record.exportReady && (
            <IconButton
              icon={PackageCheck}
              label={t('packaging.markReady')}
              title={t('packaging.markReadyTitle')}
              onClick={() => onMarkExportReady(record)}
            />
          )}
        </RowActions>
      )}
    />
  );
}
