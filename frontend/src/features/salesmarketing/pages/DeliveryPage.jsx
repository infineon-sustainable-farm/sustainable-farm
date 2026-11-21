import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Card, PageHeader } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import CargoDetails from '../components/delivery/CargoDetails';
import LocationPanel from '../components/delivery/LocationPanel';
import ShipmentList from '../components/delivery/ShipmentList';
import ShipmentTimeline from '../components/delivery/ShipmentTimeline';
import { buildTimeline, downloadManifest, getErrorMessage, normalizeStatus } from '../components/delivery/deliveryData';

export default function DeliveryPage() {
  const { t } = useTranslation('salesmarketing');
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [shipments, setShipments] = useState([]);
  const [events, setEvents] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedId, setSelectedId] = useState(null);

  const fetchDeliveryData = useCallback(async () => {
    try {
      const [shipRes, eventRes] = await Promise.allSettled([api.getShipments(), api.getShipmentEvents()]);
      setApiError(null);

      const failures = [];

      if (shipRes.status === 'fulfilled') {
        const nextShipments = Array.isArray(shipRes.value) ? shipRes.value : [];
        setShipments(nextShipments);
        setSelectedId(nextShipments.length > 0 ? nextShipments[0].shipmentId : null);
      } else {
        setShipments([]);
        setSelectedId(null);
        failures.push(t('delivery.shipmentsFailed', { message: getErrorMessage(shipRes.reason, t('delivery.shipmentsFallback')) }));
      }

      if (eventRes.status === 'fulfilled') {
        setEvents(Array.isArray(eventRes.value) ? eventRes.value : []);
      } else {
        setEvents([]);
        failures.push(t('delivery.eventsFailed', { message: getErrorMessage(eventRes.reason, t('delivery.eventsFallback')) }));
      }

      if (failures.length > 0) {
        setApiError(failures.join(' · '));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('delivery.loadFailed')));
      setShipments([]);
      setEvents([]);
      setSelectedId(null);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchDeliveryData);
  }, [fetchDeliveryData]);

  const q = searchQuery.trim().toLowerCase();
  const filtered = shipments.filter((s) => {
    const matchesQuery = !q
      ? true
      : [s.shipmentCode, s.origin, s.destination, s.status, s.vesselName].filter(Boolean).join(' ').toLowerCase().includes(q);
    const matchesStatus = statusFilter === 'ALL' ? true : normalizeStatus(s.status) === statusFilter;
    return matchesQuery && matchesStatus;
  });

  const selected = filtered.find((s) => s.shipmentId === selectedId) || filtered[0] || shipments[0] || null;
  const timeline = buildTimeline(events, selected);

  const resetFilters = () => {
    setSearchQuery('');
    setStatusFilter('ALL');
    if (shipments.length > 0) setSelectedId(shipments[0].shipmentId);
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t('delivery.title')} subtitle={t('delivery.subtitle')} />

      <div className="flex flex-col gap-6 xl:flex-row">
        <div className="flex w-full shrink-0 flex-col gap-4 xl:w-96">
          {apiError && (
            <Notice tone="offline" title={t('delivery.apiNotice')} onRetry={fetchDeliveryData}>
              {apiError}
            </Notice>
          )}
          <ShipmentList
            loading={loading}
            shipments={filtered}
            selectedId={selected?.shipmentId}
            total={shipments.length}
            searchQuery={searchQuery}
            onSearch={setSearchQuery}
            statusFilter={statusFilter}
            onStatusFilter={setStatusFilter}
            onSelect={setSelectedId}
            onReset={resetFilters}
          />
        </div>

        <div className="flex flex-1 flex-col gap-6">
          <LocationPanel selected={selected} />
          <Card className="flex flex-1 flex-col gap-8 lg:flex-row">
            <ShipmentTimeline events={timeline} />
            <CargoDetails selected={selected} onDownload={() => downloadManifest(selected)} />
          </Card>
        </div>
      </div>
    </div>
  );
}
