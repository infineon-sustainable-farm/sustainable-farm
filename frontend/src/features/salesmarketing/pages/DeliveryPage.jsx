import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

const MAP_IMAGE =
  "url('https://lh3.googleusercontent.com/aida-public/AB6AXuBIm-zErSziwUiSSJzhvrKgcrj-IuZDfCZDbfSqbQlcrHwsnfJ7L0Z0wkyOcxg6ImSv_Y8emrvLdoTY8GlsnffE1E9sGLd0GAPYlbtFbPAqkyOJ0uneLb0yk5bSveVBNCYxQmkp18mnvtX48wrd5g8urh2nDoDops0PXBsMRNgeTzdMkesfMfOOXeNFy1LE7ayoz9DPfmYE1u6p9p2Xq0cNqxz_PlbtH4nTKslKW4FMdSDLAPNcOZMw')";

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  return data.message || fallback;
}

function normalizeStatus(status) {
  return (status || '').toUpperCase().replace(/\s+/g, '_');
}

function StatusPill({ status }) {
  const s = normalizeStatus(status);
  if (s.includes('TRANSIT') || s === 'IN_TRANSIT' || s === 'SHIPPED') {
    return (
      <div className="bg-primary/10 rounded-full px-sm py-1 flex items-center gap-xs">
        <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
        <span className="font-label-md text-label-md text-primary">IN TRANSIT</span>
      </div>
    );
  }
  if (s.includes('DELAY')) {
    return (
      <div className="bg-secondary-container/10 rounded-full px-sm py-1 flex items-center gap-xs">
        <span className="material-symbols-outlined text-[12px] text-secondary-container">warning</span>
        <span className="font-label-md text-label-md text-secondary-container">DELAYED</span>
      </div>
    );
  }
  if (s.includes('DELIVER')) {
    return (
      <div className="bg-[#1b5e20]/10 rounded-full px-sm py-1 flex items-center gap-xs">
        <span className="material-symbols-outlined text-[12px] text-[#1b5e20]">check_circle</span>
        <span className="font-label-md text-label-md text-[#1b5e20]">DELIVERED</span>
      </div>
    );
  }
  return (
    <div className="bg-surface-container-high rounded-full px-sm py-1 flex items-center gap-xs">
      <span className="font-label-md text-label-md text-on-surface-variant">{status || 'UNKNOWN'}</span>
    </div>
  );
}

