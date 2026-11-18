import { ArrowRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, StatusBadge, Table } from '../../../shared/ui';
import { formatDate, formatWeight, getStatusTone } from '../utils/format';
import { batchStatusCode, canAdvance } from './batchOptions';

/** Table of the batches, with Advance on the batches that can move to the next status. */
export default function BatchTable({ batches = [], onAdvance, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'batchId', header: t('batches.columns.batchId'), className: 'whitespace-nowrap' },
    {
      key: 'harvestDate',
      header: t('batches.columns.harvestDate'),
      className: 'whitespace-nowrap',
      cell: (row) => formatDate(row.harvestDate),
    },
    { key: 'mangoVariety', header: t('batches.columns.variety') },
    {
      key: 'harvestQuantityKg',
      header: t('batches.columns.quantity'),
      className: 'whitespace-nowrap',
      cell: (row) => formatWeight(row.harvestQuantityKg),
    },
    {
      key: 'currentStatus',
      header: t('batches.columns.status'),
      cell: (row) => <StatusBadge tone={getStatusTone(row.currentStatus)} label={batchStatusCode(row.currentStatus)} />,
    },
    {
      key: 'location',
      header: t('batches.columns.location'),
      className: 'whitespace-nowrap',
      cell: (row) => t('format.source', { farm: row.farmId, block: row.blockId }),
    },
  ];

  return (
    <Table
      caption={t('batches.title')}
      columns={columns}
      rows={batches}
      getRowKey={(row) => row.batchId}
      loading={loading}
      rowActions={(batch) =>
        canAdvance(batch) && (
          <Button variant="ghost" size="sm" onClick={() => onAdvance(batch.batchId)}>
            {t('batches.advance')}
            <ArrowRight size={16} aria-hidden="true" />
          </Button>
        )
      }
    />
  );
}
