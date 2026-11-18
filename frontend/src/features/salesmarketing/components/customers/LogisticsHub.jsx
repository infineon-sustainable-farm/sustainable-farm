import { MapPin, Truck } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';

/** Location of the selected customer, on a plain surface with a pin. */
function LogisticsHub({ location }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <Card
      title={t('customers.logistics.title')}
      actions={<Truck size={20} aria-hidden="true" className="text-text-muted" />}
      className="flex h-70 flex-col"
    >
      <div className="relative flex flex-1 items-center justify-center rounded-tile bg-surface-muted" data-location={location}>
        <span className="flex h-12 w-12 items-center justify-center rounded-pill bg-primary-tint text-primary">
          <MapPin size={20} aria-hidden="true" />
        </span>
        {location && (
          <p className="absolute bottom-3 left-3 rounded-control bg-surface-raised px-3 py-2 text-sm font-medium shadow-overlay">
            {location}
          </p>
        )}
      </div>
    </Card>
  );
}

export default LogisticsHub;