function formatEta(shipment) {
  if (shipment.etaLabel) return shipment.etaLabel;
  if (!shipment.eta) return 'TBD';
  const d = new Date(shipment.eta);
  if (isNaN(d.getTime())) return shipment.eta;
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function formatEventTime(eventTime, etaLabel) {
  if (etaLabel) return etaLabel;
  if (!eventTime) return '—';
  const d = new Date(eventTime);
  if (isNaN(d.getTime())) return eventTime;
  const date = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  const time = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
  return `${date}\n${time}`;
}

function daysUntil(eta) {
  if (!eta) return '—';
  const d = new Date(eta);
  if (isNaN(d.getTime())) return '—';
  const diff = Math.ceil((d.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  if (diff <= 0) return 'Arrived';
  return `${diff} Day${diff === 1 ? '' : 's'}`;
}

function vesselLine(shipment) {
  if (normalizeStatus(shipment.status).includes('DELAY') && shipment.delayReason) {
    return shipment.delayReason;
  }
  if (normalizeStatus(shipment.status).includes('DELIVER') && shipment.delayReason) {
    return shipment.delayReason;
  }
  if (shipment.vesselName) {
    return `Vessel: ${shipment.vesselName}${shipment.voyageNumber ? ` (${shipment.voyageNumber})` : ''}`;
  }
  return 'Shipment details';
}

export default function DeliveryPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [shipments, setShipments] = useState([]);
  const [events, setEvents] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showFilterMenu, setShowFilterMenu] = useState(false);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    fetchDeliveryData();
  }, []);

  const fetchDeliveryData = async () => {
    setLoading(true);
    setApiError(null);

    try {
      const [shipRes, eventRes] = await Promise.allSettled([
        api.getShipments(),
        api.getShipmentEvents(),
      ]);

      const failures = [];

      if (shipRes.status === 'fulfilled') {
        const nextShipments = Array.isArray(shipRes.value.data) ? shipRes.value.data : [];
        setShipments(nextShipments);
        setSelectedId(nextShipments.length > 0 ? nextShipments[0].shipmentId : null);
      } else {
        setShipments([]);
        setSelectedId(null);
        failures.push(`Shipments: ${getErrorMessage(shipRes.reason, 'Unable to load shipment data.')}`);
      }

      if (eventRes.status === 'fulfilled') {
        setEvents(Array.isArray(eventRes.value.data) ? eventRes.value.data : []);
      } else {
        setEvents([]);
        failures.push(`Shipment events: ${getErrorMessage(eventRes.reason, 'Unable to load shipment events.')}`);
      }

      if (failures.length > 0) {
        setApiError(failures.join(' · '));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to Spring Boot backend API at http://localhost:8080/api'));
      setShipments([]);
      setEvents([]);
      setSelectedId(null);
    } finally {
      setLoading(false);
    }
  };

  const sourceShipments = shipments;
  const sourceEvents = events;

  const q = searchQuery.trim().toLowerCase();
  const filtered = sourceShipments.filter((s) => {
    const matchesQuery = !q
      ? true
      : [s.shipmentCode, s.origin, s.destination, s.status, s.vesselName]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
          .includes(q);

    const matchesStatus = statusFilter === 'ALL' ? true : normalizeStatus(s.status) === statusFilter;
    return matchesQuery && matchesStatus;
  });

  useEffect(() => {
    if (filtered.length === 0) return;
    if (!filtered.some((s) => s.shipmentId === selectedId)) {
      setSelectedId(filtered[0].shipmentId);
    }
  }, [filtered, selectedId]);

  const selected = filtered.find((s) => s.shipmentId === selectedId) || sourceShipments[0] || null;

  const handleDownloadManifest = () => {
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

    const blob = new Blob([JSON.stringify(shipmentData, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${shipmentData.shipmentCode || 'shipment'}-manifest.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const timelineEvents = selected
    ? sourceEvents
        .filter((e) => e.shipmentId === selected.shipmentId)
        .sort((a, b) => new Date(a.eventTime || 0) - new Date(b.eventTime || 0))
    : [];

  const enrichedTimeline =
    timelineEvents.length > 0
      ? timelineEvents.map((e, idx, arr) => {
          if (e.done || e.current || e.pending) return e;
          const isLast = idx === arr.length - 1;
          const status = normalizeStatus(selected?.status);
          if (status.includes('DELIVER')) return { ...e, done: true };
          if (isLast && (status.includes('TRANSIT') || status.includes('DELAY'))) return { ...e, current: true };
          if (!isLast) return { ...e, done: true };
          return { ...e, pending: true };
        })
      : [];

  return (
    <div className="flex flex-col w-full h-full relative">
      <div className="px-margin-desktop py-xl">
        <h2 className="font-headline-lg text-headline-lg text-primary mb-xs">Delivery Tracking</h2>
        <p className="font-body-md text-body-md text-on-surface-variant max-w-2xl">
          Monitor real-time global shipment statuses, predict delays, and optimize route efficiency across all supply chain channels.
        </p>
      </div>

      <div className="px-margin-desktop pb-xxl flex-1 flex flex-col xl:flex-row gap-lg h-full">
        {/* Sidebar list */}
        <div className="w-full xl:w-[400px] flex-shrink-0 flex flex-col gap-md">
          {apiError && (
            <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl shadow-sm">
              <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant mb-sm">{apiError}</p>
              <button
                onClick={fetchDeliveryData}
                className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
              >
                <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
              </button>
            </div>
          )}

          <div className="flex items-center justify-between bg-surface-container rounded-xl p-md shadow-sm relative">
            <div className="relative flex-1 mr-md">
              <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-on-surface-variant">
                search
              </span>
              <input
                className="w-full bg-surface-container-lowest font-body-sm text-body-sm text-on-surface rounded-lg pl-xl pr-md py-sm focus:outline-none focus:ring-1 focus:ring-primary shadow-sm transition-shadow"
                placeholder="Search by ID or destination..."
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowFilterMenu((prev) => !prev)}
                className="w-10 h-10 rounded-lg bg-surface-container-lowest flex items-center justify-center text-on-surface hover:text-primary transition-colors shadow-sm"
              >
                <span className="material-symbols-outlined">filter_list</span>
              </button>
              {showFilterMenu && (
                <div className="absolute right-0 top-[calc(100%+8px)] bg-surface-container-lowest rounded-lg shadow-lg border border-outline-variant/30 min-w-[180px] z-20">
                  {['ALL', 'IN_TRANSIT', 'DELAYED', 'DELIVERED'].map((option) => (
                    <button
                      key={option}
                      type="button"
                      onClick={() => {
                        setStatusFilter(option);
                        setShowFilterMenu(false);
                      }}
                      className={`w-full text-left px-md py-sm font-label-md text-label-md hover:bg-surface-container ${statusFilter === option ? 'text-primary bg-primary/5' : 'text-on-surface'}`}
                    >
                      {option === 'ALL' ? 'All shipments' : option.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {loading ? (
            <div className="p-xl bg-surface-container-lowest rounded-xl text-center shadow-sm">
              <span className="material-symbols-outlined text-[32px] text-primary animate-spin">sync</span>
                <p className="font-body-sm text-body-sm text-on-surface-variant mt-sm">Loading shipments…</p>
            </div>
          ) : filtered.length === 0 ? (
            <div className="p-xl bg-surface-container-lowest rounded-xl text-center shadow-sm">
              <p className="font-body-sm text-body-sm text-on-surface-variant">No shipments match your search.</p>
            </div>
          ) : (
            filtered.map((shipment) => {
              const active = shipment.shipmentId === selected?.shipmentId;
              const status = normalizeStatus(shipment.status);
              const isTransit = status.includes('TRANSIT') || status === 'SHIPPED';
              return (
                <button
                  key={shipment.shipmentId}
                  type="button"
                  onClick={() => setSelectedId(shipment.shipmentId)}
                  className={`w-full text-left rounded-xl p-md relative overflow-hidden group transition-all duration-300 ${
                    active
                      ? 'bg-surface-container-lowest shadow-md border-l-4 border-primary hover:-translate-y-1'
                      : 'bg-surface-container shadow-sm border-l-4 border-transparent hover:border-outline-variant'
                  }`}
                >
                  {active && (
                    <div className="absolute inset-0 bg-gradient-to-r from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                  )}
                  <div className="relative z-10 flex flex-col gap-sm">
                    <div className="flex justify-between items-start">
                      <span className="font-mono-label text-mono-label text-on-surface-variant">
                        {shipment.shipmentCode || `SHP-${shipment.shipmentId}`}
                      </span>
                      <StatusPill status={shipment.status} />
                    </div>
                    <div>
                      <h3 className="font-headline-sm text-headline-sm text-on-surface">
                        {shipment.origin}{' '}
                        <span className="material-symbols-outlined text-[16px] text-outline mx-xs align-middle">
                          arrow_forward
                        </span>{' '}
                        {shipment.destination}
                      </h3>
                      <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                        {vesselLine(shipment)}
                      </p>
                    </div>
                    {!status.includes('DELIVER') && (
                      <div className="pt-sm mt-sm border-t border-outline-variant/30 flex justify-between items-center">
                        <div className="flex flex-col">
                          <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                            {status.includes('DELAY') ? 'Updated ETA' : 'ETA'}
                          </span>
                          <span className="font-body-md text-body-md text-on-surface font-semibold">
                            {formatEta(shipment)}
                          </span>
                        </div>
                        {isTransit && (
                          <span className="material-symbols-outlined text-outline group-hover:text-primary transition-colors">
                            chevron_right
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                </button>
              );
            })
          )}

          <button
            type="button"
            onClick={() => {
              setSearchQuery('');
              setStatusFilter('ALL');
              setShowFilterMenu(false);
              if (sourceShipments.length > 0) setSelectedId(sourceShipments[0].shipmentId);
            }}
            className="font-label-md text-label-md text-primary uppercase text-center mt-sm hover:underline"
          >
            View All {sourceShipments.length} Shipments
          </button>
        </div>

        {/* Map + detail */}
        <div className="flex-1 flex flex-col gap-lg min-h-[600px]">
          <div className="w-full h-[50%] lg:h-[60%] rounded-xl shadow-md overflow-hidden relative bg-surface-container-high group">
            <div
              className="absolute inset-0 bg-cover bg-center transition-transform duration-[10s] group-hover:scale-105"
              style={{ backgroundImage: MAP_IMAGE }}
            />
            <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/90 via-surface-container-lowest/20 to-transparent pointer-events-none" />
            <div className="absolute bottom-md left-md right-md bg-surface-container-lowest/95 backdrop-blur-md rounded-xl p-md shadow-lg flex flex-col md:flex-row gap-lg justify-between items-center">
              <div className="flex items-center gap-md">
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                  <span className="material-symbols-outlined text-primary text-[24px]">directions_boat</span>
                </div>
                <div>
                  <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                    Current Location
                  </p>
                  <p className="font-headline-sm text-headline-sm text-on-surface">
                    {selected?.currentLocationLabel || `${selected?.origin || '—'} → ${selected?.destination || '—'}`}
                  </p>
                  <p className="font-body-sm text-body-sm text-outline">
                    Lat {selected?.currentLat ?? '—'}, Lon {selected?.currentLon ?? '—'} · Speed:{' '}
                    {selected?.speedKnots != null ? `${selected.speedKnots} kts` : '—'}
                  </p>
                </div>
              </div>
              <div className="flex gap-md">
                <div className="text-right">
                  <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                    Distance Remaining
                  </p>
                  <p className="font-headline-md text-headline-md text-primary">
                    {selected?.distanceRemainingNm != null
                      ? `${Number(selected.distanceRemainingNm).toLocaleString()} NM`
                      : '—'}
                  </p>
                </div>
                <div className="w-px h-10 bg-outline-variant/50 self-center" />
                <div className="text-right">
                  <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                    Est. Arrival
                  </p>
                  <p className="font-headline-md text-headline-md text-on-surface">
                    {daysUntil(selected?.eta)}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-lg flex flex-col lg:flex-row gap-xl flex-1">
            <div className="flex-1">
              <h3 className="font-headline-sm text-headline-sm text-on-surface mb-lg">Shipment Timeline</h3>
              <div className="relative pl-6 space-y-6">
                <div className="absolute left-[11px] top-2 bottom-6 w-0.5 bg-outline-variant/40" />
                {enrichedTimeline.map((event, idx) => {
                  const done = event.done;
                  const current = event.current;
                  const pending = event.pending || (!done && !current);
                  return (
                    <div key={event.eventId || idx} className={`relative ${pending ? 'opacity-50' : ''}`}>
                      <div
                        className={`absolute -left-[30px] top-0 w-6 h-6 rounded-full flex items-center justify-center shadow-[0_0_0_4px_rgba(255,255,255,1)] ${
                          done
                            ? 'bg-primary'
                            : current
                              ? 'bg-surface-container-lowest border-2 border-primary'
                              : 'bg-surface-container-lowest border-2 border-outline-variant'
                        }`}
                      >
                        {done && (
                          <span className="material-symbols-outlined text-on-primary text-[14px]">check</span>
                        )}
                        {current && <div className="w-2 h-2 rounded-full bg-primary animate-ping" />}
                      </div>
                      <div className="flex justify-between items-start">
                        <div>
                          <p
                            className={`font-body-md text-body-md font-semibold ${
                              current ? 'text-primary' : 'text-on-surface'
                            }`}
                          >
                            {event.step}
                          </p>
                          <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                            {event.note || '—'}
                          </p>
                        </div>
                        <span
                          className={`font-mono-label text-mono-label text-right whitespace-pre-line ${
                            current ? 'text-primary' : 'text-on-surface-variant'
                          }`}
                        >
                          {formatEventTime(event.eventTime, event.etaLabel)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="w-full lg:w-1/3 bg-surface rounded-xl p-md border border-outline-variant/30 flex flex-col gap-md">
              <h4 className="font-headline-sm text-headline-sm text-on-surface">Cargo Details</h4>
              <div className="flex flex-col gap-1">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase">Commodity</span>
                <span className="font-body-md text-body-md text-on-surface">
                  {selected?.commodityDescription || 'Dried Mango'}
                </span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase">Volume / Weight</span>
                <span className="font-body-md text-body-md text-on-surface">
                  {selected?.containerCount != null
                    ? `${selected.containerCount} x ${selected.containerType || 'container'} / ${
                        selected.cargoWeightKg != null
                          ? `${Number(selected.cargoWeightKg).toLocaleString()} kg`
                          : '—'
                      }`
                    : '—'}
                </span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                  Condition Monitoring
                </span>
                <div className="mt-xs bg-surface-container-highest rounded-lg p-sm flex items-center justify-between">
                  <div className="flex items-center gap-sm">
                    <span className="material-symbols-outlined text-primary text-[20px]">device_thermostat</span>
                    <span className="font-mono-label text-mono-label text-on-surface">
                      {selected?.currentTempC != null ? `${Number(selected.currentTempC).toFixed(1)}°C` : '—'}
                    </span>
                  </div>
                  <span className="font-label-md text-label-md text-primary bg-primary/10 px-2 py-1 rounded-full">
                    {selected?.tempStatus || 'OPTIMAL'}
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={handleDownloadManifest}
                className="mt-auto w-full py-sm bg-primary text-on-primary font-label-md text-label-md uppercase tracking-wide rounded-lg hover:bg-on-primary-fixed-variant transition-colors shadow-sm"
              >
                Download Manifest
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
