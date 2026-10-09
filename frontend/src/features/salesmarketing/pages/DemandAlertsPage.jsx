import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.validationErrors && typeof data.validationErrors === 'object') {
      const details = Object.values(data.validationErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

function formatRelativeTime(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return String(dateStr);
  const diffMs = Date.now() - d.getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins} min${mins === 1 ? '' : 's'} ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? '' : 's'} ago`;
}

function formatTons(n) {
  if (n == null || Number.isNaN(Number(n))) return '—';
  return `${Number(n).toLocaleString(undefined, { maximumFractionDigits: 0 })} t`;
}

function isActiveStatus(status) {
  const s = (status || 'open').toUpperCase();
  return s === 'OPEN' || s === 'ACKNOWLEDGED' || s === '';
}

function alertTitle(alert) {
  if (alert.region) return `Dried Mango — ${alert.region}`;
  return alert.alertType || alert.alertCode || `Alert #${alert.alertId}`;
}

function severityBucket(alert) {
  const sev = (alert.severity || '').toUpperCase();
  const type = (alert.alertType || '').toLowerCase();
  if (sev === 'CRITICAL' || type.includes('high')) return 'high';
  if (sev === 'WARNING' || type.includes('low')) return 'medium';
  if (sev === 'INFO' || type.includes('deviation') || type.includes('data')) return 'data';
  return 'data';
}

const emptyAlertForm = {
  alertCode: '',
  alertType: 'High Demand',
  severity: 'critical',
  message: '',
  region: 'Germany',
  origin: 'Burkina Faso',
  status: 'open',
};

const PAGE_SIZE = 5;

export default function DemandAlertsPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  const [alerts, setAlerts] = useState([]);
  const [forecasts, setForecasts] = useState([]);
  const [pricingHistory, setPricingHistory] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState('active');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [showFilterMenu, setShowFilterMenu] = useState(false);

  const [showModal, setShowModal] = useState(false);
  const [editingAlert, setEditingAlert] = useState(null);
  const [alertForm, setAlertForm] = useState(emptyAlertForm);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [resolvingId, setResolvingId] = useState(null);

  useEffect(() => {
    fetchAlertsData();
  }, []);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
  };

  const fetchAlertsData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [alertRes, forecastRes, pricingRes] = await Promise.allSettled([
  api.getAlerts(),
  api.getDemandForecasts(),
  api.getPricingHistory(),
]);
      const failures = [];

      if (alertRes.status === 'fulfilled') {
        setAlerts(Array.isArray(alertRes.value.data) ? alertRes.value.data : []);
      } else {
        setAlerts([]);
        failures.push(`Alerts: ${getErrorMessage(alertRes.reason, 'Failed to load /api/alerts')}`);
      }

      if (forecastRes.status === 'fulfilled') {
        setForecasts(Array.isArray(forecastRes.value.data) ? forecastRes.value.data : []);
      } else {
        setForecasts([]);
        failures.push(`Forecasts: ${getErrorMessage(forecastRes.reason, 'Failed to load /api/demand-forecasts')}`);
      }
      if (pricingRes.status === 'fulfilled') {
  setPricingHistory(
    Array.isArray(pricingRes.value.data)
      ? pricingRes.value.data
      : []
  );
} else {
  setPricingHistory([]);
  failures.push(
    `Pricing History: ${getErrorMessage(
      pricingRes.reason,
      'Failed to load /api/pricing-history'
    )}`
  );
}

      if (failures.length > 0) {
        setApiError(failures.join(' · '));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to http://localhost:8080/api'));
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  };

  // ---- Derived KPIs from forecasts ----
  const baseForecasts = forecasts.filter((f) => {
    const s = (f.scenario || '').toLowerCase();
    return !s || s.includes('base');
  });
  const kpiSource = baseForecasts.length > 0 ? baseForecasts : forecasts;
  const totalBase = kpiSource.reduce((s, f) => s + Number(f.forecastedVolumeT || 0), 0);
  const highActiveCount = alerts.filter(
    (a) => isActiveStatus(a.status) && severityBucket(a) === 'high'
  ).length;
