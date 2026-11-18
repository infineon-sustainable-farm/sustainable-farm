import { CalendarDays } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';

/** Static band of the peak harvest months (March to July) on a 12 month bar. */
function HarvestWindow() {
  const { t } = useTranslation('salesmarketing');
  return (
    <Card className="flex items-center gap-4">
      <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-pill bg-success-bg text-success-fg">
        <CalendarDays size={20} aria-hidden="true" />
      </span>
      <div className="flex-1">
        <div className="mb-1 flex items-end justify-between">
          <h2 className="text-lg font-semibold">{t('dashboard.harvest.title')}</h2>
          <span className="text-sm text-text-muted">{t('dashboard.harvest.months')}</span>
        </div>
        <div className="flex h-3 w-full overflow-hidden rounded-pill bg-neutral-bg">
          <div className="h-full w-2/12" />
          <div className="h-full w-5/12 bg-success-fg" />
        </div>
      </div>
    </Card>
  );
}

export default HarvestWindow;
