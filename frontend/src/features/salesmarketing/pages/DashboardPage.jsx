import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

function formatTons(value) {
  if (value == null || Number.isNaN(Number(value))) return '—';
  return `${Number(value).toLocaleString(undefined, { maximumFractionDigits: 0 })} t`;
}

function formatEuroMillions(tons, priceEurPerKg) {
  if (
    tons == null ||
    Number.isNaN(Number(tons)) ||
    priceEurPerKg == null ||
    Number.isNaN(Number(priceEurPerKg))
  ) {
    return '—';
  }

  const eur = Number(tons) * 1000 * Number(priceEurPerKg);

  return `€${(eur / 1_000_000).toFixed(1)}M`;
}


function monthLabel(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return String(dateStr).slice(0, 7);
  return d.toLocaleDateString('en-US', { month: 'short' });
}

function buildChartPaths(forecasts) {
  const points = [...forecasts]
    .filter((f) => f.forecastedVolumeT != null)
    .sort((a, b) => new Date(a.forecastMonth) - new Date(b.forecastMonth))
    .slice(0, 9);

  if (points.length < 2) return null;

  const values = points.map((p) => Number(p.forecastedVolumeT));
  const lowers = points.map((p) => Number(p.lowerBoundT ?? p.forecastedVolumeT));
  const uppers = points.map((p) => Number(p.upperBoundT ?? p.forecastedVolumeT));
  const maxY = Math.max(...uppers, ...values, 1);
  const minY = 0;
  const range = maxY - minY || 1;

  const toY = (v) => 280 - ((v - minY) / range) * 250;
  const toX = (i) => (i / (points.length - 1)) * 1000;

  const splitAt = Math.max(1, Math.floor(points.length * 0.6));

  const line = (arr) =>
    arr.map((v, i) => `${i === 0 ? 'M' : 'L'} ${toX(i)} ${toY(v)}`).join(' ');

  const histVals = values.slice(0, splitAt + 1);
  const forecastVals = values.slice(splitAt);
  const forecastXs = forecastVals.map((_, i) => toX(i + splitAt));

  const bandUpper = uppers.slice(splitAt);
  const bandLower = lowers.slice(splitAt).reverse();
  const bandPath = [
    ...bandUpper.map((v, i) => `${i === 0 ? 'M' : 'L'} ${forecastXs[i]} ${toY(v)}`),
    ...bandLower.map((v, i) => `L ${forecastXs[forecastXs.length - 1 - i]} ${toY(v)}`),
    'Z',
  ].join(' ');

  const histArea = `${line(histVals)} L ${toX(splitAt)} 300 L 0 300 Z`;

  return {
    labels: points.map((p) => monthLabel(p.forecastMonth)),
    histLine: line(histVals),
    histArea,
    forecastLine: line(values.slice(splitAt - 1)),
    bandPath,
    todayX: toX(splitAt),
    maxLabel: Math.ceil(maxY / 1000) * 1000 || 1000,
  };
}

