import React, { useEffect, useMemo, useState } from 'react';
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

function formatMonth(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (!isNaN(date.getTime())) {
    return date.toLocaleDateString('en-US', {
      month: 'short',
      year: 'numeric',
    });
  }
  return String(dateStr);
}

function formatTons(n) {
  if (n == null || Number.isNaN(Number(n))) return '—';
  return Number(n).toLocaleString(undefined, {
    maximumFractionDigits: 0,
  });
}

function computeMomGrowth(sortedRows) {
  return sortedRows.map((row, idx) => {
    if (idx === 0) return { ...row, momGrowth: '-' };

    const prev = Number(sortedRows[idx - 1].forecastedVolumeT || 0);
    const curr = Number(row.forecastedVolumeT || 0);

    if (prev === 0) {
      return {
        ...row,
        momGrowth: curr === 0 ? '0%' : '+âË†Å¾',
      };
    }

    const pct = ((curr - prev) / prev) * 100;
    const sign = pct > 0 ? '+' : '';

    return {
      ...row,
      momGrowth: `${sign}${pct.toFixed(1)}%`,
    };
  });
}

function buildProjectionChart(rows) {
  const points = [...rows]
    .filter((r) => r.forecastedVolumeT != null)
    .sort(
      (a, b) =>
        new Date(a.forecastMonth) - new Date(b.forecastMonth)
    )
    .slice(0, 10);

  if (points.length < 2) return null;

  const values = points.map((p) => Number(p.forecastedVolumeT));

  const lowers = points.map((p) =>
    Number(p.lowerBoundT ?? p.forecastedVolumeT)
  );

  const uppers = points.map((p) =>
    Number(p.upperBoundT ?? p.forecastedVolumeT)
  );

  const maxY = Math.max(...uppers, ...values, 1);

  const toY = (v) => 380 - (v / maxY) * 320;
  const toX = (i) => (i / (points.length - 1)) * 1000;

  const splitAt = Math.max(
    1,
    Math.floor(points.length * 0.55)
  );

  const line = (from, to) =>
    values
      .slice(from, to)
      .map((v, i) => {
        const idx = from + i;
        return `${i === 0 ? 'M' : 'L'}${toX(idx)},${toY(v)}`;
      })
      .join(' ');

  const bandXs = values
    .slice(splitAt)
    .map((_, i) => toX(i + splitAt));

  const bandPath = [
    ...uppers
      .slice(splitAt)
      .map(
        (v, i) =>
          `${i === 0 ? 'M' : 'L'}${bandXs[i]},${toY(v)}`
      ),
    ...lowers
      .slice(splitAt)
      .reverse()
      .map(
        (v, i) =>
          `L${bandXs[bandXs.length - 1 - i]},${toY(v)}`
      ),
    'Z',
  ].join(' ');

  return {
    labels: points.map((p, i) =>
      i === 0
        ? formatMonth(p.forecastMonth)
        : new Date(p.forecastMonth).toLocaleDateString(
            'en-US',
            { month: 'short' }
          )
    ),

    histLine: line(0, splitAt + 1),
    forecastLine: line(splitAt, points.length),
    bandPath,
    todayX: toX(splitAt),

    points: values.map((v, i) => ({
      x: toX(i),
      y: toY(v),
      historical: i < splitAt,
    })),
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

export default function ForecastingPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  const [forecasts, setForecasts] = useState([]);
  const [modelRuns, setModelRuns] = useState([]);
  const [products, setProducts] = useState([]);

  // Actual sales data
  const [orders, setOrders] = useState([]);
  const [orderItems, setOrderItems] = useState([]);

  // Model parameters
  const [selectedProductId, setSelectedProductId] = useState('');
  const [region, setRegion] = useState('');
  const [confidenceInterval, setConfidenceInterval] = useState(95);
  const [weatherPatterns, setWeatherPatterns] = useState(true);
  const [commodityPrices, setCommodityPrices] = useState(true);
  const [geopoliticalIndex, setGeopoliticalIndex] = useState(false);
  const [scenarioFilter, setScenarioFilter] = useState('all');

  const [showProductMenu, setShowProductMenu] = useState(false);
  const [showRegionMenu, setShowRegionMenu] = useState(false);
  const [retraining, setRetraining] = useState(false);

  const [showForecastModal, setShowForecastModal] = useState(false);
  const [editingForecast, setEditingForecast] = useState(null);
  const [forecastForm, setForecastForm] =
    useState(emptyForecastForm);
  const [savingForecast, setSavingForecast] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const REGIONS = [
    ...new Set(
      modelRuns.map((run) => run.region).filter(Boolean)
    ),
  ];

  useEffect(() => {
    fetchForecastingData();
  }, []);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);

    window.setTimeout(
      () => setActionSuccess(null),
      4000
    );
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
  };

  const fetchForecastingData = async () => {
    setLoading(true);
    setApiError(null);

    try {
      const [
        forecastRes,
        modelRunRes,
        productRes,
        orderRes,
        orderItemRes,
      ] = await Promise.allSettled([
        api.getDemandForecasts(),
        api.getForecastModelRuns(),
        api.getProducts(),
        api.getOrders(),
        api.getOrderItems(),
      ]);

      const failures = [];

      if (forecastRes.status === 'fulfilled') {
        setForecasts(
          Array.isArray(forecastRes.value.data)
            ? forecastRes.value.data
            : []
        );
      } else {
        setForecasts([]);

        failures.push(
          `Forecasts: ${getErrorMessage(
            forecastRes.reason
          )}`
        );
      }

      if (modelRunRes.status === 'fulfilled') {
        const runs = Array.isArray(modelRunRes.value.data)
          ? modelRunRes.value.data
          : [];

        setModelRuns(runs);

        const latest = [...runs].sort(
          (a, b) =>
            new Date(b.createdAt || 0) -
            new Date(a.createdAt || 0)
        )[0];

        if (latest) {
          if (latest.region) {
            setRegion(latest.region);
          }

          if (
            latest.confidenceIntervalPct != null
          ) {
            setConfidenceInterval(
              Number(latest.confidenceIntervalPct)
            );
          }

          if (latest.useWeatherData != null) {
            setWeatherPatterns(
              !!latest.useWeatherData
            );
          }

          if (
            latest.useCommodityPrices != null
          ) {
            setCommodityPrices(
              !!latest.useCommodityPrices
            );
          }

          if (
            latest.useGeopoliticalIndex != null
          ) {
            setGeopoliticalIndex(
              !!latest.useGeopoliticalIndex
            );
          }
        }
      } else {
        setModelRuns([]);

        failures.push(
          `Model runs: ${getErrorMessage(
            modelRunRes.reason
          )}`
        );
      }

      if (productRes.status === 'fulfilled') {
        const list = Array.isArray(
          productRes.value.data
        )
          ? productRes.value.data
          : [];

        setProducts(list);

        if (
          !selectedProductId &&
          list[0]?.productId
        ) {
          setSelectedProductId(
            String(list[0].productId)
          );
        }
      } else {
        setProducts([]);

        failures.push(
          `Products: ${getErrorMessage(
            productRes.reason
          )}`
        );
      }

      // Orders
      if (orderRes.status === 'fulfilled') {
        setOrders(
          Array.isArray(orderRes.value.data)
            ? orderRes.value.data
            : []
        );
      } else {
        setOrders([]);

        failures.push(
          `Orders: ${getErrorMessage(
            orderRes.reason
          )}`
        );
      }

      // Order items
      if (orderItemRes.status === 'fulfilled') {
        setOrderItems(
          Array.isArray(orderItemRes.value.data)
            ? orderItemRes.value.data
            : []
        );
      } else {
        setOrderItems([]);

        failures.push(
          `Order items: ${getErrorMessage(
            orderItemRes.reason
          )}`
        );
      }

      if (failures.length > 0) {
        setApiError(failures.join(' · '));
      }
    } catch (err) {
      setApiError(
        getErrorMessage(
          err,
          'Error connecting to http://localhost:8080/api'
        )
      );

      setForecasts([]);
      setModelRuns([]);
      setOrders([]);
      setOrderItems([]);
    } finally {
      setLoading(false);
    }
  };

  const selectedProduct = products.find(
    (p) =>
      String(p.productId) ===
      String(selectedProductId)
  );

  const cropVariety =
    selectedProduct?.variety ||
    selectedProduct?.name ||
    'Select product';

  const latestRun = [...modelRuns].sort(
    (a, b) =>
      new Date(b.createdAt || 0) -
      new Date(a.createdAt || 0)
  )[0];

  /*
   * Calculate actual sales by:
   * product + month
   *
   * Example:
   * product 1 + 2026-06 = 21.5 t
   * product 1 + 2026-07 = 25.2 t
   * product 1 + 2026-08 = 28.9 t
   */
  const actualSalesByProductMonth = useMemo(() => {
    const orderById = new Map(
      orders.map((order) => [
        Number(order.orderId),
        order,
      ])
    );

    const totals = new Map();

    orderItems.forEach((item) => {
      const order = orderById.get(
        Number(item.orderId)
      );

      if (
        !order ||
        !order.orderDate ||
        !item.productId
      ) {
        return;
      }

      const quantityKg = Number(
        item.quantityKg ?? 0
      );

      if (
        !Number.isFinite(quantityKg) ||
        quantityKg <= 0
      ) {
        return;
      }

      const month = String(
        order.orderDate
      ).slice(0, 7);

      const key = `${Number(item.productId)}-${month}`;

      totals.set(
        key,
        (totals.get(key) || 0) +
          quantityKg / 1000
      );
    });

    return totals;
  }, [orders, orderItems]);

  /*
   * Compare actual sales with base forecasts.
   *
   * If there is no actual for a forecast month:
   * accuracy = null
   *
   * This prevents us from inventing accuracy
   * for future forecast months.
   */
  const forecastAccuracyData = useMemo(() => {
    return forecasts
      .filter((forecast) => {
        const scenario = (
          forecast.scenario || ''
        ).toLowerCase();

        return scenario === 'base';
      })
      .map((forecast) => {
        const productId = Number(
          forecast.productId
        );

        const month = String(
          forecast.forecastMonth
        ).slice(0, 7);

        const key = `${productId}-${month}`;

        const actualVolumeT =
          actualSalesByProductMonth.get(key);

        const forecastVolumeT = Number(
          forecast.forecastedVolumeT ?? 0
        );

        if (
          actualVolumeT === undefined ||
          !Number.isFinite(forecastVolumeT) ||
          forecastVolumeT <= 0
        ) {
          return {
            ...forecast,
            actualVolumeT: null,
            accuracy: null,
          };
        }

        const absolutePercentageError =
          Math.abs(
            actualVolumeT - forecastVolumeT
          ) / forecastVolumeT;

        const accuracy = Math.max(
          0,
          (1 - absolutePercentageError) * 100
        );

        return {
          ...forecast,
          actualVolumeT,
          accuracy,
        };
      });
  }, [
    forecasts,
    actualSalesByProductMonth,
  ]);

  console.table(
  forecastAccuracyData.map((row) => ({
    month: row.forecastMonth,
    productId: row.productId,
    forecastT: row.forecastedVolumeT,
    actualT: row.actualVolumeT,
    accuracy: row.accuracy,
  }))
);

  // Filter: product + scenario
  let filtered = forecasts;

  if (selectedProductId) {
    filtered = filtered.filter(
      (f) =>
        f.productId == null ||
        String(f.productId) ===
          String(selectedProductId)
    );
  }

  if (scenarioFilter !== 'all') {
    filtered = filtered.filter(
      (f) =>
        (f.scenario || '').toLowerCase() ===
        scenarioFilter
    );
  }

  const sortedTable = computeMomGrowth(
    [...filtered].sort(
      (a, b) =>
        new Date(a.forecastMonth) -
        new Date(b.forecastMonth)
    )
  );

  const byScenario = (name) =>
    forecasts
      .filter((f) =>
        (f.scenario || '')
          .toLowerCase()
          .includes(name)
      )
      .reduce(
        (s, f) =>
          s +
          Number(
            f.forecastedVolumeT || 0
          ),
        0
      );

  const baseTotal =
    byScenario('base') ||
    forecasts
      .filter((f) => {
        const s = (
          f.scenario || ''
        ).toLowerCase();

        return !s || s === 'base';
      })
      .reduce(
        (s, f) =>
          s +
          Number(
            f.forecastedVolumeT || 0
          ),
        0
      );

  const pessimisticTotal =
    byScenario('pessimistic') ||
    forecasts.reduce(
      (s, f) =>
        s +
        Number(f.lowerBoundT ?? 0),
      0
    );

  const optimisticTotal =
    byScenario('optimistic') ||
    forecasts.reduce(
      (s, f) =>
        s +
        Number(f.upperBoundT ?? 0),
      0
    );

  const chart = buildProjectionChart(
    scenarioFilter === 'all'
      ? filtered.filter((f) => {
          const s = (
            f.scenario || 'base'
          ).toLowerCase();

          return (
            s.includes('base') ||
            !f.scenario
          );
        })
      : filtered
  );

  const getRiskBadge = (risk) => {
    const riskStr =
      risk?.toLowerCase() || '';

    if (riskStr.includes('low')) {
      return (
        <span className="bg-[#4caf50]/10 text-[#2e7d32] font-label-md text-[10px] px-sm py-xs rounded-full uppercase tracking-wider">
          Low Risk
        </span>
      );
    }

    if (riskStr.includes('medium')) {
      return (
        <span className="bg-[#ef6c00]/10 text-[#e65100] font-label-md text-[10px] px-sm py-xs rounded-full uppercase tracking-wider">
          Medium Risk
        </span>
      );
    }

    if (riskStr.includes('high')) {
      return (
        <span className="bg-[#c62828]/10 text-[#b71c1c] font-label-md text-[10px] px-sm py-xs rounded-full uppercase tracking-wider">
          High Risk
        </span>
      );
    }

    return (
      <span className="bg-surface-container text-on-surface-variant font-label-md text-[10px] px-sm py-xs rounded-full uppercase tracking-wider">
        {risk || '—'}
      </span>
    );
  };

  const handleRetrain = async () => {
    setRetraining(true);
    setActionError(null);

    try {
      await api.createForecastModelRun({
        cropVariety:
          cropVariety === 'Select product'
            ? 'Dried Mango'
            : cropVariety,
        region: region || null,
        confidenceIntervalPct:
          confidenceInterval,
        useWeatherData: weatherPatterns,
        useCommodityPrices:
          commodityPrices,
        useGeopoliticalIndex:
          geopoliticalIndex,
        modelStatus:
          'Cold-Start Forecast',
      });

      flashSuccess(
        'Model retrain job recorded (POST /api/forecast-model-runs).'
      );

      await fetchForecastingData();
    } catch (err) {
      flashError(
        getErrorMessage(
          err,
          'Failed to create model run'
        )
      );
    } finally {
      setRetraining(false);
    }
  };

  const handleExportCsv = () => {
    if (sortedTable.length === 0) {
      flashError(
        'No forecast rows to export.'
      );
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
      'momGrowth',
      'capacityRisk',
      'modelVersion',
    ];

    const lines = [
      headers.join(','),
      ...sortedTable.map((row) =>
        headers
          .map(
            (h) =>
              `"${String(
                row[h] ?? ''
              ).replace(/"/g, '""')}"`
          )
          .join(',')
      ),
    ];

    const blob = new Blob(
      [lines.join('\n')],
      {
        type: 'text/csv;charset=utf-8;',
      }
    );

    const url =
      URL.createObjectURL(blob);

    const a =
      document.createElement('a');

    a.href = url;

    a.download = `monthly-forecasts-${new Date()
      .toISOString()
      .slice(0, 10)}.csv`;

    a.click();

    URL.revokeObjectURL(url);

    flashSuccess(
      'CSV export downloaded.'
    );
  };

  const openCreateForecast = () => {
    setEditingForecast(null);

    setForecastForm({
      ...emptyForecastForm,
      productId:
        selectedProductId ||
        (products[0]
          ? String(products[0].productId)
          : ''),
      forecastMonth:
        new Date()
          .toISOString()
          .slice(0, 7) + '-01',
    });

    setShowForecastModal(true);
  };

  const openEditForecast = (row) => {
    setEditingForecast(row);

    setForecastForm({
      productId:
        row.productId != null
          ? String(row.productId)
          : selectedProductId,

      forecastMonth: row.forecastMonth
        ? String(
            row.forecastMonth
          ).slice(0, 10)
        : '',

      scenario:
        row.scenario || 'base',

      forecastedVolumeT:
        row.forecastedVolumeT != null
          ? String(
              row.forecastedVolumeT
            )
          : '',

      lowerBoundT:
        row.lowerBoundT != null
          ? String(row.lowerBoundT)
          : '',

      upperBoundT:
        row.upperBoundT != null
          ? String(row.upperBoundT)
          : '',

      capacityRisk:
        row.capacityRisk || 'low',

      modelVersion:
        row.modelVersion || 'v1',
    });

    setShowForecastModal(true);
  };

  const handleSaveForecast = async (e) => {
    e.preventDefault();

    setSavingForecast(true);
    setActionError(null);

    const payload = {
      productId:
        forecastForm.productId
          ? Number(
              forecastForm.productId
            )
          : null,

      forecastMonth:
        forecastForm.forecastMonth,

      scenario:
        forecastForm.scenario,

      forecastedVolumeT:
        Number(
          forecastForm.forecastedVolumeT
        ),

      lowerBoundT:
        forecastForm.lowerBoundT !== ''
          ? Number(
              forecastForm.lowerBoundT
            )
          : null,

      upperBoundT:
        forecastForm.upperBoundT !== ''
          ? Number(
              forecastForm.upperBoundT
            )
          : null,

      capacityRisk:
        forecastForm.capacityRisk || null,

      isActual:
        editingForecast?.isActual ??
        false,

      modelVersion:
        forecastForm.modelVersion || null,
    };

    try {
      if (editingForecast) {
        await api.updateDemandForecast(
          editingForecast.forecastId,
          payload
        );

        flashSuccess(
          `Forecast #${editingForecast.forecastId} updated.`
        );
      } else {
        await api.createDemandForecast(
          payload
        );

        flashSuccess(
          'Forecast created.'
        );
      }

      setShowForecastModal(false);
      setEditingForecast(null);

      await fetchForecastingData();
    } catch (err) {
      flashError(
        getErrorMessage(
          err,
          editingForecast
            ? 'Failed to update forecast'
            : 'Failed to create forecast'
        )
      );
    } finally {
      setSavingForecast(false);
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;

    setDeleting(true);

    try {
      await api.deleteDemandForecast(
        pendingDelete.forecastId
      );

      flashSuccess(
        `Forecast #${pendingDelete.forecastId} deleted.`
      );

      setPendingDelete(null);

      await fetchForecastingData();
    } catch (err) {
      flashError(
        getErrorMessage(
          err,
          'Failed to delete forecast'
        )
      );
    } finally {
      setDeleting(false);
    }
  };

  const confidencePct =
    ((confidenceInterval - 80) /
      (99 - 80)) *
    100;

  return (
    <div className="flex flex-col w-full px-margin-desktop py-xl max-w-[1600px] mx-auto">

      {apiError && (
        <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-secondary text-[24px]">
              wifi_off
            </span>

            <div>
              <p className="font-label-md text-label-md text-on-surface font-bold">
                API Connection Notice
              </p>

              <p className="font-body-sm text-body-sm text-on-surface-variant">
                {apiError}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={fetchForecastingData}
            className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
          >
            <span className="material-symbols-outlined text-[16px]">
              refresh
            </span>
            Retry
          </button>
        </div>
      )}

      {actionError && (
        <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <div className="flex items-center gap-md min-w-0">
            <span className="material-symbols-outlined text-error text-[24px]">
              error
            </span>

            <p className="font-body-sm text-body-sm text-on-error-container truncate">
              {actionError}
            </p>
          </div>

          <button
            type="button"
            onClick={() =>
              setActionError(null)
            }
            className="text-on-error-container"
          >
            <span className="material-symbols-outlined">
              close
            </span>
          </button>
        </div>
      )}

      {actionSuccess && (
        <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
          <p className="font-body-sm text-body-sm text-[#1b5e20]">
            {actionSuccess}
          </p>

          <button
            type="button"
            onClick={() =>
              setActionSuccess(null)
            }
            className="text-[#1b5e20]"
          >
            <span className="material-symbols-outlined">
              close
            </span>
          </button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-end justify-between mb-xl gap-md">
        <div>
          <h2 className="font-headline-lg text-headline-lg text-primary uppercase">
            Demand Forecasting
          </h2>

          <p className="font-body-md text-body-md text-on-surface-variant mt-sm">
            Predictive modeling for agricultural yield and supply chain capacity
          </p>
        </div>

        <div className="flex items-center gap-md flex-wrap">
          <div className="flex items-center gap-sm bg-surface-container-high px-md py-sm rounded-full">
            <span
              className={`w-3 h-3 rounded-full ${
                apiError
                  ? 'bg-secondary'
                  : 'bg-primary'
              } animate-pulse`}
            />

            <span className="font-mono-label text-mono-label text-on-surface uppercase">
              Model:{' '}
              {latestRun?.modelStatus ||
                'Cold-Start Forecast'}
            </span>
          </div>

          <button
            type="button"
            onClick={openCreateForecast}
            className="bg-surface-container text-primary font-label-md text-label-md px-lg py-md rounded-xl hover:bg-surface-container-high transition-colors flex items-center gap-sm uppercase tracking-wider"
          >
            <span className="material-symbols-outlined text-[18px]">
              add
            </span>
            Add Forecast
          </button>

          <button
            type="button"
            disabled={retraining}
            onClick={handleRetrain}
            className="bg-primary text-on-primary font-label-md text-label-md px-lg py-md rounded-xl shadow-md hover:bg-on-primary-fixed-variant transition-colors flex items-center gap-sm uppercase tracking-wider disabled:opacity-60"
          >
            <span
              className={`material-symbols-outlined text-[18px] ${
                retraining
                  ? 'animate-spin'
                  : ''
              }`}
            >
              {retraining
                ? 'sync'
                : 'model_training'}
            </span>

            Retrain Model
          </button>
        </div>
      </div>

      {loading ? (
        <div className="p-xxl bg-surface-container-lowest rounded-xl text-center shadow-sm border border-outline-variant/20 mb-xl">
          <span className="material-symbols-outlined text-[40px] text-primary animate-spin">
            sync
          </span>

          <p className="font-body-md text-body-md text-on-surface-variant mt-md">
            Loading forecasts from APIââ‚¬Â¦
          </p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-12 gap-gutter mb-xl">

            {/* Parameters */}
            <div className="col-span-12 lg:col-span-3 flex flex-col gap-gutter">

              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm border border-outline-variant/20">
                <h3 className="font-headline-sm text-headline-sm text-on-surface mb-lg flex items-center gap-sm border-b border-surface-variant pb-md">
                  <span className="material-symbols-outlined text-primary">
                    tune
                  </span>
                  Model Parameters
                </h3>

                <div className="flex flex-col gap-lg">

                  {/* Crop / Product */}
                  <div className="relative">
                    <label className="font-label-md text-label-md text-on-surface-variant mb-sm block uppercase tracking-wider">
                      Crop Variety
                    </label>

                    <button
                      type="button"
                      onClick={() => {
                        setShowProductMenu(
                          (v) => !v
                        );
                        setShowRegionMenu(false);
                      }}
                      className="w-full bg-surface-container flex items-center justify-between px-md py-md rounded-lg text-on-surface font-body-sm text-body-sm hover:bg-surface-container-high transition-colors"
                    >
                      <span className="truncate">
                        {cropVariety}
                      </span>

                      <span className="material-symbols-outlined text-[18px]">
                        expand_more
                      </span>
                    </button>

                    {showProductMenu && (
                      <div className="absolute z-20 mt-1 w-full bg-surface-container-lowest border border-outline-variant/40 rounded-lg shadow-lg max-h-48 overflow-y-auto">

                        <button
                          type="button"
                          className="w-full text-left px-md py-sm font-body-sm hover:bg-surface-container"
                          onClick={() => {
                            setSelectedProductId('');
                            setShowProductMenu(false);
                          }}
                        >
                          All products
                        </button>

                        {products.map((p) => (
                          <button
                            key={p.productId}
                            type="button"
                            className="w-full text-left px-md py-sm font-body-sm hover:bg-surface-container"
                            onClick={() => {
                              setSelectedProductId(
                                String(p.productId)
                              );
                              setShowProductMenu(false);
                            }}
                          >
                            {p.variety || p.name}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Region */}
                  <div className="relative">
                    <label className="font-label-md text-label-md text-on-surface-variant mb-sm block uppercase tracking-wider">
                      Region
                    </label>

                    <button
                      type="button"
                      onClick={() => {
                        setShowRegionMenu(
                          (v) => !v
                        );
                        setShowProductMenu(false);
                      }}
                      className="w-full bg-surface-container flex items-center justify-between px-md py-md rounded-lg text-on-surface font-body-sm text-body-sm hover:bg-surface-container-high transition-colors"
                    >
                      <span className="truncate">
                        {region}
                      </span>

                      <span className="material-symbols-outlined text-[18px]">
                        expand_more
                      </span>
                    </button>

                    {showRegionMenu && (
                      <div className="absolute z-20 mt-1 w-full bg-surface-container-lowest border border-outline-variant/40 rounded-lg shadow-lg">
                        {REGIONS.map((r) => (
                          <button
                            key={r}
                            type="button"
                            className="w-full text-left px-md py-sm font-body-sm hover:bg-surface-container"
                            onClick={() => {
                              setRegion(r);
                              setShowRegionMenu(false);
                            }}
                          >
                            {r}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Scenario */}
                  <div>
                    <label className="font-label-md text-label-md text-on-surface-variant mb-sm block uppercase tracking-wider">
                      Scenario Filter
                    </label>

                    <select
                      value={scenarioFilter}
                      onChange={(e) =>
                        setScenarioFilter(
                          e.target.value
                        )
                      }
                      className="w-full bg-surface-container px-md py-md rounded-lg text-on-surface font-body-sm border-0"
                    >
                      <option value="all">
                        All scenarios
                      </option>
                      <option value="base">
                        base
                      </option>
                      <option value="optimistic">
                        optimistic
                      </option>
                      <option value="pessimistic">
                        pessimistic
                      </option>
                    </select>
                  </div>

                  {/* Confidence */}
                  <div>
                    <div className="flex justify-between items-center mb-sm">
                      <label className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                        Confidence Interval
                      </label>

                      <span className="font-mono-label text-mono-label text-primary">
                        {confidenceInterval}%
                      </span>
                    </div>

                    <div className="relative h-2 bg-surface-container rounded-full">
                      <div
                        className="absolute top-0 left-0 h-full bg-primary rounded-full"
                        style={{
                          width: `${confidencePct}%`,
                        }}
                      />

                      <input
                        type="range"
                        min="80"
                        max="99"
                        value={confidenceInterval}
                        onChange={(e) =>
                          setConfidenceInterval(
                            Number(
                              e.target.value
                            )
                          )
                        }
                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                      />
                    </div>

                    <div className="flex justify-between mt-sm text-on-surface-variant font-mono-label text-[10px]">
                      <span>80%</span>
                      <span>99%</span>
                    </div>
                  </div>

                  {/* External Variables */}
                  <div>
                    <label className="font-label-md text-label-md text-on-surface-variant mb-sm block uppercase tracking-wider">
                      External Variables
                    </label>

                    <div className="space-y-sm">
                      {[
                        {
                          label:
                            'Weather Patterns',
                          value:
                            weatherPatterns,
                          set:
                            setWeatherPatterns,
                        },
                        {
                          label:
                            'Commodity Prices',
                          value:
                            commodityPrices,
                          set:
                            setCommodityPrices,
                        },
                        {
                          label:
                            'Geopolitical Index',
                          value:
                            geopoliticalIndex,
                          set:
                            setGeopoliticalIndex,
                          muted: true,
                        },
                      ].map((item) => (
                        <label
                          key={item.label}
                          className="flex items-center gap-md cursor-pointer group"
                          onClick={() =>
                            item.set(
                              !item.value
                            )
                          }
                        >
                          <div
                            className={`relative w-10 h-5 rounded-full transition-colors flex-shrink-0 ${
                              item.value
                                ? 'bg-primary'
                                : 'bg-surface-variant'
                            }`}
                          >
                            <div
                              className={`absolute top-1 w-3 h-3 rounded-full transition-transform ${
                                item.value
                                  ? 'bg-on-primary left-1 translate-x-5'
                                  : 'bg-surface left-1'
                              }`}
                            />
                          </div>

                          <span
                            className={`font-body-sm text-body-sm group-hover:text-primary transition-colors ${
                              item.muted
                                ? 'text-on-surface-variant'
                                : 'text-on-surface'
                            }`}
                          >
                            {item.label}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              {/* Model Status */}
              <div className="bg-primary-container rounded-xl p-lg shadow-sm text-on-primary-container relative overflow-hidden group">
                <div className="absolute -right-12 -bottom-12 w-32 h-32 bg-on-primary-container opacity-5 rounded-full blur-xl group-hover:scale-150 transition-transform duration-700" />

                <h4 className="font-label-md text-label-md uppercase tracking-wider opacity-80 mb-sm">
                  MODEL STATUS
                </h4>

                <p className="font-bold text-lg text-white">
                  {latestRun?.modelStatus ||
                    'Cold-Start Forecast'}
                </p>

                <p className="font-body-sm text-body-sm mt-sm opacity-90 leading-relaxed text-white/90">
                  {modelRuns.length > 0
                    ? `${modelRuns.length} model run(s) · latest #${latestRun?.runId}`
                    : 'Accuracy: Not yet available — awaiting historical sales data'}
                </p>

                <div className="grid grid-cols-2 gap-sm mt-md">

                  <div className="flex flex-col">
                    <span className="text-[10px] uppercase opacity-70">
                      Pessimistic Case
                    </span>
                    <span className="font-bold text-white">
                      {formatTons(
                        pessimisticTotal
                      )}{' '}
                      t
                    </span>
                  </div>

                  <div className="flex flex-col">
                    <span className="text-[10px] uppercase opacity-70">
                      Base Case
                    </span>
                    <span className="font-bold text-white">
                      {formatTons(
                        baseTotal
                      )}{' '}
                      t
                    </span>
                  </div>

                  <div className="flex flex-col">
                    <span className="text-[10px] uppercase opacity-70">
                      Optimistic Case
                    </span>
                    <span className="font-bold text-white">
                      {formatTons(
                        optimisticTotal
                      )}{' '}
                      t
                    </span>
                  </div>

                  <div className="flex flex-col">
                    <span className="text-[10px] uppercase opacity-70">
                      Regional Base
                    </span>
                    <span className="font-bold text-white">
                      No data
                    </span>
                  </div>
                </div>

                <div className="mt-lg pt-md border-t border-white/10">
                  <p className="text-[10px] uppercase opacity-70">
                    Revenue Potential
                  </p>

                  <p className="text-xl font-bold text-white">
                    —
                  </p>
                </div>
              </div>
            </div>

            {/* Chart */}
            <div className="col-span-12 lg:col-span-9 flex flex-col gap-gutter">

              <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm border border-outline-variant/20 flex-1 flex flex-col min-h-[500px]">

                <div className="flex flex-col sm:flex-row justify-between items-start mb-lg gap-sm">

                  <div>
                    <h3 className="font-headline-md text-headline-md text-on-surface">
                      Demand Projection (Next 6 Months)
                    </h3>

                    <p className="font-body-md text-body-md text-on-surface-variant">
                      Measured in Metric Tons (MT)
                      {filtered.length > 0
                        ? ` · ${filtered.length} row(s)`
                        : ''}
                    </p>
                  </div>

                  <div className="flex items-center gap-md flex-wrap">

                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-surface-variant" />
                      <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                        Historical
                      </span>
                    </div>

                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-primary" />
                      <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                        Forecast
                      </span>
                    </div>

                    <div className="flex items-center gap-xs">
                      <span className="w-3 h-3 rounded-full bg-primary opacity-20" />
                      <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                        {confidenceInterval}% CI
                      </span>
                    </div>

                  </div>
                </div>

                <div className="flex-1 relative w-full h-full bg-surface-container-low rounded-lg p-md min-h-[360px]">

                  {!chart ? (
                    <div className="absolute inset-0 flex flex-col items-center justify-center text-on-surface-variant">
                      <span className="material-symbols-outlined text-[40px] opacity-40 mb-sm">
                        show_chart
                      </span>

                      <p className="font-body-sm text-body-sm">
                        Not enough forecast points to chart.
                      </p>

                      <button
                        type="button"
                        onClick={
                          openCreateForecast
                        }
                        className="mt-md text-primary font-label-md text-label-md hover:underline"
                      >
                        Add forecast rows
                      </button>
                    </div>
                  ) : (
                    <>
                      <svg
                        className="w-full h-full"
                        preserveAspectRatio="none"
                        viewBox="0 0 1000 400"
                      >
                        <line
                          className="text-surface-variant"
                          stroke="currentColor"
                          strokeDasharray="4"
                          x1="0"
                          x2="1000"
                          y1="50"
                          y2="50"
                        />

                        <line
                          className="text-surface-variant"
                          stroke="currentColor"
                          strokeDasharray="4"
                          x1="0"
                          x2="1000"
                          y1="150"
                          y2="150"
                        />

                        <line
                          className="text-surface-variant"
                          stroke="currentColor"
                          strokeDasharray="4"
                          x1="0"
                          x2="1000"
                          y1="250"
                          y2="250"
                        />

                        <line
                          className="text-surface-variant"
                          stroke="currentColor"
                          strokeDasharray="4"
                          x1="0"
                          x2="1000"
                          y1="350"
                          y2="350"
                        />

                        <line
                          className="text-outline"
                          stroke="currentColor"
                          strokeDasharray="8"
                          strokeWidth="2"
                          x1={chart.todayX}
                          x2={chart.todayX}
                          y1="0"
                          y2="400"
                        />

                        <text
                          className="font-mono-label text-[12px] fill-on-surface-variant font-bold"
                          x={
                            chart.todayX + 10
                          }
                          y="24"
                        >
                          TODAY
                        </text>

                        <path
                          className="text-primary opacity-10"
                          d={chart.bandPath}
                          fill="currentColor"
                        />

                        <path
                          className="text-outline-variant"
                          d={chart.histLine}
                          fill="none"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeWidth="4"
                        />

                        <path
                          className="text-primary"
                          d={chart.forecastLine}
                          fill="none"
                          stroke="currentColor"
                          strokeDasharray="8"
                          strokeLinecap="round"
                          strokeWidth="4"
                        />

                        {chart.points.map(
                          (pt, i) => (
                            <circle
                              key={i}
                              className={
                                pt.historical
                                  ? 'text-on-surface-variant'
                                  : 'text-primary'
                              }
                              cx={pt.x}
                              cy={pt.y}
                              fill="currentColor"
                              r={
                                pt.historical
                                  ? 5
                                  : 6
                              }
                            />
                          )
                        )}
                      </svg>

                      <div className="absolute bottom-2 left-0 right-0 flex justify-between px-md font-mono-label text-[11px] text-on-surface-variant">
                        {chart.labels.map(
                          (label, i) => (
                            <span
                              key={`${label}-${i}`}
                              className={
                                i ===
                                Math.floor(
                                  chart.labels.length *
                                    0.55
                                )
                                  ? 'font-bold text-primary'
                                  : ''
                              }
                            >
                              {label}
                            </span>
                          )
                        )}
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Monthly table */}
          <div className="bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/20 overflow-hidden mb-xl">
          {/* Forecast Accuracy */}
<div className="bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/20 overflow-hidden mb-xl">

  <div className="px-lg py-md border-b border-surface-variant bg-surface">
    <h3 className="font-headline-sm text-headline-sm text-on-surface">
      Forecast Accuracy
    </h3>

    <p className="font-body-sm text-body-sm text-on-surface-variant mt-xs">
      Comparison between forecasted demand and actual sales when actual data is available.
    </p>
  </div>

  <div className="p-lg">

    {forecastAccuracyData.length === 0 ? (
      <div className="text-center py-lg text-on-surface-variant font-body-sm">
        No forecast accuracy data available.
      </div>
    ) : (
      <div className="w-full overflow-x-auto">

        <table className="w-full text-left border-collapse">

          <thead>
            <tr className="bg-surface-container-low font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">

              <th className="py-md px-lg font-semibold">
                Month
              </th>

              <th className="py-md px-lg font-semibold">
                Forecast (MT)
              </th>

              <th className="py-md px-lg font-semibold">
                Actual (MT)
              </th>

              <th className="py-md px-lg font-semibold">
                Forecast Accuracy
              </th>

            </tr>
          </thead>

          <tbody className="font-body-sm text-body-sm text-on-surface">

            {forecastAccuracyData.map((row) => (
              <tr
                key={`accuracy-${row.forecastId}`}
                className="border-b border-surface-variant hover:bg-surface-bright transition-colors"
              >

                <td className="py-md px-lg font-mono-label">
                  {formatMonth(row.forecastMonth)}
                </td>

                <td className="py-md px-lg">
                  {Number(row.forecastedVolumeT ?? 0).toFixed(1)} MT
                </td>

                <td className="py-md px-lg">
                  {row.actualVolumeT === null
                    ? 'N/A'
                    : `${row.actualVolumeT.toFixed(1)} MT`}
                </td>

                <td className="py-md px-lg font-semibold">
                  {row.accuracy === null
                    ? 'N/A'
                    : `${row.accuracy.toFixed(1)}%`}
                </td>

              </tr>
            ))}

          </tbody>

        </table>

      </div>
    )}

  </div>
</div>

            <div className="px-lg py-md border-b border-surface-variant bg-surface flex justify-between items-center gap-md flex-wrap">

              <h3 className="font-headline-sm text-headline-sm text-on-surface">
                Monthly Forecast Data
              </h3>

              <button
                type="button"
                onClick={handleExportCsv}
                className="flex items-center gap-xs text-primary font-label-md text-label-md hover:bg-surface-container px-md py-sm rounded-lg transition-colors"
              >
                <span className="material-symbols-outlined text-[18px]">
                  download
                </span>
                EXPORT CSV
              </button>
            </div>

            <div className="w-full overflow-x-auto">

              {sortedTable.length === 0 ? (
                <div className="p-xl text-center text-on-surface-variant font-body-sm">
                  No forecasts from /api/demand-forecasts match the current filters.
                </div>
              ) : (
                <table className="w-full text-left border-collapse">

                  <thead>
                    <tr className="bg-surface-container-low font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">

                      <th className="py-md px-lg font-semibold">
                        Month
                      </th>

                      <th className="py-md px-lg font-semibold">
                        Predicted Demand (MT)
                      </th>

                      <th className="py-md px-lg font-semibold">
                        Lower Bound ({confidenceInterval}%)
                      </th>

                      <th className="py-md px-lg font-semibold">
                        Upper Bound ({confidenceInterval}%)
                      </th>

                      <th className="py-md px-lg font-semibold">
                        MoM Growth
                      </th>

                      <th className="py-md px-lg font-semibold">
                        Capacity Risk
                      </th>

                      <th className="py-md px-lg font-semibold text-right">
                        Actions
                      </th>
                    </tr>
                  </thead>

                  <tbody className="font-body-sm text-body-sm text-on-surface">

                    {sortedTable.map(
                      (row) => (
                        <tr
                          key={
                            row.forecastId
                          }
                          className="border-b border-surface-variant hover:bg-surface-bright transition-colors"
                        >

                          <td className="py-md px-lg font-mono-label">
                            {formatMonth(
                              row.forecastMonth
                            )}

                            {row.scenario ? (
                              <span className="ml-sm text-[10px] uppercase text-on-surface-variant">
                                (
                                {
                                  row.scenario
                                }
                                )
                              </span>
                            ) : null}
                          </td>

                          <td className="py-md px-lg font-bold text-lg">
                            {row.forecastedVolumeT !=
                            null
                              ? Number(
                                  row.forecastedVolumeT
                                ).toLocaleString()
                              : '—'}
                          </td>

                          <td className="py-md px-lg text-on-surface-variant">
                            {row.lowerBoundT !=
                            null
                              ? Number(
                                  row.lowerBoundT
                                ).toLocaleString()
                              : '—'}
                          </td>

                          <td className="py-md px-lg text-on-surface-variant">
                            {row.upperBoundT !=
                            null
                              ? Number(
                                  row.upperBoundT
                                ).toLocaleString()
                              : '—'}
                          </td>

                          <td className="py-md px-lg text-on-surface-variant">
                            {row.momGrowth &&
                            String(
                              row.momGrowth
                            ).includes('+') ? (
                              <span className="text-[#4caf50] font-semibold flex items-center gap-xs">
                                <span className="material-symbols-outlined text-[16px]">
                                  trending_up
                                </span>

                                {
                                  row.momGrowth
                                }
                              </span>
                            ) : (
                              row.momGrowth ||
                              '-'
                            )}
                          </td>

                          <td className="py-md px-lg">
                            {getRiskBadge(
                              row.capacityRisk
                            )}
                          </td>

                          <td className="py-md px-lg text-right">
                            <div className="inline-flex items-center gap-xs">

                              <button
                                type="button"
                                title="Edit"
                                onClick={() =>
                                  openEditForecast(
                                    row
                                  )
                                }
                                className="w-8 h-8 rounded-full text-on-surface-variant hover:bg-primary/10 hover:text-primary transition-colors"
                              >
                                <span className="material-symbols-outlined text-[18px]">
                                  edit
                                </span>
                              </button>

                              <button
                                type="button"
                                title="Delete"
                                onClick={() =>
                                  setPendingDelete(
                                    row
                                  )
                                }
                                className="w-8 h-8 rounded-full text-on-surface-variant hover:bg-error/10 hover:text-error transition-colors"
                              >
                                <span className="material-symbols-outlined text-[18px]">
                                  delete
                                </span>
                              </button>

                            </div>
                          </td>
                        </tr>
                      )
                    )}

                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}

      {/* Create / Edit modal */}
      {showForecastModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">

          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg p-xl relative max-h-[90vh] overflow-y-auto">

            <button
              type="button"
              onClick={() =>
                setShowForecastModal(false)
              }
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">
                close
              </span>
            </button>

            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingForecast
                ? `Edit Forecast #${editingForecast.forecastId}`
                : 'Add Forecast'}
            </h3>

            <form
              onSubmit={
                handleSaveForecast
              }
              className="flex flex-col gap-md"
            >

              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Product
                </label>

                <select
                  required
                  value={
                    forecastForm.productId
                  }
                  onChange={(e) =>
                    setForecastForm(
                      (f) => ({
                        ...f,
                        productId:
                          e.target.value,
                      })
                    )
                  }
                  className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                >
                  <option value="">
                    Selectââ‚¬Â¦
                  </option>

                  {products.map((p) => (
                    <option
                      key={p.productId}
                      value={p.productId}
                    >
                      {p.name}{' '}
                      {p.variety
                        ? `(${p.variety})`
                        : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-md">

                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Month
                  </label>

                  <input
                    required
                    type="date"
                    value={
                      forecastForm.forecastMonth
                    }
                    onChange={(e) =>
                      setForecastForm(
                        (f) => ({
                          ...f,
                          forecastMonth:
                            e.target.value,
                        })
                      )
                    }
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>

                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Scenario
                  </label>

                  <select
                    required
                    value={
                      forecastForm.scenario
                    }
                    onChange={(e) =>
                      setForecastForm(
                        (f) => ({
                          ...f,
                          scenario:
                            e.target.value,
                        })
                      )
                    }
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  >
                    <option value="base">
                      base
                    </option>

                    <option value="optimistic">
                      optimistic
                    </option>

                    <option value="pessimistic">
                      pessimistic
                    </option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-md">

                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Volume (t)
                  </label>

                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={
                      forecastForm.forecastedVolumeT
                    }
                    onChange={(e) =>
                      setForecastForm(
                        (f) => ({
                          ...f,
                          forecastedVolumeT:
                            e.target.value,
                        })
                      )
                    }
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>

                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Lower
                  </label>

                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={
                      forecastForm.lowerBoundT
                    }
                    onChange={(e) =>
                      setForecastForm(
                        (f) => ({
                          ...f,
                          lowerBoundT:
                            e.target.value,
                        })
                      )
                    }
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>

                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Upper
                  </label>

                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={
                      forecastForm.upperBoundT
                    }
                    onChange={(e) =>
                      setForecastForm(
                        (f) => ({
                          ...f,
                          upperBoundT:
                            e.target.value,
                        })
                      )
                    }
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Capacity Risk
                </label>

                <select
                  value={
                    forecastForm.capacityRisk
                  }
                  onChange={(e) =>
                    setForecastForm(
                      (f) => ({
                        ...f,
                        capacityRisk:
                          e.target.value,
                      })
                    )
                  }
                  className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                >
                  <option value="low">
                    low
                  </option>

                  <option value="medium">
                    medium
                  </option>

                  <option value="high">
                    high
                  </option>
                </select>
              </div>

              <div className="flex gap-md mt-md">

                <button
                  type="button"
                  onClick={() =>
                    setShowForecastModal(
                      false
                    )
                  }
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={savingForecast}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60"
                >
                  {savingForecast
                    ? 'Savingââ‚¬Â¦'
                    : editingForecast
                    ? 'Update'
                    : 'Create'}
                </button>

              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirm */}
      {pendingDelete && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">

          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">

            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">
              Delete forecast?
            </h3>

            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              Calls{' '}
              <code className="text-primary">
                DELETE /api/demand-forecasts/
                {
                  pendingDelete.forecastId
                }
              </code>{' '}
              for{' '}
              {formatMonth(
                pendingDelete.forecastMonth
              )}{' '}
              (
              {pendingDelete.scenario ||
                'scenario'}
              ).
            </p>

            <div className="flex gap-md">

              <button
                type="button"
                onClick={() =>
                  setPendingDelete(null)
                }
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
                {deleting
                  ? 'Deletingââ‚¬Â¦'
                  : 'Delete'}
              </button>

            </div>
          </div>
        </div>
      )}
    </div>
  );
}
