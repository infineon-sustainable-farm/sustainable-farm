import { Ellipsis } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Card, IconButton } from '../../../../shared/ui';
import { useCampaignFormat } from './campaignFormat';

/** Budget and reach of the active campaign (or the first one), with the spent share as a bar. */
function ActiveCampaignCard({ campaign, onEdit, className }) {
  const { t } = useTranslation('salesmarketing');
  const format = useCampaignFormat();
  const spent = Number(campaign?.budgetSpentEur ?? 0);
  const budget = Number(campaign?.budgetEur ?? 0);
  const progress = budget > 0 ? Math.min(100, Math.round((spent / budget) * 100)) : 0;

  return (
    <Card className={`flex flex-col ${className ?? ''}`}>
      <div className="mb-4 flex items-start justify-between">
        <Badge tone="success">{t('campaigns.activeNow')}</Badge>
        <IconButton
          icon={Ellipsis}
          tone="neutral"
          label={t('campaigns.editActive')}
          onClick={() => campaign && onEdit(campaign)}
        />
      </div>
      <h2 className="text-lg font-semibold">{campaign?.name || t('campaigns.noActive')}</h2>
      <p className="mb-6 text-sm text-text-muted">
        {campaign ? t('campaigns.activeSubtitle') : t('campaigns.activeEmpty')}
      </p>
      <dl className="mt-auto grid grid-cols-2 gap-4">
        <div>
          <dt className="text-xs text-text-muted">{t('campaigns.budgetSpent')}</dt>
          <dd className="text-lg font-semibold">
            {campaign ? format.moneyFull(spent, campaign.currency) : format.none}{' '}
            <span className="text-sm font-normal text-text-muted">
              {t('campaigns.ofBudget', { budget: campaign && budget ? format.thousands(budget) : format.none })}
            </span>
          </dd>
        </div>
        <div>
          <dt className="text-xs text-text-muted">{t('campaigns.estReach')}</dt>
          <dd className="text-lg font-semibold">
            {campaign?.estimatedReach != null ? format.number(campaign.estimatedReach) : format.none}
          </dd>
        </div>
      </dl>
      <div
        className="mt-4 h-1 w-full rounded-pill bg-neutral-bg"
        role="progressbar"
        aria-label={t('campaigns.budgetSpent')}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={progress}
      >
        <div className="h-full rounded-pill bg-primary" style={{ width: `${progress}%` }} />
      </div>
    </Card>
  );
}

export default ActiveCampaignCard;