const latestPriceByProduct = pricingHistory.reduce((acc, pricing) => {
  const productId = Number(pricing.productId);
  const price = Number(pricing.priceEurPerKg);

  if (
    !productId ||
    !Number.isFinite(price) ||
    price <= 0
  ) {
    return acc;
  }

  const existing = acc[productId];

  if (
    !existing ||
    String(pricing.priceDate) > String(existing.priceDate)
  ) {
    acc[productId] = {
      priceEurPerKg: price,
      priceDate: pricing.priceDate,
    };
  }

  return acc;
}, {});

const revenuePotentialEur = kpiSource.reduce((total, forecast) => {
  const productId = Number(forecast.productId);
  const volumeT = Number(forecast.forecastedVolumeT || 0);
  const pricing = latestPriceByProduct[productId];

  if (
    !pricing ||
    !Number.isFinite(volumeT) ||
    volumeT <= 0
  ) {
    return total;
  }

  return total + volumeT * 1000 * pricing.priceEurPerKg;
}, 0);
  // ---- Filter / search ----
  const activeAlerts = alerts.filter((a) => isActiveStatus(a.status));
  const highCount = alerts.filter((a) => severityBucket(a) === 'high' && isActiveStatus(a.status)).length;
  const mediumCount = alerts.filter((a) => severityBucket(a) === 'medium' && isActiveStatus(a.status)).length;
  const dataCount = alerts.filter((a) => severityBucket(a) === 'data' && isActiveStatus(a.status)).length;

  const filteredAlerts = alerts.filter((alert) => {
    const q = searchQuery.trim().toLowerCase();
    const haystack = [
      alertTitle(alert),
      alert.message,
      alert.alertCode,
      alert.alertType,
      alert.region,
      alert.origin,
      alert.severity,
      alert.status,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    if (q && !haystack.includes(q)) return false;

    if (selectedFilter === 'active') return isActiveStatus(alert.status);
    if (selectedFilter === 'high') return severityBucket(alert) === 'high' && isActiveStatus(alert.status);
    if (selectedFilter === 'medium') return severityBucket(alert) === 'medium' && isActiveStatus(alert.status);
    if (selectedFilter === 'data') return severityBucket(alert) === 'data' && isActiveStatus(alert.status);
    if (selectedFilter === 'resolved') {
      const s = (alert.status || '').toUpperCase();
      return s === 'RESOLVED' || s === 'DISMISSED' || s === 'CLOSED';
    }
    return true;
  });

  const visibleAlerts = filteredAlerts.slice(0, visibleCount);
  const hasMore = visibleCount < filteredAlerts.length;

  // Hotspots by region
  const regionCounts = {};
  activeAlerts.forEach((a) => {
    const key = a.region || 'Unknown';
    regionCounts[key] = (regionCounts[key] || 0) + 1;
  });
  const hotspotRows = Object.entries(regionCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);
  const maxHotspot = hotspotRows[0]?.[1] || 1;

  // Resolution activity timeline
  const activityItems = [...alerts]
    .sort((a, b) => new Date(b.resolvedAt || b.createdAt || 0) - new Date(a.resolvedAt || a.createdAt || 0))
    .slice(0, 5)
    .map((a) => {
      const resolved = a.resolvedAt || (a.status || '').toUpperCase() === 'RESOLVED';
      return {
        id: a.alertId,
        text: resolved
          ? `Alert ${a.alertCode || a.alertId} resolved${a.region ? ` (${a.region})` : ''}.`
          : `Alert ${a.alertCode || a.alertId} flagged: ${(a.message || '').slice(0, 80)}${(a.message || '').length > 80 ? 'ââ‚¬Â¦' : ''}`,
        when: formatRelativeTime(a.resolvedAt || a.createdAt),
        primary: !resolved,
      };
    });

  const getAlertStyle = (severity, type) => {
    const sev = (severity || '').toUpperCase();
    if (sev === 'CRITICAL' || type === 'High Demand') {
      return {
        borderClass: 'border-l-4 border-error',
        badgeBg: 'bg-error-container text-on-error-container',
        iconBg: 'bg-error-container text-on-error-container',
        icon: 'trending_up',
        buttonText: 'Review Production',
      };
    }
    if (sev === 'WARNING' || type === 'Low Demand') {
      return {
        borderClass: 'border-l-4 border-secondary',
        badgeBg: 'bg-secondary-container text-on-secondary-container',
        iconBg: 'bg-secondary-container text-on-secondary-container',
        icon: 'trending_down',
        buttonText: 'Review Strategy',
      };
    }
    return {
      borderClass: 'border-l-4 border-primary opacity-90',
      badgeBg: 'bg-primary-container text-on-primary-container',
      iconBg: 'bg-primary-container text-on-primary-container',
      icon: 'query_stats',
      buttonText: 'Check Data',
    };
  };

  const openCreate = () => {
    setEditingAlert(null);
    setAlertForm({
      ...emptyAlertForm,
      alertCode: `ALT-${Date.now().toString().slice(-4)}`,
    });
    setShowModal(true);
  };

  const openEdit = (alert) => {
    setEditingAlert(alert);
    setAlertForm({
      alertCode: alert.alertCode || '',
      alertType: alert.alertType || '',
      severity: alert.severity || 'critical',
      message: alert.message || '',
      region: alert.region || 'Germany',
      origin: alert.origin || 'Burkina Faso',
      status: alert.status || 'open',
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setActionError(null);
    const payload = {
      alertCode: alertForm.alertCode || null,
      alertType: alertForm.alertType || null,
      severity: alertForm.severity,
      message: alertForm.message,
      region: alertForm.region || null,
      origin: alertForm.origin || null,
      status: alertForm.status || 'open',
      relatedTable: editingAlert?.relatedTable ?? null,
      relatedId: editingAlert?.relatedId ?? null,
      createdAt: editingAlert?.createdAt ?? null,
      resolvedAt:
        (alertForm.status || '').toLowerCase() === 'resolved'
          ? editingAlert?.resolvedAt || new Date().toISOString()
          : null,
    };
    try {
      if (editingAlert) {
        await api.updateAlert(editingAlert.alertId, payload);
        flashSuccess(`Alert ${payload.alertCode || editingAlert.alertId} updated.`);
      } else {
        await api.createAlert(payload);
        flashSuccess('Alert created.');
      }
      setShowModal(false);
      setEditingAlert(null);
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingAlert ? 'Failed to update alert' : 'Failed to create alert'));
    } finally {
      setSaving(false);
    }
  };

  const handleResolve = async (alert) => {
    if (resolvingId === alert.alertId) return;
    setResolvingId(alert.alertId);
    setActionError(null);
    try {
      await api.updateAlert(alert.alertId, {
        ...alert,
        status: 'resolved',
        resolvedAt: new Date().toISOString(),
      });
      flashSuccess(`${alert.alertCode || `Alert #${alert.alertId}`} marked resolved.`);
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to resolve alert'));
    } finally {
      setResolvingId(null);
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await api.deleteAlert(pendingDelete.alertId);
      flashSuccess(`${pendingDelete.alertCode || `Alert #${pendingDelete.alertId}`} deleted.`);
      setPendingDelete(null);
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete alert'));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="flex flex-col w-full px-margin-desktop py-xl max-w-[1600px] mx-auto">
      {apiError && (
        <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
            <div>
              <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={fetchAlertsData}
            className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
          </button>
        </div>
      )}

      {actionError && (
        <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <p className="font-body-sm text-body-sm text-on-error-container">{actionError}</p>
          <button type="button" onClick={() => setActionError(null)} className="text-on-error-container">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
      )}

      {actionSuccess && (
        <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <p className="font-body-sm text-body-sm text-[#1b5e20]">{actionSuccess}</p>
          <button type="button" onClick={() => setActionSuccess(null)} className="text-[#1b5e20]">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
      )}

      {/* Header */}
      <div className="mb-xxl flex flex-col sm:flex-row sm:items-end justify-between gap-md">
        <div className="flex flex-col gap-md">
          <div className="flex items-center gap-sm">
            <div className="w-1.5 h-6 bg-error rounded-full" />
            <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase tracking-tight">
              Demand Alerts Center
            </h2>
          </div>
          <p className="font-body-lg text-body-lg text-on-surface-variant max-w-2xl">
            Monitor real-time supply chain anomalies, demand spikes, and forecast deviations requiring immediate attention.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="h-12 px-lg rounded-xl bg-primary text-on-primary font-label-md text-label-md uppercase tracking-wider shadow-md hover:bg-on-primary-fixed-variant transition-colors flex items-center gap-sm shrink-0"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          New Alert
        </button>
      </div>

      {loading ? (
        <div className="p-xxl bg-surface-container-lowest rounded-xl text-center shadow-sm border border-outline-variant/20 mb-xxl">
          <span className="material-symbols-outlined text-[40px] text-primary animate-spin">sync</span>
          <p className="font-body-md text-body-md text-on-surface-variant mt-md">Loading alerts from APIââ‚¬Â¦</p>
        </div>
      ) : (
        <>
          {/* KPIs */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter mb-xxl">
            <div className="bg-surface-container rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
              <div className="absolute -right-4 -top-4 w-32 h-32 bg-primary/5 rounded-full blur-xl group-hover:scale-110 transition-transform" />
              <div className="flex justify-between items-start mb-xl relative z-10">
                <div>
                  <p className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs">Base Case</p>
                  <h3 className="font-display-lg text-display-lg text-on-surface">
                    {kpiSource.length > 0 ? formatTons(totalBase) : 'No data'}
                  </h3>
                </div>
                <div className="w-10 h-10 rounded-full bg-primary-container flex items-center justify-center">
                  <span className="material-symbols-outlined text-on-primary-container">notifications_active</span>
                </div>
              </div>
              <div className="flex items-center gap-sm relative z-10">
                <span className="font-mono-label text-mono-label text-error flex items-center">
                  <span className="material-symbols-outlined text-[16px] mr-1">notifications</span>
                  {activeAlerts.length} active
                </span>
                <span className="font-body-sm text-body-sm text-on-surface-variant">from /api/alerts</span>
              </div>
            </div>

            <div className="bg-error-container rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
              <div className="absolute -right-4 -bottom-4 w-40 h-40 bg-error/5 rounded-full blur-2xl group-hover:scale-110 transition-transform" />
              <div className="flex justify-between items-start mb-xl relative z-10">
                <div>
                  <p className="font-label-md text-label-md text-on-error-container uppercase mb-xs">Germany Base Case</p>
                  <h3 className="font-display-lg text-display-lg text-on-error-container">
                    No data
                  </h3>
                </div>
                <div className="w-10 h-10 rounded-full bg-error flex items-center justify-center">
                  <span className="material-symbols-outlined text-on-error">warning</span>
                </div>
              </div>
              <div className="flex items-center gap-sm relative z-10">
                <span className="font-mono-label text-mono-label text-on-error-container flex items-center font-bold">
                  <span className="material-symbols-outlined text-[16px] mr-1">priority_high</span>
                  {highActiveCount > 0 ? `${highActiveCount} High Priority` : 'No high alerts'}
                </span>
              </div>
            </div>

            <div className="bg-surface-container rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
              <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-48 h-48 bg-secondary/5 rounded-full blur-3xl group-hover:scale-105 transition-transform" />
              <div className="flex justify-between items-start mb-xl relative z-10">
                <div>
                  <p className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs">Revenue Potential</p>
                  <h3 className="font-display-lg text-display-lg text-on-surface">
                    {revenuePotentialEur > 0
  ? `ââ€šÂ¬${Math.round(revenuePotentialEur).toLocaleString('en-US')}`
  : '—'}
                  </h3>
                </div>
                <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center">
                  <span className="material-symbols-outlined text-on-secondary-container">timer</span>
                </div>
              </div>
              <div className="flex items-center gap-sm relative z-10">

                  <span className="font-mono-label text-mono-label text-primary flex items-center font-semibold">
  <span className="material-symbols-outlined text-[16px] mr-1">
    euro
  </span>
  Based on latest applied pricing
</span>

<span className="font-body-sm text-body-sm text-on-surface-variant">
  from demand forecasts

</span>

              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter">
            <div className="lg:col-span-8 flex flex-col gap-lg">
              {/* Controls */}
              <div className="flex flex-col sm:flex-row justify-between items-center bg-surface-container-lowest p-md rounded-xl shadow-sm border border-outline-variant/20 gap-md">
                <div className="flex items-center gap-md w-full sm:w-auto">
                  <div className="relative w-full sm:w-64">
                    <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                      search
                    </span>
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        setVisibleCount(PAGE_SIZE);
                      }}
                      className="w-full bg-surface pl-10 pr-4 py-2 rounded-lg font-body-sm text-body-sm text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all border border-outline-variant/40"
                      placeholder="Search alerts..."
                    />
                  </div>
                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => setShowFilterMenu((value) => !value)}
                      title="Choose alert filter"
                      aria-expanded={showFilterMenu}
                      className={`flex items-center justify-center w-10 h-10 rounded-lg transition-colors border border-outline-variant/40 ${
                        showFilterMenu || selectedFilter === 'resolved'
                          ? 'bg-primary text-on-primary'
                          : 'bg-surface hover:bg-surface-variant text-on-surface-variant'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[20px]">filter_list</span>
                    </button>
                    {showFilterMenu && (
                      <div className="absolute left-0 top-12 z-20 w-44 rounded-lg border border-outline-variant/40 bg-surface-container-lowest p-xs shadow-lg">
                        {[
                          ['active', 'Active alerts'],
                          ['high', 'High priority'],
                          ['medium', 'Medium priority'],
                          ['data', 'Data availability'],
                          ['resolved', 'Resolved alerts'],
                          ['all', 'All alerts'],
                        ].map(([filterId, label]) => (
                          <button
                            key={filterId}
                            type="button"
                            onClick={() => {
                              setSelectedFilter(filterId);
                              setVisibleCount(PAGE_SIZE);
                              setShowFilterMenu(false);
                            }}
                            className={`w-full rounded-md px-sm py-sm text-left font-body-sm text-body-sm hover:bg-surface-container ${
                              selectedFilter === filterId ? 'bg-primary/10 text-primary font-semibold' : 'text-on-surface'
                            }`}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-sm w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
                  {[
                    { id: 'active', label: `Active (${activeAlerts.length})`, activeClass: 'bg-on-surface text-surface' },
                    { id: 'high', label: `High Priority (${highCount})`, activeClass: 'bg-error text-on-error font-bold' },
                    { id: 'medium', label: `Medium Priority (${mediumCount})`, activeClass: 'bg-secondary text-on-secondary font-bold' },
                    { id: 'data', label: `Data Availability (${dataCount})`, activeClass: 'bg-primary text-on-primary font-bold' },
                  ].map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => {
                        setSelectedFilter(f.id);
                        setVisibleCount(PAGE_SIZE);
                      }}
                      className={`px-4 py-1.5 rounded-full font-label-md text-label-md whitespace-nowrap transition-colors ${
                        selectedFilter === f.id
                          ? f.activeClass
                          : 'bg-surface-variant text-on-surface-variant hover:bg-surface-container-high'
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* List */}
              <div className="flex flex-col gap-md">
                {visibleAlerts.length === 0 ? (
                  <div className="p-xl bg-surface-container-lowest rounded-xl text-center border border-outline-variant/30">
                    <span className="material-symbols-outlined text-[48px] text-on-surface-variant opacity-40 mb-xs">
                      check_circle
                    </span>
                    <p className="font-headline-sm text-headline-sm text-on-surface">No matching alerts</p>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">
                      {alerts.length === 0
                        ? 'No rows returned from /api/alerts.'
                        : 'All supply chain anomaly signals are clear for this filter.'}
                    </p>
                  </div>
                ) : (
                  visibleAlerts.map((alertItem) => {
                    const style = getAlertStyle(alertItem.severity, alertItem.alertType);
                    const resolved = !isActiveStatus(alertItem.status);
                    return (
                      <div
                        key={alertItem.alertId}
                        className={`bg-surface-container-lowest rounded-xl p-lg shadow-sm ${style.borderClass} relative overflow-hidden group border border-outline-variant/20 ${
                          resolved ? 'opacity-70' : ''
                        }`}
                      >
                        <div className="flex flex-col sm:flex-row gap-lg justify-between items-start relative z-10">
                          <div className="flex gap-md w-full sm:w-3/4">
                            <div className={`mt-1 w-8 h-8 rounded-full ${style.iconBg} flex items-center justify-center shrink-0`}>
                              <span className="material-symbols-outlined text-[18px]">{style.icon}</span>
                            </div>
                            <div>
                              <div className="flex items-center gap-sm mb-1 flex-wrap">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${style.badgeBg}`}>
                                  {alertItem.alertType || alertItem.severity || 'ALERT'}
                                </span>
                                <span className="font-mono-label text-mono-label text-on-surface-variant">
                                  {alertItem.alertCode || `#ALT-${alertItem.alertId}`}
                                </span>
                                <span className="w-1 h-1 bg-outline-variant rounded-full" />
                                <span className="font-body-sm text-body-sm text-on-surface-variant text-[12px]">
                                  {formatRelativeTime(alertItem.createdAt)}
                                </span>
                                {resolved && (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-surface-container text-on-surface-variant">
                                    {alertItem.status}
                                  </span>
                                )}
                              </div>
                              <h4 className="font-headline-sm text-headline-sm text-on-surface mb-xs">
                                {alertTitle(alertItem)}
                              </h4>
                              <p className="font-body-sm text-body-sm text-on-surface-variant line-clamp-2">
                                {alertItem.message}
                              </p>
                              <div className="mt-md flex flex-wrap gap-2">
                                <span className="px-3 py-1 bg-surface-container rounded-full font-label-md text-label-md text-on-surface flex items-center gap-1">
                                  <span className="material-symbols-outlined text-[14px]">location_on</span>
                                  Region: {alertItem.region || '—'}
                                </span>
                                <span className="px-3 py-1 bg-surface-container rounded-full font-label-md text-label-md text-on-surface flex items-center gap-1">
                                  <span className="material-symbols-outlined text-[14px]">public</span>
                                  Origin: {alertItem.origin || '—'}
                                </span>
                              </div>
                            </div>
                          </div>
                          <div className="flex sm:flex-col gap-sm w-full sm:w-auto shrink-0 justify-end sm:justify-start">
                            {!resolved && (
                              <button
                                type="button"
                                disabled={resolvingId === alertItem.alertId}
                                onClick={() => handleResolve(alertItem)}
                                className="px-4 py-2 bg-primary text-on-primary font-label-md text-label-md rounded-lg hover:bg-on-primary-fixed-variant transition-colors shadow-sm flex-1 sm:flex-none text-center disabled:opacity-60"
                              >
                                {resolvingId === alertItem.alertId ? 'Resolvingââ‚¬Â¦' : style.buttonText}
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() => openEdit(alertItem)}
                              className="px-4 py-2 bg-surface-container text-primary font-label-md text-label-md rounded-lg hover:bg-surface-container-high transition-colors flex-1 sm:flex-none text-center"
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              onClick={() => setPendingDelete(alertItem)}
                              className="px-4 py-2 bg-transparent text-on-surface-variant font-label-md text-label-md rounded-lg hover:bg-surface-variant transition-colors flex-1 sm:flex-none text-center"
                            >
                              Dismiss
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              {hasMore && (
                <div className="flex justify-center mt-md">
                  <button
                    type="button"
                    onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
                    className="px-6 py-2 rounded-full font-label-md text-label-md text-primary bg-primary/10 hover:bg-primary/20 transition-colors"
                  >
                    Load More Alerts ({filteredAlerts.length - visibleCount} remaining)
                  </button>
                </div>
              )}
            </div>

            {/* Right sidebar */}
            <div className="lg:col-span-4 flex flex-col gap-lg">
              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm border border-outline-variant/20">
                <h3 className="font-headline-sm text-headline-sm text-on-surface mb-md">Anomaly Hotspots</h3>
                <div
                  className="w-full h-48 rounded-lg mb-md bg-cover bg-center overflow-hidden relative"
                  style={{
                    backgroundImage: `url('https://lh3.googleusercontent.com/aida-public/AB6AXuCPMh0faMDlycH08Yujx3yGlq7yjt36RSE9NUIx9WH7T8PTaXZTvYEt9FvG_KDQSXo2vkzBaB_UpXhgZwkDqHMmIi_iFcwubZsbkAc31-rUTo-eUmM8HFauJ6zFvZGOV_x9iTWQvrfKtGHoykBDcs_LLEOEAkXrv02AVm25JZTk7t4kZxxL0f_jr1hYOWBdkf7KRbKuSxqmLtIwsSj_m--UCikwr4Z44WfemgWxbpeLZMuvQOPYxJ5j')`,
                  }}
                >
                  <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/80 to-transparent" />
                  {hotspotRows.length > 0 && (
                    <>
                      <div className="absolute top-1/2 left-1/3 w-3 h-3 bg-error rounded-full animate-ping opacity-75" />
                      <div className="absolute top-1/2 left-1/3 w-3 h-3 bg-error rounded-full" />
                    </>
                  )}
                </div>
                <div className="space-y-sm">
                  {hotspotRows.length === 0 ? (
                    <p className="font-body-sm text-body-sm text-on-surface-variant">No active regional alerts.</p>
                  ) : (
                    hotspotRows.map(([region, count]) => (
                      <div key={region} className="flex justify-between items-center py-2 border-b border-surface-variant">
                        <span className="font-body-sm text-body-sm text-on-surface">
                          {region.includes('German') || region === 'Germany' ? `EU - ${region}` : region}
                        </span>
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-2 bg-surface-variant rounded-full overflow-hidden">
                            <div
                              className="h-full bg-error"
                              style={{ width: `${Math.max(12, (count / maxHotspot) * 100)}%` }}
                            />
                          </div>
                          <span className="font-mono-label text-mono-label text-error font-bold">{count}</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm border border-outline-variant/20">
                <h3 className="font-headline-sm text-headline-sm text-on-surface mb-lg">Resolution Activity</h3>
                <div className="relative pl-6 border-l-2 border-surface-variant space-y-lg">
                  {activityItems.length === 0 ? (
                    <p className="font-body-sm text-body-sm text-on-surface-variant">No recent alert activity.</p>
                  ) : (
                    activityItems.map((item) => (
                      <div key={item.id} className="relative">
                        <div
                          className={`absolute -left-[31px] top-1 w-4 h-4 rounded-full border-2 border-surface-container-lowest flex items-center justify-center ${
                            item.primary ? 'bg-primary' : 'bg-surface-variant'
                          }`}
                        />
                        <p className="font-body-sm text-body-sm text-on-surface mb-xs">{item.text}</p>
                        <p className="font-mono-label text-mono-label text-on-surface-variant text-[11px]">{item.when}</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Create / Edit modal */}
      {showModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingAlert ? `Edit Alert #${editingAlert.alertId}` : 'New Alert'}
            </h3>
            <form onSubmit={handleSave} className="flex flex-col gap-md">
              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Code</label>
                  <input
                    value={alertForm.alertCode}
                    onChange={(e) => setAlertForm((f) => ({ ...f, alertCode: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Type</label>
                  <input
                    required
                    value={alertForm.alertType}
                    onChange={(e) => setAlertForm((f) => ({ ...f, alertType: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Severity *</label>
                  <select
                    required
                    value={alertForm.severity}
                    onChange={(e) => setAlertForm((f) => ({ ...f, severity: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  >
                    <option value="critical">critical</option>
                    <option value="warning">warning</option>
                    <option value="info">info</option>
                  </select>
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Status</label>
                  <select
                    value={alertForm.status}
                    onChange={(e) => setAlertForm((f) => ({ ...f, status: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  >
                    <option value="open">open</option>
                    <option value="acknowledged">acknowledged</option>
                    <option value="resolved">resolved</option>
                    <option value="dismissed">dismissed</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Message *</label>
                <textarea
                  required
                  rows={3}
                  value={alertForm.message}
                  onChange={(e) => setAlertForm((f) => ({ ...f, message: e.target.value }))}
                  className="w-full px-md py-sm rounded-lg bg-surface-container font-body-sm resize-y"
                />
              </div>
              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Region</label>
                  <input
                    value={alertForm.region}
                    onChange={(e) => setAlertForm((f) => ({ ...f, region: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Origin</label>
                  <input
                    value={alertForm.origin}
                    onChange={(e) => setAlertForm((f) => ({ ...f, origin: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
              </div>
              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60"
                >
                  {saving ? 'Savingââ‚¬Â¦' : editingAlert ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete / Dismiss confirm */}
      {pendingDelete && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Dismiss alert?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This permanently deletes via{' '}
              <code className="text-primary">DELETE /api/alerts/{pendingDelete.alertId}</code> (
              {pendingDelete.alertCode || `#${pendingDelete.alertId}`}).
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg line-clamp-3">
              {pendingDelete.message}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDelete(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={confirmDelete}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60"
              >
                {deleting ? 'Deletingââ‚¬Â¦' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
