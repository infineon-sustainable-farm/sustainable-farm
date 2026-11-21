import { CircleCheck, Truck, TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge } from '../../../../shared/ui';
import { normalizeStatus } from './deliveryData';

/** Status of a shipment: in transit, delayed, delivered, or the raw status. */
function ShipmentStatus({ status }) {
  const { t } = useTranslation('salesmarketing');
  const s = normalizeStatus(status);
  if (s.includes('TRANSIT') || s === 'IN_TRANSIT' || s === 'SHIPPED') {
    return <Badge tone="info" icon={Truck}>{t('delivery.status.inTransit')}</Badge>;
  }
  if (s.includes('DELAY')) {
    return <Badge tone="warning" icon={TriangleAlert}>{t('delivery.status.delayed')}</Badge>;
  }
  if (s.includes('DELIVER')) {
    return <Badge tone="success" icon={CircleCheck}>{t('delivery.status.delivered')}</Badge>;
  }
  return <Badge>{status || t('delivery.status.unknown')}</Badge>;
}

export default ShipmentStatus;
