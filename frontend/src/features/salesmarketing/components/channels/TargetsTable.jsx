import { Pencil, Plus, Shapes, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Card, IconButton, Select, Table } from '../../../../shared/ui';
import { useRevenueFormat } from './channelModel';

/** Revenue targets by period, filtered by channel. */
function TargetsTable({ targets, channels, selectedChannelId, onSelectChannel, onCreate, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  const formatRevenue = useRevenueFormat();

  const columns = [
    {
      key: 'channel',
      header: t('channels.table.channel'),
      cell: (target) => {
        const ch = channels.find((c) => c.channelId === target.channelId);
        const Icon = ch?.icon ?? Shapes;
        return (
          <div className="flex items-center gap-2">
            <div
              className={`flex h-9 w-9 items-center justify-center rounded-pill ${ch?.iconBg || 'bg-neutral-bg text-neutral-fg'}`}
            >
              <Icon size={16} aria-hidden="true" />
            </div>
            <div>
              <p className="font-medium">{ch?.name || t('channels.channelNumber', { id: target.channelId })}</p>
              <p className="text-sm font-normal text-text-muted">{ch?.shortLabel || target.channelId}</p>
            </div>
          </div>
        );
      },
    },
    {
      key: 'period',
      header: t('channels.table.period'),
      cell: (target) => (
        <span className="text-sm">{t('channels.period', { start: target.periodStart, end: target.periodEnd })}</span>
      ),
    },
    {
      key: 'revenue',
      header: t('channels.table.targetRevenue'),
      cell: (target) => <span className="font-semibold">{formatRevenue(target.revenueTargetEur)}</span>,
    },
  ];

  return (
    <Card
      padding="none"
      className="overflow-hidden"
    >
      <div className="flex flex-wrap items-center justify-between gap-4 p-5">
        <div>
          <h2 className="text-lg font-semibold">{t('channels.targetsTitle')}</h2>
          <p className="text-sm text-text-muted">{t('channels.targetsSubtitle')}</p>
        </div>
        <div className="flex items-center gap-3">
          <Select
            aria-label={t('channels.filterChannel')}
            value={selectedChannelId || ''}
            onChange={(e) => onSelectChannel(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">{t('channels.allChannels')}</option>
            {channels.map((ch) => (
              <option key={ch.channelId || ch.key} value={ch.channelId}>
                {ch.name}
              </option>
            ))}
          </Select>
          <Button icon={Plus} onClick={() => onCreate()} className="shrink-0">
            {t('channels.newTarget')}
          </Button>
        </div>
      </div>
      <div className="px-5 pb-5">
        <Table
          caption={t('channels.targetsTitle')}
          columns={columns}
          rows={targets}
          getRowKey={(target) => target.targetId}
          emptyMessage={t('channels.noTargets')}
          rowActions={(target) => (
            <div className="flex justify-end gap-1">
              <IconButton icon={Pencil} label={t('channels.editTarget')} onClick={() => onEdit(target)} />
              <IconButton icon={Trash2} tone="danger" label={t('channels.deleteTarget')} onClick={() => onDelete(target)} />
            </div>
          )}
        />
      </div>
    </Card>
  );
}

export default TargetsTable;
