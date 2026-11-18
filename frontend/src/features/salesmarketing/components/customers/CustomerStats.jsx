import { ChartLine, Info, UserPlus, UserX, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { KpiCard } from '../../../../shared/ui';
import { useSalesFormat } from '../../format';

function Trend({ icon: Icon, tone = 'text-text-accent', children }) {
  return (
    <span className={`flex items-center gap-1 ${tone}`}>
      <Icon size={16} aria-hidden="true" />
      {children}
    </span>
  );
}

/** Active, prospect and inactive counts, and a one-line summary of the CRM data. */
function CustomerStats({ activeCount, prospectCount, inactiveCount, totalCount }) {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
      <KpiCard
        icon={Users}
        label={t('customers.stats.totalActive')}
        value={format.number(activeCount)}
      />
      <KpiCard
        icon={UserPlus}
        tone="warning"
        label={t('customers.stats.newOnboarded')}
        value={format.number(prospectCount)}
      />
      <KpiCard
        icon={UserX}
        tone="error"
        label={t('customers.stats.inactive')}
        value={format.number(inactiveCount)}
        hint={
          <Trend icon={Info} tone="text-error-fg">
            {t('customers.stats.followUp')}
          </Trend>
        }
      />
      <section className="flex flex-col gap-4 rounded-card bg-primary p-5 text-on-primary shadow-raised">
        <h2 className="flex items-center gap-2 text-sm font-semibold">
          <ChartLine size={20} aria-hidden="true" />
          {t('customers.stats.insight')}
        </h2>
        <p>
          {totalCount === 0
            ? t('customers.stats.insightEmpty')
            : t('customers.stats.insightSummary', {
                active: t('customers.stats.activeCustomers', { count: activeCount }),
                prospects: t('customers.stats.prospects', { count: prospectCount }),
              })}
        </p>
      </section>
    </div>
  );
}

export default CustomerStats;
