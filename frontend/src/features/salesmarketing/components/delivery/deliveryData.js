import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSalesFormat } from '../../format';

export const STATUS_FILTERS = ['ALL', 'IN_TRANSIT', 'DELAYED', 'DELIVERED'];

export function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  return data.message || fallback;
}

export function normalizeStatus(status) {
  return (status || '').toUpperCase().replace(/\s+/g, '_');
}

/** Events of the selected shipment in time order, each marked done, current or pending. */
export function buildTimeline(events, selected) {
  if (!selected) return [];
  const status = normalizeStatus(selected.status);
  return events
    .filter((e) => e.shipmentId === selected.shipmentId)
    .sort((a, b) => new Date(a.eventTime || 0) - new Date(b.eventTime || 0))
    .map((e, idx, arr) => {
      if (e.done || e.current || e.pending) return e;
      const isLast = idx === arr.length - 1;
      if (status.includes('DELIVER')) return { ...e, done: true };
      if (isLast && (status.includes('TRANSIT') || status.includes('DELAY'))) return { ...e, current: true };
      if (!isLast) return { ...e, done: true };
      return { ...e, pending: true };
    });
}

/** Downloads the manifest of a shipment: its manifestUrl, or a JSON file of its fields. */
export function downloadManifest(selected) {
  if (!selected) return;

  const shipmentData = {
    shipmentCode: selected.shipmentCode || `SHP-${selected.shipmentId}`,
    origin: selected.origin,
    destination: selected.destination,
    status: selected.status,
    eta: selected.eta,
    vesselName: selected.vesselName,
    voyageNumber: selected.voyageNumber,
    currentLocationLabel: selected.currentLocationLabel,
    currentLat: selected.currentLat,
    currentLon: selected.currentLon,
    commodityDescription: selected.commodityDescription,
    containerCount: selected.containerCount,
    containerType: selected.containerType,
    cargoWeightKg: selected.cargoWeightKg,
    currentTempC: selected.currentTempC,
    tempStatus: selected.tempStatus,
  };

  if (selected.manifestUrl) {
    window.open(selected.manifestUrl, '_blank', 'noopener,noreferrer');
    return;
  }

  const blob = new Blob([JSON.stringify(shipmentData, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${shipmentData.shipmentCode || 'shipment'}-manifest.json`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/** Dates, durations and texts of the delivery screen, in the active language. */
export function useDeliveryFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(() => {
    const none = t('delivery.none');
    const shortDay = (d) => format.date(d, { month: 'short', day: 'numeric' });
    return {
      none,
      number: (value) => format.number(value),
      fixed: (value, digits) =>
        format.number(value, { minimumFractionDigits: digits, maximumFractionDigits: digits, useGrouping: false }),
      eta(shipment) {
        if (shipment.etaLabel) return shipment.etaLabel;
        if (!shipment.eta) return t('delivery.tbd');
        const d = new Date(shipment.eta);
        if (Number.isNaN(d.getTime())) return shipment.eta;
        return shortDay(d);
      },
      eventTime(eventTime, etaLabel) {
        if (etaLabel) return etaLabel;
        if (!eventTime) return none;
        const d = new Date(eventTime);
        if (Number.isNaN(d.getTime())) return eventTime;
        const time = format.date(d, { hour: '2-digit', minute: '2-digit', hour12: false });
        return `${shortDay(d)}\n${time}`;
      },
      daysUntil(eta) {
        if (!eta) return none;
        const d = new Date(eta);
        if (Number.isNaN(d.getTime())) return none;
        const diff = Math.ceil((d.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
        if (diff <= 0) return t('delivery.arrived');
        return t(diff === 1 ? 'delivery.day' : 'delivery.days', { number: diff });
      },
      vesselLine(shipment) {
        const status = normalizeStatus(shipment.status);
        if ((status.includes('DELAY') || status.includes('DELIVER')) && shipment.delayReason) {
          return shipment.delayReason;
        }
        if (shipment.vesselName) {
          return shipment.voyageNumber
            ? t('delivery.vesselVoyage', { vessel: shipment.vesselName, voyage: shipment.voyageNumber })
            : t('delivery.vessel', { vessel: shipment.vesselName });
        }
        return t('delivery.details');
      },
    };
  }, [t, format]);
}
