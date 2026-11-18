import { Tag, TrendingDown, TrendingUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';
import { usePricingFormat } from './pricingModel';

/** Latest price per kg of the current product and its change against the previous price. */
function CurrentPriceCard({ loading, currentPrice, delta, deltaPct }) {
  const { t } = useTranslation('salesmarketing');
  const format = usePricingFormat();
  const TrendIcon = delta == null || delta >= 0 ? TrendingUp : TrendingDown;

  return (
    <Card>
      <p className="mb-2 flex items-center gap-1 text-sm font-semibold text-text-accent">
        <Tag size={20} aria-hidden="true" />
        {t('pricing.engine')}
      </p>
      <p className="mt-4 text-sm text-text-muted">{t('pricing.currentPrice')}</p>
      <p className="flex items-baseline gap-2">
        <span className="text-3xl font-bold text-text-accent">
          {loading ? t('pricing.loadingValue') : format.price(currentPrice)}
        </span>
        <span className="text-text-muted">{t('pricing.perKg')}</span>
      </p>
      <p className="mt-4 flex w-fit flex-wrap items-center gap-2 rounded-pill bg-surface-muted px-4 py-2 text-sm">
        <TrendIcon size={18} aria-hidden="true" className="text-warning-fg" />
        <span className="font-medium text-warning-fg">
          {deltaPct == null ? t('pricing.awaitingHistory') : format.delta(delta, deltaPct)}
        </span>
        <span className="text-xs text-text-muted">{t('pricing.vsPreviousMonth')}</span>
      </p>
    </Card>
  );
}

export default CurrentPriceCard;
