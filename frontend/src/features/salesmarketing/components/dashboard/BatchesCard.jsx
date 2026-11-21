import { RefreshCw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Card, IconButton, Table } from '../../../../shared/ui';
import { useDashboardFormat } from './dashboardData';

const STATUS = {
  processed: { tone: 'success', key: 'processed' },
  'in transit': { tone: 'warning', key: 'inTransit' },
  in_transit: { tone: 'warning', key: 'inTransit' },
  scheduled: { tone: 'info', key: 'scheduled' },
  delayed: { tone: 'error', key: 'delayed' },
};

function BatchStatus({ status }) {
  const { t } = useTranslation('salesmarketing');
  const known = STATUS[(status || '').toLowerCase()];
  if (!known) return <Badge>{status || t('dashboard.none')}</Badge>;
  return <Badge tone={known.tone}>{t(`dashboard.batchStatus.${known.key}`)}</Badge>;
}

/** Product batches from product transformation, with a refresh button. */
function BatchesCard({ batches, onRefresh, className }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDashboardFormat();
  const columns = [
    { key: 'batchCode', header: t('dashboard.batches.id'), cell: (batch) => batch.batchCode || `#BT-${batch.batchId}` },
    {
      key: 'stockT',
      header: t('dashboard.batches.stock'),
      cell: (batch) => (batch.stockT != null ? t('dashboard.tons', { value: format.number(batch.stockT) }) : format.none),
    },
    { key: 'status', header: t('dashboard.batches.status'), align: 'right', cell: (batch) => <BatchStatus status={batch.status} /> },
  ];

  return (
    <Card
      className={className}
      title={t('dashboard.batches.title')}
      actions={<IconButton icon={RefreshCw} label={t('dashboard.batches.refresh')} onClick={onRefresh} />}
    >
      {batches.length === 0 ? (
        <p className="py-4 text-sm text-text-muted">{t('dashboard.batches.empty')}</p>
      ) : (
        <Table caption={t('dashboard.batches.title')} columns={columns} rows={batches} getRowKey={(batch) => batch.batchId} />
      )}
    </Card>
  );
}

export default BatchesCard;
