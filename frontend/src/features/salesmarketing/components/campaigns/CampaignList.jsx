import { Archive, List, Megaphone, Pencil, Plus, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Button, EmptyState, IconButton } from '../../../../shared/ui';
import { normalizeStatus, useCampaignFormat } from './campaignFormat';

const STATUS_TONES = { active: 'success', scheduled: 'warning', ended: 'neutral', draft: 'neutral' };

function CampaignThumb({ status }) {
  const Icon = status === 'draft' ? Pencil : status === 'ended' ? Archive : Megaphone;
  return (
    <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-tile bg-primary-tint text-primary">
      <Icon size={20} aria-hidden="true" />
    </div>
  );
}

function CampaignTile({ campaign, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  const format = useCampaignFormat();
  const status = normalizeStatus(campaign.status);
  const isKnown = Object.hasOwn(STATUS_TONES, status);
  const isEnded = status === 'ended';
  const isDraft = status === 'draft' || !isKnown;
  const money = isEnded
    ? format.money(campaign.budgetSpentEur ?? campaign.budgetEur, campaign.currency)
    : format.money(campaign.budgetEur, campaign.currency);

  return (
    <li
      className={`flex cursor-pointer items-center gap-4 rounded-card border bg-surface-raised p-4 shadow-raised transition duration-(--duration-fast) ease-standard hover:shadow-overlay ${
        isDraft ? 'border-dashed border-border-input' : 'border-border-subtle'
      }`}
      onClick={() => onEdit(campaign)}
    >
      <CampaignThumb status={isKnown ? status : 'draft'} />
      <div className="min-w-0 flex-1">
        <Badge tone={STATUS_TONES[status] ?? 'neutral'} className="mb-1">
          {t(`campaigns.status.${isKnown ? status : 'draft'}`)}
        </Badge>
        <h3 className={`truncate font-semibold ${isDraft ? 'italic' : ''}`}>
          <button type="button" className="truncate text-left" onClick={(event) => { event.stopPropagation(); onEdit(campaign); }}>
            {campaign.name}
          </button>
        </h3>
        <p className="truncate text-xs text-text-muted">{format.dateRange(campaign.startDate, campaign.endDate)}</p>
      </div>
      <div className="shrink-0 text-right">
        <p className="font-medium">{money}</p>
        <p className="text-xs text-text-muted">{isEnded ? t('campaigns.spent') : t('campaigns.budget')}</p>
      </div>
      <IconButton
        icon={Trash2}
        tone="danger"
        label={t('campaigns.deleteNamed', { name: campaign.name })}
        onClick={(event) => {
          event.stopPropagation();
          onDelete(campaign);
        }}
      />
    </li>
  );
}

/** Every campaign as a tile; a click opens the edit dialog. */
function CampaignList({ campaigns, onCreate, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <section className="flex flex-col gap-4">
      <h2 className="flex items-center gap-2 text-lg font-semibold">
        <List size={20} aria-hidden="true" className="text-primary" />
        {t('campaigns.all')}
      </h2>
      {campaigns.length === 0 ? (
        <div className="rounded-card border border-dashed border-border-input bg-surface-raised">
          <EmptyState
            icon={Megaphone}
            title={t('campaigns.emptyTitle')}
            message={t('campaigns.emptyMessage')}
            action={
              <Button icon={Plus} onClick={onCreate}>
                {t('campaigns.createFirst')}
              </Button>
            }
          />
        </div>
      ) : (
        <ul className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {campaigns.map((campaign, index) => (
            <CampaignTile
              key={campaign.campaignId || index}
              campaign={campaign}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

export default CampaignList;
