import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

export function normalizeStatus(status) {
  return (status || 'draft').toLowerCase();
}

export function emptyCampaignForm() {
  return { name: '', status: 'draft', budgetEur: '', budgetSpentEur: '', estimatedReach: '', currency: 'EUR', startDate: '', endDate: '' };
}

export const CAMPAIGN_STATUSES = ['draft', 'scheduled', 'active', 'ended'];

function currencySymbol(currency) {
  return currency === 'USD' || currency === '$' ? '$' : 'EUR';
}

/** Money and date formatters of the campaign screen, in the active language. */
export function useCampaignFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(() => {
    const none = t('campaigns.none');
    return {
      /** Compact amount of a campaign tile: "EUR1.5k", "EUR800". */
      money(amount, currency = 'EUR') {
        if (amount == null || amount === '') return none;
        const n = Number(amount);
        const symbol = currencySymbol(currency);
        if (n >= 1000) {
          const value = format.number(n / 1000, {
            minimumFractionDigits: n % 1000 === 0 ? 0 : 1,
            maximumFractionDigits: n % 1000 === 0 ? 0 : 1,
            useGrouping: false,
          });
          return t('campaigns.moneyThousands', { symbol, value });
        }
        return t('campaigns.money', { symbol, value: format.number(n) });
      },
      /** Full amount: "EUR12,500". */
      moneyFull(amount, currency = 'EUR') {
        if (amount == null) return none;
        return t('campaigns.money', { symbol: currencySymbol(currency), value: format.number(amount) });
      },
      /** Budget of the active campaign in thousands: "13k". */
      thousands(amount) {
        return t('campaigns.thousands', {
          value: format.number(amount / 1000, { maximumFractionDigits: 0, useGrouping: false }),
        });
      },
      number(value) {
        return format.number(value);
      },
      /** "Mar 5 - Apr 30", "TBD" without dates. */
      dateRange(start, end) {
        if (!start && !end) return t('campaigns.tbd');
        const day = (value) => {
          const date = new Date(value);
          if (Number.isNaN(date.getTime())) return value;
          return format.date(date, { month: 'short', day: 'numeric' });
        };
        return t('campaigns.dateRange', { start: day(start), end: day(end) });
      },
      none,
    };
  }, [t, format]);
}
