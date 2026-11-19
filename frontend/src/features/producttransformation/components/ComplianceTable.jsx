import { Eye } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IconButton, StatusBadge, Table } from '../../../shared/ui';
import { formatIsoDate, getComplianceTone } from '../utils/format';
import RowActions from './RowActions';
import { complianceResultLabel, complianceTypeLabel, nextAuditLabel } from './complianceOptions';

/** Table of the compliance records, with View, Edit and Delete on each row. */
export default function ComplianceTable({ records = [], auditorName, onView, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'recordId', header: t('compliance.columns.record'), className: 'whitespace-nowrap' },
    { key: 'batchId', header: t('compliance.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'complianceType',
      header: t('compliance.columns.type'),
      className: 'whitespace-nowrap',
      cell: (row) => complianceTypeLabel(row.complianceType),
    },
    {
      key: 'result',
      header: t('compliance.columns.result'),
      cell: (row) => <StatusBadge tone={getComplianceTone(row.result)} label={complianceResultLabel(row.result)} />,
    },
    {
      key: 'auditDate',
      header: t('compliance.columns.auditDate'),
      className: 'whitespace-nowrap',
      cell: (row) => formatIsoDate(row.auditDate),
    },
    {
      key: 'nextAuditDate',
      header: t('compliance.columns.nextAudit'),
      className: 'whitespace-nowrap',
      cell: (row) => nextAuditLabel(row.nextAuditDate),
    },
    {
      key: 'auditor',
      header: t('compliance.columns.auditor'),
      className: 'whitespace-nowrap',
      cell: (row) => auditorName(row.auditorId),
    },
    {
      key: 'status',
      header: t('compliance.columns.status'),
      cell: (row) =>
        row.auditOverdue ? (
          <StatusBadge tone="warning" label={t('compliance.overdue')} />
        ) : (
          <span className="text-xs text-text-muted">{t('compliance.current')}</span>
        ),
    },
  ];

  return (
    <Table
      caption={t('compliance.title')}
      columns={columns}
      rows={records}
      getRowKey={(row) => row.recordId}
      loading={loading}
      rowActions={(record) => (
        <RowActions onEdit={() => onEdit(record)} onDelete={() => onDelete(record)}>
          <IconButton icon={Eye} label={t('compliance.view')} tone="neutral" onClick={() => onView(record)} />
        </RowActions>
      )}
    />
  );
}
