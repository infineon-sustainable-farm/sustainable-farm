import { useTranslation } from 'react-i18next';
import { StatusBadge, Table } from '../../../shared/ui';
import { formatDateTime, getQcResultTone } from '../utils/format';
import RowActions from './RowActions';
import { qcResultLabel, qcStageLabel } from './qcOptions';

/** Table of the QC checkpoints, with Edit and Delete on each row. */
export default function QcCheckpointTable({ checkpoints = [], inspectorName, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    {
      key: 'checkpointId',
      header: t('qc.columns.checkpoint'),
      className: 'whitespace-nowrap',
      cell: (row) => (
        <>
          <div>{row.checkpointId}</div>
          {row.mandatory && <span className="text-xs font-normal text-warning-fg">{t('qc.mandatory')}</span>}
        </>
      ),
    },
    { key: 'batchId', header: t('qc.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'stage',
      header: t('qc.columns.stage'),
      className: 'whitespace-nowrap',
      cell: (row) => qcStageLabel(row.stage),
    },
    {
      key: 'result',
      header: t('qc.columns.result'),
      cell: (row) => <StatusBadge tone={getQcResultTone(row.result)} label={qcResultLabel(row.result)} />,
    },
    { key: 'defectsCount', header: t('qc.columns.defects'), cell: (row) => row.defectsCount ?? 0 },
    {
      key: 'inspector',
      header: t('qc.columns.inspector'),
      className: 'whitespace-nowrap',
      cell: (row) => inspectorName(row.inspectorId),
    },
    {
      key: 'checkpointTime',
      header: t('qc.columns.checkpointTime'),
      className: 'whitespace-nowrap',
      cell: (row) => formatDateTime(row.checkpointTime),
    },
  ];

  return (
    <Table
      caption={t('qc.title')}
      columns={columns}
      rows={checkpoints}
      getRowKey={(row) => row.checkpointId}
      loading={loading}
      rowActions={(checkpoint) => (
        <RowActions onEdit={() => onEdit(checkpoint)} onDelete={() => onDelete(checkpoint)} />
      )}
    />
  );
}
