import { ArrowRight, ChevronRight, Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, Field, Input, Select, Skeleton } from '../../../../shared/ui';
import ShipmentStatus from './ShipmentStatus';
import { normalizeStatus, STATUS_FILTERS, useDeliveryFormat } from './deliveryData';

function ShipmentItem({ shipment, active, onSelect }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDeliveryFormat();
  const status = normalizeStatus(shipment.status);
  const isTransit = status.includes('TRANSIT') || status === 'SHIPPED';
  return (
    <li>
      <button
        type="button"
        aria-pressed={active}
        onClick={() => onSelect(shipment.shipmentId)}
        className={`flex w-full flex-col gap-2 rounded-card border-l-4 bg-surface-raised p-4 text-left shadow-raised transition duration-(--duration-fast) ease-standard hover:shadow-overlay ${
          active ? 'border-primary' : 'border-transparent'
        }`}
      >
        <span className="flex items-start justify-between gap-2">
          <span className="text-xs text-text-muted">{shipment.shipmentCode || `SHP-${shipment.shipmentId}`}</span>
          <ShipmentStatus status={shipment.status} />
        </span>
        <span>
          <span className="flex items-center gap-1 font-semibold">
            {shipment.origin}
            <ArrowRight size={16} aria-label={t('delivery.to')} className="text-text-muted" />
            {shipment.destination}
          </span>
          <span className="mt-1 block text-sm text-text-muted">{format.vesselLine(shipment)}</span>
        </span>
        {!status.includes('DELIVER') && (
          <span className="mt-2 flex items-center justify-between">
            <span className="flex flex-col">
              <span className="text-xs text-text-muted">
                {status.includes('DELAY') ? t('delivery.updatedEta') : t('delivery.eta')}
              </span>
              <span className="font-semibold">{format.eta(shipment)}</span>
            </span>
            {isTransit && <ChevronRight size={18} aria-hidden="true" className="text-text-muted" />}
          </span>
        )}
      </button>
    </li>
  );
}

/** Search, status filter and the list of shipments; the selected one is highlighted. */
function ShipmentList({ loading, shipments, selectedId, total, searchQuery, onSearch, statusFilter, onStatusFilter, onSelect, onReset }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <div className="flex flex-col gap-4">
      <Card className="flex items-end gap-3">
        <Field label={t('delivery.search')} className="flex-1">
          <div className="relative">
            <Search size={16} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
            <Input
              type="text"
              className="pl-9"
              placeholder={t('delivery.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => onSearch(e.target.value)}
            />
          </div>
        </Field>
        <Field label={t('delivery.filter')}>
          <Select value={statusFilter} onChange={(e) => onStatusFilter(e.target.value)}>
            {STATUS_FILTERS.map((option) => (
              <option key={option} value={option}>
                {t(`delivery.filters.${option}`)}
              </option>
            ))}
          </Select>
        </Field>
      </Card>

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">{t('delivery.loading')}</p>
          <Skeleton radius="card" className="h-32" />
          <Skeleton radius="card" className="h-32" />
        </div>
      ) : shipments.length === 0 ? (
        <Card>
          <p className="text-center text-sm text-text-muted">{t('delivery.noMatch')}</p>
        </Card>
      ) : (
        <ul className="flex flex-col gap-4">
          {shipments.map((shipment) => (
            <ShipmentItem
              key={shipment.shipmentId}
              shipment={shipment}
              active={shipment.shipmentId === selectedId}
              onSelect={onSelect}
            />
          ))}
        </ul>
      )}

      <button
        type="button"
        onClick={onReset}
        className="rounded-control py-2 text-center text-sm font-medium text-text-accent hover:underline"
      >
        {t('delivery.viewAll', { number: total })}
      </button>
    </div>
  );
}

export default ShipmentList;
