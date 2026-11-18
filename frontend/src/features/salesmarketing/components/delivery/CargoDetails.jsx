import { Download, Thermometer } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Button } from '../../../../shared/ui';
import { useDeliveryFormat } from './deliveryData';

/** Commodity, volume, temperature and manifest download of the selected shipment. */
function CargoDetails({ selected, onDownload }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDeliveryFormat();
  const weight =
    selected?.cargoWeightKg != null ? t('delivery.kg', { value: format.number(selected.cargoWeightKg) }) : format.none;
  return (
    <div className="flex w-full flex-col gap-4 rounded-tile bg-surface-muted p-4 lg:w-1/3">
      <h3 className="font-semibold">{t('delivery.cargo')}</h3>
      <dl className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <dt className="text-xs text-text-muted">{t('delivery.commodity')}</dt>
          <dd>{selected?.commodityDescription || format.none}</dd>
        </div>
        <div className="flex flex-col gap-1">
          <dt className="text-xs text-text-muted">{t('delivery.volumeWeight')}</dt>
          <dd>
            {selected?.containerCount != null
              ? t('delivery.containers', {
                  number: selected.containerCount,
                  type: selected.containerType || t('delivery.container'),
                  weight,
                })
              : format.none}
          </dd>
        </div>
        <div className="flex flex-col gap-1">
          <dt className="text-xs text-text-muted">{t('delivery.condition')}</dt>
          <dd className="mt-1 flex items-center justify-between rounded-control bg-surface-raised p-2">
            <span className="flex items-center gap-2">
              <Thermometer size={20} aria-hidden="true" className="text-primary" />
              <span>
                {selected?.currentTempC != null
                  ? t('delivery.celsius', { value: format.fixed(selected.currentTempC, 1) })
                  : format.none}
              </span>
            </span>
            {selected?.tempStatus ? <Badge tone="success">{selected.tempStatus}</Badge> : <span>{format.none}</span>}
          </dd>
        </div>
      </dl>
      {/* Narrow column: a short label; the full action is the accessible name and the tooltip. */}
      <Button
        className="mt-auto"
        fullWidth
        icon={Download}
        onClick={onDownload}
        aria-label={t('delivery.download')}
        title={t('delivery.download')}
      >
        {t('delivery.downloadShort')}
      </Button>
    </div>
  );
}

export default CargoDetails;