const emptyForecastForm = {
  productId: '',
  forecastMonth: '',
  scenario: 'base',
  forecastedVolumeT: '',
  lowerBoundT: '',
  upperBoundT: '',
  capacityRisk: 'low',
  modelVersion: 'v1',
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [forecasts, setForecasts] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [productBatches, setProductBatches] = useState([]);
  const [products, setProducts] = useState([]);

  const [showForecastModal, setShowForecastModal] = useState(false);
  const [forecastForm, setForecastForm] = useState(emptyForecastForm);
  const [savingForecast, setSavingForecast] = useState(false);
  const [pendingDeleteAlert, setPendingDeleteAlert] = useState(null);
  const [deletingAlert, setDeletingAlert] = useState(false);
  const [pricingHistory, setPricingHistory] = useState([]);
  const [customers, setCustomers] = useState([]);
const [orders, setOrders] = useState([]);

  useEffect(() => {
    fetchDashboardData();
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

  const fetchDashboardData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [
  forecastRes,
  alertRes,
  batchRes,
  productRes,
  pricingRes,
  customerRes,
  orderRes,
] = await Promise.allSettled([
  api.getDemandForecasts(),
  api.getAlerts(),
  api.getProductBatches(),
  api.getProducts(),
  api.getPricingHistory(),
  api.getCustomers(),
  api.getOrders(),
]);

      const failures = [];

      if (forecastRes.status === 'fulfilled') {
        setForecasts(Array.isArray(forecastRes.value.data) ? forecastRes.value.data : []);
      } else {
        setForecasts([]);
        failures.push(`Forecasts: ${getErrorMessage(forecastRes.reason)}`);
      }

      if (alertRes.status === 'fulfilled') {
        setAlerts(Array.isArray(alertRes.value.data) ? alertRes.value.data : []);
      } else {
        setAlerts([]);
        failures.push(`Alerts: ${getErrorMessage(alertRes.reason)}`);
      }

      if (batchRes.status === 'fulfilled') {
        setProductBatches(Array.isArray(batchRes.value.data) ? batchRes.value.data : []);
      } else {
        setProductBatches([]);
        failures.push(`Batches: ${getErrorMessage(batchRes.reason)}`);
      }

      if (productRes.status === 'fulfilled') {
        setProducts(Array.isArray(productRes.value.data) ? productRes.value.data : []);
      } else {
        setProducts([]);
        failures.push(`Products: ${getErrorMessage(productRes.reason)}`);
      }

      if (pricingRes.status === 'fulfilled') {
        setPricingHistory(
          Array.isArray(pricingRes.value.data) ? pricingRes.value.data : []
        );
      } else {
        setPricingHistory([]);
        failures.push(`Pricing: ${getErrorMessage(pricingRes.reason)}`);
      }
      if (customerRes.status === 'fulfilled') {
  setCustomers(
    Array.isArray(customerRes.value.data)
      ? customerRes.value.data
      : []
  );
} else {
  setCustomers([]);
  failures.push(`Customers: ${getErrorMessage(customerRes.reason)}`);
}

if (orderRes.status === 'fulfilled') {
  setOrders(
    Array.isArray(orderRes.value.data)
      ? orderRes.value.data
      : []
  );
} else {
  setOrders([]);
  failures.push(`Orders: ${getErrorMessage(orderRes.reason)}`);
}

      if (failures.length === 5) {
        setApiError(failures.join(' · ') || 'Unable to reach the Sales & Marketing API.');
      } else if (failures.length > 0) {
        setApiError(`Partial load — ${failures.join(' · ')}`);
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to http://localhost:8080/api'));
      setForecasts([]);
      setAlerts([]);
      setProductBatches([]);
    } finally {
      setLoading(false);
    }
  };

  // ---- Derived KPIs from real forecast rows ----
  const baseForecasts = forecasts.filter((f) => {
    const s = (f.scenario || '').toLowerCase();
    return !s || s.includes('base');
  });
  const kpiSource = baseForecasts.length > 0 ? baseForecasts : forecasts;
  const forecastProductId = kpiSource.find((f) => f.productId != null)?.productId;

const latestPricing = pricingHistory
  .filter((p) => p.productId === forecastProductId)
  .sort(
    (a, b) =>
      new Date(b.priceDate).getTime() - new Date(a.priceDate).getTime()
  )[0];

const currentPriceEurPerKg =
  latestPricing?.priceEurPerKg != null
    ? Number(latestPricing.priceEurPerKg)
    : null;

  const totalBase = kpiSource.reduce((sum, f) => sum + Number(f.forecastedVolumeT || 0), 0);
  const totalLower = kpiSource.reduce((sum, f) => sum + Number(f.lowerBoundT ?? f.forecastedVolumeT ?? 0), 0);
  const totalUpper = kpiSource.reduce((sum, f) => sum + Number(f.upperBoundT ?? f.forecastedVolumeT ?? 0), 0);

  const activeAlerts = alerts.filter((a) => {
    const s = (a.status || 'ACTIVE').toUpperCase();
    return s === 'ACTIVE' || s === 'OPEN' || s === 'NEW';
  });
  const displayAlerts = activeAlerts.length > 0 ? activeAlerts : alerts;

  const chart = buildChartPaths(kpiSource.length > 0 ? kpiSource : forecasts);
  const yTicks = chart
    ? [chart.maxLabel, chart.maxLabel * 0.75, chart.maxLabel * 0.5, chart.maxLabel * 0.25, 0].map((n) =>
        n >= 1000 ? `${Math.round(n / 1000)}k` : String(Math.round(n))
      )
    : ['50k', '40k', '30k', '20k', '10k', '0'];

  const getStatusBadge = (status) => {
    switch ((status || '').toLowerCase()) {
      case 'processed':
        return <span className="inline-flex items-center px-2 py-1 rounded-md bg-[#10b981]/10 text-[#059669] font-label-md text-[10px] uppercase tracking-wider">Processed</span>;
      case 'in transit':
      case 'in_transit':
        return <span className="inline-flex items-center px-2 py-1 rounded-md bg-secondary/10 text-secondary font-label-md text-[10px] uppercase tracking-wider">In Transit</span>;
      case 'scheduled':
        return <span className="inline-flex items-center px-2 py-1 rounded-md bg-primary/10 text-primary font-label-md text-[10px] uppercase tracking-wider">Scheduled</span>;
      case 'delayed':
        return <span className="inline-flex items-center px-2 py-1 rounded-md bg-error/10 text-error font-label-md text-[10px] uppercase tracking-wider">Delayed</span>;
      default:
        return <span className="inline-flex items-center px-2 py-1 rounded-md bg-surface-container text-on-surface-variant font-label-md text-[10px] uppercase tracking-wider">{status || '—'}</span>;
    }
  };

  const openNewForecast = () => {
    setForecastForm({
      ...emptyForecastForm,
      productId: products[0]?.productId ? String(products[0].productId) : '',
      forecastMonth: new Date().toISOString().slice(0, 7) + '-01',
    });
    setShowForecastModal(true);
    setActionError(null);
  };

  const handleCreateForecast = async (e) => {
    e.preventDefault();
    setSavingForecast(true);
    setActionError(null);
    try {
      const payload = {
        productId: forecastForm.productId ? Number(forecastForm.productId) : null,
        forecastMonth: forecastForm.forecastMonth,
        scenario: forecastForm.scenario,
        forecastedVolumeT: Number(forecastForm.forecastedVolumeT),
        lowerBoundT: forecastForm.lowerBoundT !== '' ? Number(forecastForm.lowerBoundT) : null,
        upperBoundT: forecastForm.upperBoundT !== '' ? Number(forecastForm.upperBoundT) : null,
        capacityRisk: forecastForm.capacityRisk || null,
        isActual: false,
        modelVersion: forecastForm.modelVersion || null,
      };
      await api.createDemandForecast(payload);
      setShowForecastModal(false);
      flashSuccess('Forecast created successfully.');
      await fetchDashboardData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to create forecast'));
    } finally {
      setSavingForecast(false);
    }
  };

  const handleExportReport = () => {
    if (forecasts.length === 0) {
      flashError('No forecast data available to export.');
      return;
    }
    const headers = [
      'forecastId',
      'productId',
      'forecastMonth',
      'scenario',
      'forecastedVolumeT',
      'lowerBoundT',
      'upperBoundT',
      'capacityRisk',
      'modelVersion',
    ];
    const rows = forecasts.map((f) =>
      headers.map((h) => {
        const val = f[h] ?? '';
        const str = String(val).replace(/"/g, '""');
        return `"${str}"`;
      }).join(',')
    );
    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `demand-forecasts-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    flashSuccess('Forecast export downloaded.');
  };

  const confirmDeleteAlert = async () => {
    if (!pendingDeleteAlert) return;
    setDeletingAlert(true);
    try {
      await api.deleteAlert(pendingDeleteAlert.alertId);
      setPendingDeleteAlert(null);
      flashSuccess(`Alert ${pendingDeleteAlert.alertCode || pendingDeleteAlert.alertId} deleted.`);
      await fetchDashboardData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete alert'));
    } finally {
      setDeletingAlert(false);
    }
    };

  const germanyDemandT = customers
    .filter(
      (customer) =>
        String(customer.country || '').toLowerCase() === 'germany'
    )
    .reduce((total, customer) => {
      const customerId = Number(customer.customerId);

      const customerVolumeKg = orders
        .filter(
          (order) =>
            Number(order.customerId) === customerId
        )
        .reduce(
          (sum, order) =>
            sum + Number(order.totalVolumeKg ?? 0),
          0
        );

      return total + customerVolumeKg / 1000;
    }, 0);
    console.log('GERMANY DEMAND DEBUG');
console.log('Customers:', customers);
console.log('Orders:', orders);
console.log('Germany customers:', customers.filter(
  (customer) =>
    String(customer.country || '').toLowerCase() === 'germany'
));
console.log('Germany Demand MT:', germanyDemandT);

  return (
    <div className="flex flex-col w-full px-margin-desktop py-xl gap-gutter max-w-[1600px] mx-auto">
      {/* Load / action banners */}
      {apiError && (
        <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
            <div>
              <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
            </div>
          </div>
          <button
            onClick={fetchDashboardData}
            className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
          </button>
        </div>
      )}

      {actionError && (
        <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-error text-[24px]">error</span>
            <div>
              <p className="font-label-md text-label-md text-on-error-container font-bold">Action failed</p>
              <p className="font-body-sm text-body-sm text-on-error-container">{actionError}</p>
            </div>
          </div>
          <button onClick={() => setActionError(null)} className="text-on-error-container px-sm">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
      )}

      {actionSuccess && (
        <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-[#2e7d32] text-[24px]">check_circle</span>
            <p className="font-body-sm text-body-sm text-[#1b5e20]">{actionSuccess}</p>
          </div>
          <button onClick={() => setActionSuccess(null)} className="text-[#1b5e20] px-sm">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
      )}

      {/* Header Area */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-md mb-md">
        <div>
          <div className="flex items-center gap-xs text-on-surface-variant font-mono-label text-mono-label uppercase tracking-widest mb-xs">
            <span className="material-symbols-outlined text-[16px]">monitoring</span>
            <span>Demand Intelligence</span>
          </div>
          <h2 className="font-headline-lg text-headline-lg text-on-background">Overview</h2>
        </div>
        <div className="flex gap-md">
          <button
            type="button"
            onClick={handleExportReport}
            className="bg-surface-container hover:bg-surface-container-high text-primary px-lg py-sm rounded-full font-label-md text-label-md transition-colors flex items-center gap-sm"
          >
            <span className="material-symbols-outlined text-[18px]">download</span>
            EXPORT REPORT
          </button>
          <button
            type="button"
            onClick={openNewForecast}
            className="bg-primary hover:bg-on-primary-fixed text-on-primary px-lg py-sm rounded-full font-label-md text-label-md transition-colors shadow-md flex items-center gap-sm"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            NEW FORECAST
          </button>
        </div>
      </div>

      {loading ? (
        <div className="p-xxl bg-surface-container-lowest rounded-xl shadow-sm text-center border border-outline-variant/20">
          <span className="material-symbols-outlined text-[40px] text-primary animate-spin">sync</span>
          <p className="font-body-md text-body-md text-on-surface-variant mt-md">Loading dashboard from API…</p>
        </div>
      ) : (
        <>
          {/* KPI Cards Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter mb-lg">
            <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary" />
              <div className="flex justify-between items-start mb-md">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase tracking-wider">Forecasted Demand (Base)</span>
                <span className="material-symbols-outlined text-primary bg-primary-fixed-dim/20 p-xs rounded-lg">trending_up</span>
              </div>
              <div className="font-display-lg text-display-lg text-on-surface mb-sm">
                {kpiSource.length > 0 ? formatTons(totalBase) : 'No data'}
              </div>
              <div className="flex items-center gap-xs text-primary font-label-md text-label-md">
                <span className="material-symbols-outlined text-[16px]">info</span>
                <span>
                  {kpiSource.length > 0
                    ? `Range: ${formatTons(totalLower).replace(' t', '')} – ${formatTons(totalUpper)}`
                    : 'Awaiting forecast rows'}
                </span>
              </div>
            </div>

            <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-secondary" />
              <div className="flex justify-between items-start mb-md">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase tracking-wider">Germany Demand</span>
                <span className="material-symbols-outlined text-secondary bg-secondary-fixed/50 p-xs rounded-lg">location_on</span>
              </div>
              <div className="font-display-lg text-display-lg text-on-surface mb-sm">
                {germanyDemandT > 0
  ? `${germanyDemandT.toFixed(1)} MT`
  : 'No data'}
              </div>
              <div className="flex items-center gap-xs text-secondary font-label-md text-label-md">
                <span className="material-symbols-outlined text-[16px]">analytics</span>
                <span>No regional forecast field available</span>
              </div>
            </div>

            <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-[#10b981]" />
              <div className="flex justify-between items-start mb-md">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase tracking-wider">Active Alerts</span>
                <span className="material-symbols-outlined text-[#10b981] bg-[#10b981]/10 p-xs rounded-lg">notifications_active</span>
              </div>
              <div className="font-display-lg text-display-lg text-on-surface mb-sm">
                {displayAlerts.length} Active
              </div>
              <div className="flex items-center gap-xs text-[#10b981] font-label-md text-label-md">
                <span className={`w-2 h-2 rounded-full ${apiError ? 'bg-secondary' : 'bg-[#10b981]'} animate-pulse`} />
                <span>{apiError ? 'Degraded connectivity' : 'System operational'}</span>
              </div>
            </div>

            <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm relative overflow-hidden group hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-error" />
              <div className="flex justify-between items-start mb-md">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase tracking-wider">Revenue Potential</span>
                <span className="material-symbols-outlined text-error bg-error-container p-xs rounded-lg">euro</span>
              </div>
              <div className="font-display-lg text-display-lg text-on-surface mb-sm">
                { kpiSource.length > 0
  ? formatEuroMillions(totalBase, currentPriceEurPerKg)
  : 'No data' }
              </div>
              <div className="flex items-center gap-xs text-primary font-label-md text-label-md">
                <span className="material-symbols-outlined text-[16px]">trending_up</span>
                <span>
                  {currentPriceEurPerKg != null
                    ? `Latest price: €${currentPriceEurPerKg.toFixed(2)}/kg`
                    : 'No pricing data loaded'}
                </span>
              </div>
            </div>
          </div>

          {/* Main Content Area */}
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-gutter mb-lg">
            <div className="xl:col-span-8 flex flex-col gap-gutter">
              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm h-[500px] flex flex-col relative">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-xl gap-sm">
                  <div>
                    <h3 className="font-headline-md text-headline-md text-on-surface">Demand Forecast vs. Historical</h3>
                    <p className="font-body-sm text-body-sm text-on-surface-variant mt-xs">
                      {forecasts.length > 0
                        ? `${forecasts.length} forecast row(s) from /api/demand-forecasts`
                        : '90-day projection based on current harvest yields'}
                    </p>
                  </div>
                  <div className="flex items-center gap-md">
                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-primary opacity-50" />
                      <span className="font-label-md text-label-md text-on-surface-variant">Historical</span>
                    </div>
                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-primary" />
                      <span className="font-label-md text-label-md text-on-surface-variant">Forecast</span>
                    </div>
                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-primary/20" />
                      <span className="font-label-md text-label-md text-on-surface-variant">Confidence Band</span>
                    </div>
                  </div>
                </div>

                <div className="flex-1 w-full relative">
                  {!chart ? (
                    <div className="absolute inset-0 flex flex-col items-center justify-center text-on-surface-variant">
                      <span className="material-symbols-outlined text-[40px] opacity-40 mb-sm">show_chart</span>
                      <p className="font-body-sm text-body-sm">Not enough forecast points to draw a chart yet.</p>
                      <button
                        type="button"
                        onClick={openNewForecast}
                        className="mt-md text-primary font-label-md text-label-md hover:underline"
                      >
                        Create a forecast
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="absolute left-0 top-0 bottom-8 flex flex-col justify-between text-on-surface-variant font-mono-label text-mono-label text-[10px] pb-[10px]">
                        {yTicks.map((t) => (
                          <span key={t}>{t}</span>
                        ))}
                      </div>
                      <div className="absolute left-10 right-0 top-2 bottom-8">
                        <div className="absolute inset-0 flex flex-col justify-between">
                          <div className="w-full h-px bg-outline-variant/30" />
                          <div className="w-full h-px bg-outline-variant/30" />
                          <div className="w-full h-px bg-outline-variant/30" />
                          <div className="w-full h-px bg-outline-variant/30" />
                          <div className="w-full h-px bg-outline-variant/30" />
                          <div className="w-full h-px bg-outline-variant/50" />
                        </div>
                        <div
                          className="absolute top-0 bottom-0 border-l-2 border-dashed border-secondary z-10 flex flex-col items-center"
                          style={{ left: `${(chart.todayX / 1000) * 100}%` }}
                        >
                          <div className="bg-secondary text-on-secondary font-label-md text-label-md px-2 py-1 rounded-full -mt-3 shadow-sm whitespace-nowrap">
                            TODAY
                          </div>
                        </div>
                        <svg className="w-full h-full overflow-visible" preserveAspectRatio="none" viewBox="0 0 1000 300">
                          <path className="text-primary/10" d={chart.bandPath} fill="currentColor" />
                          <path className="text-primary/50" d={chart.histLine} fill="none" stroke="currentColor" strokeWidth="3" />
                          <path className="text-primary/5" d={chart.histArea} fill="currentColor" />
                          <path
                            className="text-primary"
                            d={chart.forecastLine}
                            fill="none"
                            stroke="currentColor"
                            strokeDasharray="8,8"
                            strokeWidth="4"
                          />
                        </svg>
                      </div>
                      <div className="absolute left-10 right-0 bottom-0 flex justify-between text-on-surface-variant font-mono-label text-mono-label text-[10px] pt-sm border-t border-outline-variant/30">
                        {chart.labels.map((label, i) => (
                          <span key={`${label}-${i}`} className={i === Math.floor(chart.labels.length * 0.6) ? 'font-bold text-primary' : ''}>
                            {label}
                          </span>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              </div>

              <div className="bg-surface-container-lowest rounded-xl p-md shadow-sm flex items-center gap-md">
                <div className="w-12 h-12 rounded-full bg-[#10b981]/10 flex flex-shrink-0 items-center justify-center">
                  <span className="material-symbols-outlined text-[#10b981]">calendar_month</span>
                </div>
                <div className="flex-1">
                  <div className="flex justify-between items-end mb-xs">
                    <span className="font-headline-sm text-headline-sm text-on-surface">Peak Harvest Window</span>
                    <span className="font-label-md text-label-md text-on-surface-variant">March - July</span>
                  </div>
                  <div className="w-full h-3 bg-surface-variant rounded-full overflow-hidden flex">
                    <div className="h-full bg-transparent w-2/12" />
                    <div className="h-full bg-[#10b981] w-5/12 relative group cursor-pointer transition-all hover:bg-[#059669]">
                      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent animate-[shimmer_2s_infinite]" />
                    </div>
                    <div className="h-full bg-transparent w-5/12" />
                  </div>
                </div>
              </div>
            </div>

            <div className="xl:col-span-4 flex flex-col gap-gutter">
              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm flex-1 flex flex-col">
                <div className="flex justify-between items-center mb-md">
                  <h3 className="font-headline-sm text-headline-sm text-on-surface">Prod. Transformation</h3>
                  <button
                    type="button"
                    title="Refresh batches"
                    onClick={fetchDashboardData}
                    className="text-primary hover:text-on-primary-fixed-variant transition-colors"
                  >
                    <span className="material-symbols-outlined">open_in_new</span>
                  </button>
                </div>
                <div className="overflow-x-auto flex-1">
                  {productBatches.length === 0 ? (
                    <p className="font-body-sm text-body-sm text-on-surface-variant py-md">
                      No product batches returned from /api/product-batches.
                    </p>
                  ) : (
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="border-b border-outline-variant/30 text-on-surface-variant font-label-md text-label-md uppercase">
                          <th className="pb-sm font-semibold">Batch ID</th>
                          <th className="pb-sm font-semibold">Stock</th>
                          <th className="pb-sm font-semibold text-right">Status</th>
                        </tr>
                      </thead>
                      <tbody className="font-body-sm text-body-sm text-on-surface divide-y divide-outline-variant/20">
                        {productBatches.map((batch) => (
                          <tr key={batch.batchId} className="hover:bg-surface-container/30 transition-colors group">
                            <td className="py-md font-mono-label">{batch.batchCode || `#BT-${batch.batchId}`}</td>
                            <td className="py-md">
                              {batch.stockT != null ? `${Number(batch.stockT).toLocaleString()} t` : '—'}
                            </td>
                            <td className="py-md text-right">{getStatusBadge(batch.status)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>

              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-error/5 rounded-bl-full -z-10" />
                <h3 className="font-headline-sm text-headline-sm text-on-surface mb-md flex items-center gap-sm">
                  <span className="material-symbols-outlined text-error">warning</span>
                  System Alerts
                </h3>
                <div className="flex flex-col gap-md">
                  {displayAlerts.length === 0 ? (
                    <p className="font-body-sm text-body-sm text-on-surface-variant">No alerts from /api/alerts.</p>
                  ) : (
                    displayAlerts.slice(0, 5).map((alert) => (
                      <div
                        key={alert.alertId}
                        role="button"
                        tabIndex={0}
                        onClick={() => setPendingDeleteAlert(alert)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') setPendingDeleteAlert(alert);
                        }}
                        className={`flex items-start gap-md p-sm -mx-sm rounded-lg hover:bg-surface-container/50 transition-colors cursor-pointer border-l-2 ${
                          (alert.severity || '').toUpperCase() === 'HIGH' ? 'border-error' : 'border-secondary'
                        }`}
                        title="Click to delete this alert"
                      >
                        <div
                          className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 mt-xs ${
                            (alert.severity || '').toUpperCase() === 'HIGH'
                              ? 'bg-error/10 text-error'
                              : 'bg-secondary/10 text-secondary'
                          }`}
                        >
                          <span className="material-symbols-outlined text-[16px]">
                            {(alert.severity || '').toUpperCase() === 'HIGH' ? 'priority_high' : 'schedule'}
                          </span>
                        </div>
                        <div className="min-w-0 flex-1">
                          <h4 className="font-label-md text-label-md text-on-surface">
                            {alert.alertCode || `Alert #${alert.alertId}`}
                          </h4>
                          <p className="font-body-sm text-body-sm text-on-surface-variant mt-xs line-clamp-2">
                            {alert.message}
                          </p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
                <Link
                  to="/demand-alerts"
                  className="w-full mt-md py-sm text-center text-primary font-label-md text-label-md hover:bg-primary/5 rounded-lg transition-colors block"
                >
                  VIEW ALL ALERTS
                </Link>
              </div>
            </div>
          </div>

          {/* Bottom Feature Shortcuts */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter mt-auto">
            <button
              type="button"
              onClick={() => navigate('/forecasting')}
              className="bg-surface-container-low rounded-xl p-md flex items-center gap-md cursor-pointer hover:bg-surface-container transition-colors group shadow-sm hover:shadow-md text-left"
            >
              <div className="w-12 h-12 rounded-lg bg-surface-container-highest flex items-center justify-center group-hover:bg-primary/10 transition-colors">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">history</span>
              </div>
              <div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">Historical Data</h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">Browse past performance</p>
              </div>
              <span className="material-symbols-outlined ml-auto text-on-surface-variant opacity-0 group-hover:opacity-100 transform translate-x-2 group-hover:translate-x-0 transition-all">
                chevron_right
              </span>
            </button>
            <button
              type="button"
              onClick={() => navigate('/forecasting')}
              className="bg-surface-container-low rounded-xl p-md flex items-center gap-md cursor-pointer hover:bg-surface-container transition-colors group shadow-sm hover:shadow-md text-left"
            >
              <div className="w-12 h-12 rounded-lg bg-surface-container-highest flex items-center justify-center group-hover:bg-primary/10 transition-colors">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">settings_suggest</span>
              </div>
              <div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">Model Settings</h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">Configure AI parameters</p>
              </div>
              <span className="material-symbols-outlined ml-auto text-on-surface-variant opacity-0 group-hover:opacity-100 transform translate-x-2 group-hover:translate-x-0 transition-all">
                chevron_right
              </span>
            </button>
            <button
              type="button"
              onClick={() => navigate('/forecasting')}
              className="bg-surface-container-low rounded-xl p-md flex items-center gap-md cursor-pointer hover:bg-surface-container transition-colors group shadow-sm hover:shadow-md text-left"
            >
              <div className="w-12 h-12 rounded-lg bg-surface-container-highest flex items-center justify-center group-hover:bg-primary/10 transition-colors">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">event_note</span>
              </div>
              <div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">Seasonal Planning</h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">Manage harvest cycles</p>
              </div>
              <span className="material-symbols-outlined ml-auto text-on-surface-variant opacity-0 group-hover:opacity-100 transform translate-x-2 group-hover:translate-x-0 transition-all">
                chevron_right
              </span>
            </button>
          </div>
        </>
      )}

      {/* NEW FORECAST modal — POST /api/demand-forecasts */}
      {showForecastModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-xl relative">
            <button
              type="button"
              onClick={() => setShowForecastModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">New Forecast</h3>
            <form onSubmit={handleCreateForecast} className="flex flex-col gap-md">
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Product</label>
                <select
                  required
                  value={forecastForm.productId}
                  onChange={(e) => setForecastForm((f) => ({ ...f, productId: e.target.value }))}
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm text-body-sm"
                >
                  <option value="">Select product…</option>
                  {products.map((p) => (
                    <option key={p.productId} value={p.productId}>
                      {p.name} {p.variety ? `(${p.variety})` : ''}
                    </option>
                  ))}
                </select>
                {products.length === 0 && (
                  <p className="font-body-sm text-body-sm text-secondary mt-xs">
                    No products loaded — ensure /api/products has data (productId is required).
                  </p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Month</label>
                  <input
                    required
                    type="date"
                    value={forecastForm.forecastMonth}
                    onChange={(e) => setForecastForm((f) => ({ ...f, forecastMonth: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Scenario</label>
                  <select
                    required
                    value={forecastForm.scenario}
                    onChange={(e) => setForecastForm((f) => ({ ...f, scenario: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    <option value="base">base</option>
                    <option value="optimistic">optimistic</option>
                    <option value="pessimistic">pessimistic</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Volume (t)</label>
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={forecastForm.forecastedVolumeT}
                    onChange={(e) => setForecastForm((f) => ({ ...f, forecastedVolumeT: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Lower</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={forecastForm.lowerBoundT}
                    onChange={(e) => setForecastForm((f) => ({ ...f, lowerBoundT: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Upper</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={forecastForm.upperBoundT}
                    onChange={(e) => setForecastForm((f) => ({ ...f, upperBoundT: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Capacity Risk</label>
                <select
                  value={forecastForm.capacityRisk}
                  onChange={(e) => setForecastForm((f) => ({ ...f, capacityRisk: e.target.value }))}
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                >
                  <option value="low">low</option>
                  <option value="medium">medium</option>
                  <option value="high">high</option>
                </select>
              </div>
              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowForecastModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingForecast}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs"
                >
                  {savingForecast ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Saving…
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Create
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete alert confirmation — DELETE /api/alerts/{id} */}
      {pendingDeleteAlert && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete alert?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/alerts/{pendingDeleteAlert.alertId}</code> for{' '}
              <strong>{pendingDeleteAlert.alertCode || `#${pendingDeleteAlert.alertId}`}</strong>. This cannot be undone.
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg line-clamp-3">
              {pendingDeleteAlert.message}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteAlert(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingAlert}
                onClick={confirmDeleteAlert}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60"
              >
                {deletingAlert ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
