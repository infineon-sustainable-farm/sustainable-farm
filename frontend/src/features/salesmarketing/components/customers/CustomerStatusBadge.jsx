import { StatusBadge } from '../../../../shared/ui';
import { normalizeStatus, useCustomerLabels } from './customerUtils';

const TONES = { active: 'success', inactive: 'error', prospect: 'warning' };

/** Active (green), inactive (red) or prospect (amber); an unknown status reads as prospect. */
function CustomerStatusBadge({ status }) {
  const labels = useCustomerLabels();
  const s = normalizeStatus(status);
  const known = Object.hasOwn(TONES, s) ? s : 'prospect';
  return <StatusBadge tone={TONES[known]} label={labels.status(known)} />;
}

export default CustomerStatusBadge;
