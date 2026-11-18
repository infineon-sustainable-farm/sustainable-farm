import { useMemo, useState } from 'react';
import { BrainCircuit, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Button, ConfirmDialog, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import AccuracyTable from '../components/forecasting/AccuracyTable';
import ForecastFormModal from '../components/forecasting/ForecastFormModal';
import ModelParametersCard from '../components/forecasting/ModelParametersCard';
import ModelStatusCard from '../components/forecasting/ModelStatusCard';
import MonthlyForecastTable from '../components/forecasting/MonthlyForecastTable';
import ProjectionChart from '../components/forecasting/ProjectionChart';
import { useForecastFormat } from '../components/forecasting/forecastFormat';
import {
  COLD_START_STATUS,
  DEFAULT_CROP_VARIETY,
  actualSalesByProductMonth,
  buildProjectionData,
  computeMomGrowth,
  downloadForecastsCsv,
  emptyForecastForm,
  forecastAccuracy,
  getErrorMessage,
  scenarioTotals,
} from '../components/forecasting/forecastingData';
import { useForecastingData } from '../components/forecasting/useForecastingData';

const SUCCESS_DURATION = 4000;

function optionalNumber(value) {
  return value !== '' ? Number(value) : null;
}

export default function ForecastingPage() {
  const { t } = useTranslation('salesmarketing');
  const format = useForecastFormat();
  const { loading, apiError, forecasts, modelRuns, products, orders, orderItems, refetch, params } =
    useForecastingData();
  const { selectedProductId, region, confidenceInterval, weatherPatterns, commodityPrices, geopoliticalIndex } =
    params;

  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [scenarioFilter, setScenarioFilter] = useState('all');
  const [retraining, setRetraining] = useState(false);
  const [showForecastModal, setShowForecastModal] = useState(false);
  const [editingForecast, setEditingForecast] = useState(null);
  const [forecastForm, setForecastForm] = useState(emptyForecastForm);
  const [savingForecast, setSavingForecast] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const regions = [...new Set(modelRuns.map((run) => run.region).filter(Boolean))];

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), SUCCESS_DURATION);
  };
  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
  };

  const selectedProduct = products.find((p) => String(p.productId) === String(selectedProductId));
  const cropVariety = selectedProduct?.variety || selectedProduct?.name || null;
  const latestRun = [...modelRuns].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))[0];

  const accuracyRows = useMemo(
    () => forecastAccuracy(forecasts, actualSalesByProductMonth(orders, orderItems)),
    [forecasts, orders, orderItems]
  );

  // Filter: product + scenario
  let filtered = forecasts;
  if (selectedProductId) {
    filtered = filtered.filter((f) => f.productId == null || String(f.productId) === String(selectedProductId));
  }
  if (scenarioFilter !== 'all') {
    filtered = filtered.filter((f) => (f.scenario || '').toLowerCase() === scenarioFilter);
  }

  const sortedTable = computeMomGrowth(
    [...filtered].sort((a, b) => new Date(a.forecastMonth) - new Date(b.forecastMonth))
  );
  const totals = scenarioTotals(forecasts);
  const chart = buildProjectionData(
    scenarioFilter === 'all'
      ? filtered.filter((f) => (f.scenario || 'base').toLowerCase().includes('base') || !f.scenario)
      : filtered
  );

  const handleRetrain = async () => {
    setRetraining(true);
    setActionError(null);
    try {
      await api.createForecastModelRun({
        cropVariety: cropVariety ?? DEFAULT_CROP_VARIETY,
        region: region || null,
        confidenceIntervalPct: confidenceInterval,
        useWeatherData: weatherPatterns,
        useCommodityPrices: commodityPrices,
        useGeopoliticalIndex: geopoliticalIndex,
        modelStatus: COLD_START_STATUS,
      });
      flashSuccess(t('forecasting.retrained'));
      await refetch();
    } catch (err) {
      flashError(getErrorMessage(err, t('forecasting.retrainFailed')));
    } finally {
      setRetraining(false);
    }
  };

  const handleExportCsv = () => {
    if (sortedTable.length === 0) {
      flashError(t('forecasting.exportEmpty'));
      return;
    }
    downloadForecastsCsv(sortedTable);
    flashSuccess(t('forecasting.exported'));
  };

  const openCreateForecast = () => {
    setEditingForecast(null);
    setForecastForm({
      ...emptyForecastForm,
      productId: selectedProductId || (products[0] ? String(products[0].productId) : ''),
      forecastMonth: new Date().toISOString().slice(0, 7) + '-01',
    });
    setShowForecastModal(true);
  };

  const openEditForecast = (row) => {
    setEditingForecast(row);
    setForecastForm({
      productId: row.productId != null ? String(row.productId) : selectedProductId,
      forecastMonth: row.forecastMonth ? String(row.forecastMonth).slice(0, 10) : '',
      scenario: row.scenario || 'base',
      forecastedVolumeT: row.forecastedVolumeT != null ? String(row.forecastedVolumeT) : '',
      lowerBoundT: row.lowerBoundT != null ? String(row.lowerBoundT) : '',
      upperBoundT: row.upperBoundT != null ? String(row.upperBoundT) : '',
      capacityRisk: row.capacityRisk || 'low',
      modelVersion: row.modelVersion || 'v1',
    });
    setShowForecastModal(true);
  };

  const handleSaveForecast = async (e) => {
    e.preventDefault();
    setSavingForecast(true);
    setActionError(null);
    const payload = {
      productId: forecastForm.productId ? Number(forecastForm.productId) : null,
      forecastMonth: forecastForm.forecastMonth,
      scenario: forecastForm.scenario,
      forecastedVolumeT: Number(forecastForm.forecastedVolumeT),
      lowerBoundT: optionalNumber(forecastForm.lowerBoundT),
      upperBoundT: optionalNumber(forecastForm.upperBoundT),
      capacityRisk: forecastForm.capacityRisk || null,
      isActual: editingForecast?.isActual ?? false,
      modelVersion: forecastForm.modelVersion || null,
    };
    try {
      if (editingForecast) {
        await api.updateDemandForecast(editingForecast.forecastId, payload);
        flashSuccess(t('forecasting.updated', { id: editingForecast.forecastId }));
      } else {
        await api.createDemandForecast(payload);
        flashSuccess(t('forecasting.created'));
      }
      setShowForecastModal(false);
      setEditingForecast(null);
      await refetch();
    } catch (err) {
      flashError(
        getErrorMessage(err, editingForecast ? t('forecasting.updateFailed') : t('forecasting.createFailed'))
      );
    } finally {
      setSavingForecast(false);
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await api.deleteDemandForecast(pendingDelete.forecastId);
      flashSuccess(t('forecasting.deleted', { id: pendingDelete.forecastId }));
      setPendingDelete(null);
      await refetch();
    } catch (err) {
      flashError(getErrorMessage(err, t('forecasting.deleteFailed')));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      {apiError && (
        <Notice tone="offline" title={t('forecasting.apiNotice')} onRetry={refetch}>
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
        title={t('forecasting.title')}
        subtitle={t('forecasting.subtitle')}
        actions={
          <>
            <span className="flex items-center gap-2 rounded-pill bg-surface-muted px-3 py-1 text-sm">
              <span
                aria-hidden="true"
                className={`h-2 w-2 rounded-pill ${apiError ? 'bg-warning-fg' : 'bg-primary'}`}
              />
              {t('forecasting.modelChip', { status: latestRun?.modelStatus || t('forecasting.coldStart') })}
            </span>
            <Button variant="secondary" icon={Plus} onClick={openCreateForecast}>
              {t('forecasting.add')}
            </Button>
            <Button icon={BrainCircuit} loading={retraining} onClick={handleRetrain}>
              {t('forecasting.retrain')}
            </Button>
          </>
        }
      />

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">
            {t('forecasting.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
            <Skeleton radius="card" className="h-96" />
            <Skeleton radius="card" className="h-96 lg:col-span-3" />
          </div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
            <div className="flex flex-col gap-4">
              <ModelParametersCard
                products={products}
                regions={regions}
                params={params}
                scenarioFilter={scenarioFilter}
                onScenarioFilterChange={setScenarioFilter}
              />
              <ModelStatusCard latestRun={latestRun} runCount={modelRuns.length} totals={totals} />
            </div>
            <ProjectionChart
              className="lg:col-span-3"
              chart={chart}
              rowCount={filtered.length}
              confidenceInterval={confidenceInterval}
              onAddForecast={openCreateForecast}
            />
          </div>
          <AccuracyTable rows={accuracyRows} />
          <MonthlyForecastTable
            rows={sortedTable}
            confidenceInterval={confidenceInterval}
            onExport={handleExportCsv}
            onEdit={openEditForecast}
            onDelete={setPendingDelete}
          />
        </>
      )}

      <ForecastFormModal
        open={showForecastModal}
        editing={editingForecast}
        form={forecastForm}
        setForm={setForecastForm}
        products={products}
        saving={savingForecast}
        onClose={() => setShowForecastModal(false)}
        onSubmit={handleSaveForecast}
      />

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        onClose={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title={t('forecasting.deleteTitle')}
        confirmLabel={deleting ? t('forecasting.deleting') : t('common:actions.delete')}
        message={
          pendingDelete &&
          t('forecasting.deleteMessage', {
            month: format.month(pendingDelete.forecastMonth),
            scenario: pendingDelete.scenario || t('forecasting.scenarioFallback'),
          })
        }
      />
    </div>
  );
}
