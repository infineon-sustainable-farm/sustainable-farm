import { EllipsisVertical, Goal, Pencil, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, IconButton } from '../../../../shared/ui';
import { useRevenueFormat } from './channelModel';

const MENU_ITEM =
  'flex w-full items-center gap-2 px-4 py-2 text-left text-sm transition duration-(--duration-fast) ease-standard hover:bg-surface-muted';

/** Channel tile: name, revenue target of its latest period, id, and a menu of actions. */
function ChannelCard({ channel, menuOpen, onToggleMenu, onEdit, onAddTarget, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  const formatRevenue = useRevenueFormat();
  const Icon = channel.icon;

  return (
    <Card>
      <div className="mb-6 flex items-start justify-between">
        <div className="flex min-w-0 items-center gap-2">
          <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-pill ${channel.iconBg}`}>
            <Icon size={20} aria-hidden="true" />
          </div>
          <h2 className="truncate text-lg font-semibold">{channel.name}</h2>
        </div>
        <div className="relative">
          <IconButton
            icon={EllipsisVertical}
            tone="neutral"
            label={t('channels.menuFor', { name: channel.name })}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={(event) => {
              event.stopPropagation();
              onToggleMenu();
            }}
          />
          {menuOpen && (
            <div
              role="menu"
              onClick={(event) => event.stopPropagation()}
              className="absolute right-0 top-full z-(--z-overlay) mt-1 w-48 rounded-control border border-border-subtle bg-surface-raised py-1 shadow-overlay"
            >
              <button type="button" role="menuitem" className={MENU_ITEM} onClick={onEdit}>
                <Pencil size={16} aria-hidden="true" className="text-text-muted" />
                {t('channels.editChannel')}
              </button>
              <button type="button" role="menuitem" className={MENU_ITEM} onClick={onAddTarget}>
                <Goal size={16} aria-hidden="true" className="text-text-muted" />
                {t('channels.addTarget')}
              </button>
              <button type="button" role="menuitem" className={`${MENU_ITEM} text-error-fg`} onClick={onDelete}>
                <Trash2 size={16} aria-hidden="true" />
                {t('channels.deleteChannel')}
              </button>
            </div>
          )}
        </div>
      </div>
      <dl className="flex flex-col gap-4">
        <div>
          <dt className="text-xs text-text-muted">{t('channels.revenueTarget')}</dt>
          <dd className="text-2xl font-bold">{formatRevenue(channel.revenueTargetEur)}</dd>
        </div>
        <div>
          <dt className="text-xs text-text-muted">{t('channels.channelId')}</dt>
          <dd className="text-xl font-semibold">{channel.channelId ?? t('channels.none')}</dd>
        </div>
      </dl>
    </Card>
  );
}

export default ChannelCard;
