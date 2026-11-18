import { useCallback, useEffect, useState } from 'react';
import { CircleCheck, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Button, ConfirmDialog, EmptyState, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import AlertCard from '../components/alerts/AlertCard';
import AlertControls from '../components/alerts/AlertControls';
import AlertFormModal from '../components/alerts/AlertFormModal';
import AlertKpis from '../components/alerts/AlertKpis';
import { AnomalyHotspots, ResolutionActivity } from '../components/alerts/AlertSidebar';
import {
  PAGE_SIZE,
  emptyAlertForm,
  getErrorMessage,
  isActiveStatus,
  isResolvedStatus,
  severityBucket,
  useAlertText,
} from '../components/alerts/alertUtils';

function latestPrices(pricingHistory) {
  return pricingHistory.reduce((acc, pricing) => {
    const productId = Number(pricing.productId);
    const price = Number(pricing.priceEurPerKg);
    if (!productId || !Number.isFinite(price) || price <= 0) return acc;
    const existing = acc[productId];
    if (!existing || String(pricing.priceDate) > String(existing.priceDate)) {
      acc[productId] = { priceEurPerKg: price, priceDate: pricing.priceDate };
    }
    return acc;
  }, {});
}

export default function DemandAlertsPage() {
  const { t } = useTranslation('salesmarketing');
  const text = useAlertText();
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

  const [showModal, setShowModal] = useState(false);
  const [editingAlert, setEditingAlert] = useState(null);
  const [alertForm, setAlertForm] = useState(emptyAlertForm);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [resolvingId, setResolvingId] = useState(null);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
  };

  const fetchAlertsData = useCallback(async () => {
    try {
      const [alertRes, forecastRes, pricingRes] = await Promise.allSettled([
        api.getAlerts(),
        api.getDemandForecasts(),
        api.getPricingHistory(),
      ]);
      setApiError(null);
      const failures = [];

      if (alertRes.status === 'fulfilled') {
        setAlerts(Array.isArray(alertRes.value) ? alertRes.value : []);
      } else {
        setAlerts([]);
        failures.push(t('alerts.failureAlerts', { message: getErrorMessage(alertRes.reason, t('alerts.loadAlertsFailed')) }));
      }

      if (forecastRes.status === 'fulfilled') {
        setForecasts(Array.isArray(forecastRes.value) ? forecastRes.value : []);
      } else {
        setForecasts([]);
        failures.push(
          t('alerts.failureForecasts', { message: getErrorMessage(forecastRes.reason, t('alerts.loadForecastsFailed')) })
        );
      }

      if (pricingRes.status === 'fulfilled') {
        setPricingHistory(Array.isArray(pricingRes.value) ? pricingRes.value : []);
      } else {
        setPricingHistory([]);
        failures.push(t('alerts.failurePricing', { message: getErrorMessage(pricingRes.reason, t('alerts.loadPricingFailed')) }));
      }

      if (failures.length > 0) setApiError(failures.join(' · '));
    } catch (err) {
      setApiError(getErrorMessage(err, t('alerts.loadDataFailed')));
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchAlertsData);
  }, [fetchAlertsData]);

  // KPIs from the base case forecasts (all forecasts when none is a base case).
  const baseForecasts = forecasts.filter((f) => {
    const s = (f.scenario || '').toLowerCase();
    return !s || s.includes('base');
  });
  const kpiSource = baseForecasts.length > 0 ? baseForecasts : forecasts;
  const totalBase = kpiSource.reduce((s, f) => s + Number(f.forecastedVolumeT || 0), 0);
  const latestPriceByProduct = latestPrices(pricingHistory);
  const revenuePotentialEur = kpiSource.reduce((total, forecast) => {
    const pricing = latestPriceByProduct[Number(forecast.productId)];
    const volumeT = Number(forecast.forecastedVolumeT || 0);
    if (!pricing || !Number.isFinite(volumeT) || volumeT <= 0) return total;
    return total + volumeT * 1000 * pricing.priceEurPerKg;
  }, 0);

  const activeAlerts = alerts.filter((a) => isActiveStatus(a.status));
  const countBucket = (bucket) => activeAlerts.filter((a) => severityBucket(a) === bucket).length;
  const counts = { active: activeAlerts.length, high: countBucket('high'), medium: countBucket('medium'), data: countBucket('data') };

  const filteredAlerts = alerts.filter((alert) => {
    const q = searchQuery.trim().toLowerCase();
    const haystack = [
      text.title(alert),
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
    if (['high', 'medium', 'data'].includes(selectedFilter)) {
      return severityBucket(alert) === selectedFilter && isActiveStatus(alert.status);
    }
    if (selectedFilter === 'resolved') return isResolvedStatus(alert.status);
    return true;
  });

  const visibleAlerts = filteredAlerts.slice(0, visibleCount);
  const hasMore = visibleCount < filteredAlerts.length;

  const regionCounts = {};
  activeAlerts.forEach((a) => {
    const key = a.region || t('alerts.unknownRegion');
    regionCounts[key] = (regionCounts[key] || 0) + 1;
  });
  const hotspotRows = Object.entries(regionCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  const activityItems = [...alerts]
    .sort((a, b) => new Date(b.resolvedAt || b.createdAt || 0) - new Date(a.resolvedAt || a.createdAt || 0))
    .slice(0, 5)
    .map((a) => {
      const resolved = a.resolvedAt || (a.status || '').toUpperCase() === 'RESOLVED';
      const code = a.alertCode || a.alertId;
      const message = a.message || '';
      return {
        id: a.alertId,
        text: resolved
          ? a.region
            ? t('alerts.activityResolvedRegion', { code, region: a.region })
            : t('alerts.activityResolved', { code })
          : t('alerts.activityFlagged', { code, message: `${message.slice(0, 80)}${message.length > 80 ? '…' : ''}` }),
        when: text.relativeTime(a.resolvedAt || a.createdAt),
        primary: !resolved,
      };
    });

  const changeFilter = (filter) => {
    setSelectedFilter(filter);
    setVisibleCount(PAGE_SIZE);
  };

  const openCreate = () => {
    setEditingAlert(null);
    setAlertForm({ ...emptyAlertForm, alertCode: `ALT-${Date.now().toString().slice(-4)}` });
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
        (alertForm.status || '').toLowerCase() === 'resolved' ? editingAlert?.resolvedAt || new Date().toISOString() : null,
    };
    try {
      if (editingAlert) {
        await api.updateAlert(editingAlert.alertId, payload);
        flashSuccess(t('alerts.updated', { code: payload.alertCode || editingAlert.alertId }));
      } else {
        await api.createAlert(payload);
        flashSuccess(t('alerts.created'));
      }
      setShowModal(false);
      setEditingAlert(null);
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingAlert ? t('alerts.updateFailed') : t('alerts.createFailed')));
    } finally {
      setSaving(false);
    }
  };

  const alertName = (alert) => alert.alertCode || t('alerts.numbered', { id: alert.alertId });

  const handleResolve = async (alert) => {
    if (resolvingId === alert.alertId) return;
    setResolvingId(alert.alertId);
    setActionError(null);
    try {
      await api.updateAlert(alert.alertId, { ...alert, status: 'resolved', resolvedAt: new Date().toISOString() });
      flashSuccess(t('alerts.resolved', { name: alertName(alert) }));
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, t('alerts.resolveFailed')));
    } finally {
      setResolvingId(null);
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await api.deleteAlert(pendingDelete.alertId);
      flashSuccess(t('alerts.deleted', { name: alertName(pendingDelete) }));
      setPendingDelete(null);
      await fetchAlertsData();
    } catch (err) {
      flashError(getErrorMessage(err, t('alerts.deleteFailed')));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      {apiError && (
        <Notice tone="offline" title={t('alerts.apiNotice')} onRetry={fetchAlertsData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      <PageHeader
        title={t('alerts.title')}
        subtitle={t('alerts.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('alerts.new')}
          </Button>
        }
      />

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">
            {t('alerts.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {[0, 1, 2].map((index) => (
              <Skeleton key={index} radius="card" className="h-36" />
            ))}
          </div>
        </div>
      ) : (
        <>
          <AlertKpis
            hasForecasts={kpiSource.length > 0}
            totalBase={totalBase}
            activeCount={activeAlerts.length}
            highActiveCount={counts.high}
            revenuePotentialEur={revenuePotentialEur}
          />

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
            <div className="flex flex-col gap-6 lg:col-span-8">
              <AlertControls
                search={searchQuery}
                onSearchChange={(value) => {
                  setSearchQuery(value);
                  setVisibleCount(PAGE_SIZE);
                }}
                filter={selectedFilter}
                onFilterChange={changeFilter}
                counts={counts}
              />

              {visibleAlerts.length === 0 ? (
                <div className="rounded-card border border-border-subtle bg-surface-raised">
                  <EmptyState
                    icon={CircleCheck}
                    title={t('alerts.emptyTitle')}
                    message={alerts.length === 0 ? t('alerts.emptyNone') : t('alerts.emptyFilter')}
                  />
                </div>
              ) : (
                <ul className="flex flex-col gap-4">
                  {visibleAlerts.map((alertItem) => (
                    <AlertCard
                      key={alertItem.alertId}
                      alert={alertItem}
                      resolving={resolvingId === alertItem.alertId}
                      onResolve={handleResolve}
                      onEdit={openEdit}
                      onDismiss={setPendingDelete}
                    />
                  ))}
                </ul>
              )}

              {hasMore && (
                <div className="flex justify-center">
                  <Button variant="ghost" onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}>
                    {t('alerts.loadMore', { count: filteredAlerts.length - visibleCount })}
                  </Button>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-6 lg:col-span-4">
              <AnomalyHotspots rows={hotspotRows} />
              <ResolutionActivity items={activityItems} />
            </div>
          </div>
        </>
      )}

      <AlertFormModal
        open={showModal}
        editing={editingAlert}
        form={alertForm}
        setForm={setAlertForm}
        saving={saving}
        onClose={() => setShowModal(false)}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        onClose={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title={t('alerts.dismissTitle')}
        confirmLabel={deleting ? t('alerts.deleting') : t('common:actions.delete')}
        message={
          pendingDelete && (
            <>
              <p>
                {t('alerts.dismissMessage', {
                  code: pendingDelete.alertCode || t('alerts.hashId', { id: pendingDelete.alertId }),
                })}
              </p>
              <p className="mt-2 line-clamp-3">{pendingDelete.message}</p>
            </>
          )
        }
      />
    </div>
  );
}
