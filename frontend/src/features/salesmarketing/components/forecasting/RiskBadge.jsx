import { useTranslation } from 'react-i18next';
import { StatusBadge } from '../../../../shared/ui';

/** Capacity risk of a forecast row; an unknown value is shown as it is. */
function RiskBadge({ risk }) {
  const { t } = useTranslation('salesmarketing');
  const value = risk?.toLowerCase() || '';
  if (value.includes('low')) return <StatusBadge tone="success" label={t('forecasting.risk.low')} />;
  if (value.includes('medium')) return <StatusBadge tone="warning" label={t('forecasting.risk.medium')} />;
  if (value.includes('high')) return <StatusBadge tone="error" label={t('forecasting.risk.high')} />;
  return <StatusBadge tone="neutral" label={risk || t('forecasting.none')} />;
}

export default RiskBadge;
