import { useCallback, useEffect, useState } from 'react';
import { ChartNoAxesCombined, Download, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Button, ConfirmDialog, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import AlertsCard from '../components/dashboard/AlertsCard';
import BatchesCard from '../components/dashboard/BatchesCard';
import DashboardKpis from '../components/dashboard/DashboardKpis';
import ForecastChart from '../components/dashboard/ForecastChart';
import ForecastFormModal from '../components/dashboard/ForecastFormModal';
import HarvestWindow from '../components/dashboard/HarvestWindow';
import Shortcuts from '../components/dashboard/Shortcuts';
import { emptyForecastForm, forecastsToCsv, getErrorMessage } from '../components/dashboard/dashboardData';

const LOADS = [
  { key: 'forecasts', call: 'getDemandForecasts' },
  { key: 'alerts', call: 'getAlerts' },
  { key: 'batches', call: 'getProductBatches' },
  { key: 'products', call: 'getProducts' },
  { key: 'pricing', call: 'getPricingHistory' },
  { key: 'customers', call: 'getCustomers' },
  { key: 'orders', call: 'getOrders' },
];

export default function DashboardPage() {
  const { t } = useTranslation('salesmarketing');
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

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
  };

  const fetchDashboardData = useCallback(async () => {
    const setters = {
      forecasts: setForecasts,
      alerts: setAlerts,
      batches: setProductBatches,
      products: setProducts,
      pricing: setPricingHistory,
      customers: setCustomers,
      orders: setOrders,
    };
    const requestFailed = t('dashboard.requestFailed');
    try {
      const results = await Promise.allSettled(LOADS.map(({ call }) => api[call]()));
      setApiError(null);

      const failures = [];
      results.forEach((result, index) => {
        const { key } = LOADS[index];
        if (result.status === 'fulfilled') {
          setters[key](Array.isArray(result.value) ? result.value : []);
        } else {
          setters[key]([]);
          failures.push(t(`dashboard.failure.${key}`, { message: getErrorMessage(result.reason, requestFailed) }));
        }
      });

      if (failures.length === 5) {
        setApiError(failures.join(' · ') || t('dashboard.loadFailed'));
      } else if (failures.length > 0) {
        setApiError(t('dashboard.partialLoad', { failures: failures.join(' · ') }));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('dashboard.loadDataFailed')));
      setForecasts([]);
      setAlerts([]);
      setProductBatches([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchDashboardData);
  }, [fetchDashboardData]);

  // ---- Derived KPIs from real forecast rows ----
  const baseForecasts = forecasts.filter((f) => {
    const s = (f.scenario || '').toLowerCase();
    return !s || s.includes('base');
  });
  const kpiSource = baseForecasts.length > 0 ? baseForecasts : forecasts;
  const forecastProductId = kpiSource.find((f) => f.productId != null)?.productId;

  const latestPricing = pricingHistory
    .filter((p) => p.productId === forecastProductId)
    .sort((a, b) => new Date(b.priceDate).getTime() - new Date(a.priceDate).getTime())[0];

  const currentPriceEurPerKg = latestPricing?.priceEurPerKg != null ? Number(latestPricing.priceEurPerKg) : null;

  const totalBase = kpiSource.reduce((sum, f) => sum + Number(f.forecastedVolumeT || 0), 0);
  const totalLower = kpiSource.reduce((sum, f) => sum + Number(f.lowerBoundT ?? f.forecastedVolumeT ?? 0), 0);
  const totalUpper = kpiSource.reduce((sum, f) => sum + Number(f.upperBoundT ?? f.forecastedVolumeT ?? 0), 0);

  const activeAlerts = alerts.filter((a) => {
    const s = (a.status || 'ACTIVE').toUpperCase();
    return s === 'ACTIVE' || s === 'OPEN' || s === 'NEW';
  });
  const displayAlerts = activeAlerts.length > 0 ? activeAlerts : alerts;

  const germanyDemandT = customers
    .filter((customer) => String(customer.country || '').toLowerCase() === 'germany')
    .reduce((total, customer) => {
      const customerId = Number(customer.customerId);
      const customerVolumeKg = orders
        .filter((order) => Number(order.customerId) === customerId)
        .reduce((sum, order) => sum + Number(order.totalVolumeKg ?? 0), 0);
      return total + customerVolumeKg / 1000;
    }, 0);

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
      flashSuccess(t('dashboard.forecastCreated'));
      await fetchDashboardData();
    } catch (err) {
      flashError(getErrorMessage(err, t('dashboard.forecastCreateFailed')));
    } finally {
      setSavingForecast(false);
    }
  };

  const handleExportReport = () => {
    if (forecasts.length === 0) {
      flashError(t('dashboard.exportEmpty'));
      return;
    }
    const blob = new Blob([forecastsToCsv(forecasts)], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `demand-forecasts-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    flashSuccess(t('dashboard.exportDone'));
  };

  const confirmDeleteAlert = async () => {
    if (!pendingDeleteAlert) return;
    setDeletingAlert(true);
    try {
      await api.deleteAlert(pendingDeleteAlert.alertId);
      setPendingDeleteAlert(null);
      flashSuccess(t('dashboard.alertDeleted', { code: pendingDeleteAlert.alertCode || pendingDeleteAlert.alertId }));
      await fetchDashboardData();
    } catch (err) {
      flashError(getErrorMessage(err, t('dashboard.alertDeleteFailed')));
    } finally {
      setDeletingAlert(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      {apiError && (
        <Notice tone="offline" title={t('dashboard.apiNotice')} onRetry={fetchDashboardData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" title={t('dashboard.actionFailed')} onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      <div>
        <p className="mb-1 flex items-center gap-1 text-xs font-medium text-text-muted">
          <ChartNoAxesCombined size={16} aria-hidden="true" />
          {t('dashboard.eyebrow')}
        </p>
        <PageHeader
          title={t('dashboard.title')}
          actions={
            <>
              <Button variant="secondary" icon={Download} onClick={handleExportReport}>
                {t('dashboard.export')}
              </Button>
              <Button icon={Plus} onClick={openNewForecast}>
                {t('dashboard.newForecast')}
              </Button>
            </>
          }
        />
      </div>

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">
            {t('dashboard.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
            {[0, 1, 2, 3].map((index) => (
              <Skeleton key={index} radius="card" className="h-40" />
            ))}
          </div>
          <Skeleton radius="card" className="h-96" />
        </div>
      ) : (
        <>
          <DashboardKpis
            hasForecasts={kpiSource.length > 0}
            totalBase={totalBase}
            totalLower={totalLower}
            totalUpper={totalUpper}
            germanyDemandT={germanyDemandT}
            alertCount={displayAlerts.length}
            degraded={Boolean(apiError)}
            priceEurPerKg={currentPriceEurPerKg}
          />

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-12">
            <div className="flex flex-col gap-4 xl:col-span-8">
              <ForecastChart
                forecasts={kpiSource.length > 0 ? kpiSource : forecasts}
                rowCount={forecasts.length}
                onCreate={openNewForecast}
              />
              <HarvestWindow />
            </div>
            <div className="flex flex-col gap-4 xl:col-span-4">
              <BatchesCard className="flex-1" batches={productBatches} onRefresh={fetchDashboardData} />
              <AlertsCard alerts={displayAlerts} onSelect={setPendingDeleteAlert} />
            </div>
          </div>

          <Shortcuts />
        </>
      )}

      <ForecastFormModal
        open={showForecastModal}
        form={forecastForm}
        setForm={setForecastForm}
        products={products}
        saving={savingForecast}
        onClose={() => setShowForecastModal(false)}
        onSubmit={handleCreateForecast}
      />

      <ConfirmDialog
        open={Boolean(pendingDeleteAlert)}
        onClose={() => setPendingDeleteAlert(null)}
        onConfirm={confirmDeleteAlert}
        loading={deletingAlert}
        title={t('dashboard.deleteAlertTitle')}
        confirmLabel={deletingAlert ? t('dashboard.deleting') : t('common:actions.delete')}
        message={
          pendingDeleteAlert && (
            <>
              <p>
                {t('dashboard.deleteAlertMessage', {
                  code: pendingDeleteAlert.alertCode || `#${pendingDeleteAlert.alertId}`,
                })}
              </p>
              <p className="mt-2 line-clamp-3">{pendingDeleteAlert.message}</p>
            </>
          )
        }
      />
    </div>
  );
}
