import { Check } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useDeliveryFormat } from './deliveryData';

function Marker({ done, current }) {
  if (done) {
    return (
      <span className="absolute -left-8 top-0 flex h-6 w-6 items-center justify-center rounded-pill bg-primary text-on-primary">
        <Check size={16} aria-hidden="true" />
      </span>
    );
  }
  return (
    <span
      className={`absolute -left-8 top-0 flex h-6 w-6 items-center justify-center rounded-pill border-2 bg-surface-raised ${
        current ? 'border-primary' : 'border-border-input'
      }`}
    >
      {current && <span className="h-2 w-2 rounded-pill bg-primary" />}
    </span>
  );
}

/** Steps of the selected shipment, done, current or pending. */
function ShipmentTimeline({ events }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDeliveryFormat();
  return (
    <div className="flex-1">
      <h2 className="mb-4 text-lg font-semibold">{t('delivery.timeline')}</h2>
      <ol className="ml-3 space-y-6 border-l-2 border-border-divider pl-5">
        {events.map((event, idx) => {
          const { done, current } = event;
          const pending = event.pending || (!done && !current);
          return (
            <li key={event.eventId || idx} className={`relative ${pending ? 'text-text-muted' : ''}`}>
              <Marker done={done} current={current} />
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className={`font-semibold ${current ? 'text-text-accent' : ''}`}>{event.step}</p>
                  <p className="mt-1 text-sm text-text-muted">{event.note || format.none}</p>
                </div>
                <span className={`whitespace-pre-line text-right text-xs ${current ? 'text-text-accent' : 'text-text-muted'}`}>
                  {format.eventTime(event.eventTime, event.etaLabel)}
                </span>
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

export default ShipmentTimeline;
