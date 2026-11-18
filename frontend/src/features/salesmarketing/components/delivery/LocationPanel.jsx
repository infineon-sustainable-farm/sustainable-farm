import { Ship } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';
import { useDeliveryFormat } from './deliveryData';

/** Current position, speed, distance remaining and days to arrival of the selected shipment. */
function LocationPanel({ selected }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDeliveryFormat();
  return (
    <Card className="flex flex-col items-center justify-between gap-6 md:flex-row">
      <div className="flex items-center gap-4">
        <span className="flex h-12 w-12 items-center justify-center rounded-pill bg-primary-tint text-primary">
          <Ship size={20} aria-hidden="true" />
        </span>
        <div>
          <p className="text-xs text-text-muted">{t('delivery.currentLocation')}</p>
          <p className="text-lg font-semibold">
            {selected?.currentLocationLabel ||
              t('delivery.route', { origin: selected?.origin || format.none, destination: selected?.destination || format.none })}
          </p>
          <p className="text-sm text-text-muted">
            {t('delivery.position', {
              lat: selected?.currentLat ?? format.none,
              lon: selected?.currentLon ?? format.none,
              speed: selected?.speedKnots != null ? t('delivery.knots', { value: selected.speedKnots }) : format.none,
            })}
          </p>
        </div>
      </div>
      <dl className="flex gap-6">
        <div className="text-right">
          <dt className="text-xs text-text-muted">{t('delivery.distanceRemaining')}</dt>
          <dd className="text-2xl font-bold text-text-accent">
            {selected?.distanceRemainingNm != null
              ? t('delivery.nauticalMiles', { value: format.number(selected.distanceRemainingNm) })
              : format.none}
          </dd>
        </div>
        <div className="text-right">
          <dt className="text-xs text-text-muted">{t('delivery.estArrival')}</dt>
          <dd className="text-2xl font-bold">{format.daysUntil(selected?.eta)}</dd>
        </div>
      </dl>
    </Card>
  );
}

export default LocationPanel;
