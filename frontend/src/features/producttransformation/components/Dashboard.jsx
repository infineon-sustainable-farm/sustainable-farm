/**
 * Product Transformation - Dashboard.
 *
 * The module's home: the figures of the period, then the charts that explain
 * them. Every chart reads an existing API. Two of the charts are derived on the
 * client from records the line already writes:
 *
 *  - Grade Distribution   <- /api/raw-intakes (receivedGrade, receivedQuantityKg)
 *  - Batch Processing     <- /api/batches    (currentStatus)
 *  - Processing Performance <- /api/wash-sort-records + /api/drying-runs
 *
 * No endpoint is invented and no business metric is made up. When the demo
 * build flag is on and a dataset is empty, the chart falls back to the
 * synthetic set in `data/dashboardDemoData.js` and says "Demo data" on the card.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { CheckCircle, Droplets, Factory, Package, TrendingUp, Zap } from 'lucide-react';

import { dashboardApi } from '../api/dashboardApi';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { batchApi } from '../api/batchApi';
import { washSortApi } from '../api/washSortApi';
import { dryingApi } from '../api/dryingApi';
import { formatNumber, formatPercentage, formatWeight } from '../utils/format';
import {
  aggregateBatchStatus,
  aggregateGradeDistribution,
  aggregateProcessingPerformance,
  normalizeQualityTrend,
  totalBatches,
} from '../utils/dashboardMetrics';
import {
  DEMO_BATCH_STATUS,
  DEMO_GRADE_DISTRIBUTION,
  DEMO_PROCESSING_PERFORMANCE,
  DEMO_QUALITY_TREND,
} from '../data/dashboardDemoData';

import PTKpiCard from './PTKpiCard';
import HarvestTrendChart from './charts/HarvestTrendChart';
import ProductionOutputChart from './charts/ProductionOutputChart';
import GradeDistributionChart from './charts/GradeDistributionChart';
import BatchProcessingChart from './charts/BatchProcessingChart';
import QualityTrendChart from './charts/QualityTrendChart';
import EquipmentUtilizationChart from './charts/EquipmentUtilizationChart';
import ProcessingPerformanceChart from './charts/ProcessingPerformanceChart';
import EnergyBreakdownChart from './charts/EnergyBreakdownChart';
import { PAGE_SUBTITLE, PAGE_TITLE, SKELETON } from '../styles/ptStyles';

// Build-time flag. When enabled, an empty dataset falls back to the synthetic
// demo set and the affected card says "Demo data".
const DEMO_DATA_ENABLED = import.meta.env.VITE_DEMO_DATA === 'true';

const loadDashboard = () =>
  Promise.allSettled([
    dashboardApi.getDashboardStats(),
    dashboardApi.getHarvestTrendData(),
    dashboardApi.getProductionOutputData(),
    dashboardApi.getEnergyBreakdownData(),
    dashboardApi.getEquipmentUtilizationData(),
    dashboardApi.getQualityTrendData(),
    rawIntakeApi.getAllRawIntakes(),
    batchApi.getAllBatches(),
    washSortApi.getAllWashSortRecords(),
    dryingApi.getAllDryingRuns(),
  ]);

const EMPTY_CHARTS = {
  harvestTrend: [],
  productionOutput: [],
  energyBreakdown: [],
  equipmentUtilization: [],
  qualityTrend: [],
  gradeDistribution: [],
  batchStatus: [],
  processingPerformance: [],
};

function DashboardSkeleton() {
  return (
    <div className="p-6">
      <h1 className={PAGE_TITLE}>Product Transformation Dashboard</h1>
      <p className={`${PAGE_SUBTITLE} mb-6`}>Loading the latest figures…</p>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[0, 1, 2, 3, 4, 5, 6, 7].map((index) => (
          <div key={index} className={`h-36 ${SKELETON}`} />
        ))}
      </div>
      <div className="mt-6 grid grid-cols-1 gap-5 xl:grid-cols-2">
        {[0, 1, 2, 3].map((index) => (
          <div key={index} className={`h-80 ${SKELETON}`} />
        ))}
      </div>
    </div>
  );
}

const Dashboard = () => {
  const [kpis, setKpis] = useState(null);
  const [charts, setCharts] = useState(EMPTY_CHARTS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const applyResults = useCallback((results) => {
    const [
      kpiResult,
      harvestTrend,
      productionOutput,
      energyBreakdown,
      equipmentUtilization,
      qualityTrend,
      rawIntakes,
      batches,
      washRecords,
      dryingRuns,
    ] = results;

    if (kpiResult.status === 'fulfilled') {
      setKpis(kpiResult.value);
    } else {
      console.error('Failed to fetch dashboard KPIs:', kpiResult.reason);
      setError('Failed to load dashboard KPIs. Please try again later.');
    }

    const valueOrEmpty = (result) =>
      result.status === 'fulfilled' && Array.isArray(result.value) ? result.value : [];

    setCharts({
      harvestTrend: valueOrEmpty(harvestTrend),
      productionOutput: valueOrEmpty(productionOutput),
      energyBreakdown: valueOrEmpty(energyBreakdown),
      equipmentUtilization: valueOrEmpty(equipmentUtilization),
      qualityTrend: normalizeQualityTrend(valueOrEmpty(qualityTrend)),
      gradeDistribution: aggregateGradeDistribution(valueOrEmpty(rawIntakes)),
      batchStatus: aggregateBatchStatus(valueOrEmpty(batches)),
      processingPerformance: aggregateProcessingPerformance(
        valueOrEmpty(washRecords),
        valueOrEmpty(dryingRuns),
      ),
    });

    setLoading(false);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    applyResults(await loadDashboard());
  };

  useEffect(() => {
    let active = true;
    loadDashboard().then((results) => {
      if (active) applyResults(results);
    });
    return () => {
      active = false;
    };
  }, [applyResults]);

  const demo = useMemo(
    () => ({
      gradeDistribution: DEMO_DATA_ENABLED && charts.gradeDistribution.length === 0,
      batchStatus: DEMO_DATA_ENABLED && totalBatches(charts.batchStatus) === 0,
      processingPerformance: DEMO_DATA_ENABLED && charts.processingPerformance.length === 0,
      qualityTrend: DEMO_DATA_ENABLED && charts.qualityTrend.length === 0,
    }),
    [charts],
  );

  const gradeDistribution = demo.gradeDistribution
    ? DEMO_GRADE_DISTRIBUTION
    : charts.gradeDistribution;
  const batchStatus = demo.batchStatus ? DEMO_BATCH_STATUS : charts.batchStatus;
  const processingPerformance = demo.processingPerformance
    ? DEMO_PROCESSING_PERFORMANCE
    : charts.processingPerformance;
  const qualityTrend = demo.qualityTrend ? DEMO_QUALITY_TREND : charts.qualityTrend;

  if (loading) return <DashboardSkeleton />;

  if (error) {
    return (
      <div className="p-6">
        <h1 className={PAGE_TITLE}>Product Transformation Dashboard</h1>
        <div className="mt-4 rounded-2xl border border-red-200 bg-white p-6 shadow-card">
          <p className="mb-4 text-sm text-red-700">{error}</p>
          <button
            type="button"
            onClick={fetchData}
            className="inline-flex h-10 items-center justify-center rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const kpiCards = [
    {
      to: '/product-transformation/raw-intake',
      label: 'Harvest Quantity',
      value: formatWeight(kpis?.harvestQuantity),
      icon: Package,
      tone: 'orange',
      hint: 'Plants harvest events',
    },
    {
      to: '/product-transformation/batches',
      label: 'Active Batches',
      value: kpis?.activeBatches ?? 0,
      icon: TrendingUp,
      tone: 'success',
    },
    {
      to: '/product-transformation/batches',
      label: 'Production Output',
      value: formatNumber(kpis?.productionOutput),
      icon: Factory,
      tone: 'info',
    },
    {
      to: '/product-transformation/quality-control',
      label: 'Quality Pass Rate',
      value: formatPercentage(kpis?.qualityPassRate),
      icon: CheckCircle,
      tone: 'success',
      hint: `Target: ${kpis?.qualityTargetStatus ?? 'n/a'}`,
    },
    {
      to: '/product-transformation/equipment',
      label: 'Equipment Utilization',
      value: formatPercentage(kpis?.equipmentUtilization),
      icon: Factory,
      tone: 'purple',
    },
    {
      to: '/product-transformation/drying',
      label: 'Energy Consumption',
      value: `${formatNumber(kpis?.energyConsumption)} kWh/kg`,
      icon: Zap,
      tone: 'warning',
    },
    {
      to: '/product-transformation/raw-intake',
      label: 'Grade A Share',
      value: formatPercentage(kpis?.gradeAPercentage),
      icon: CheckCircle,
      tone: 'primary',
    },
    {
      to: '/product-transformation/washing-sorting',
      label: 'Water Consumption',
      value: `${formatNumber(kpis?.waterConsumption)} L/kg`,
      icon: Droplets,
      tone: 'info',
    },
  ];

  return (
    <div className="p-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className={PAGE_TITLE}>Product Transformation Dashboard</h1>
          <p className={PAGE_SUBTITLE}>
            Period: {kpis?.period ?? 'n/a'} ({kpis?.startDate ?? '?'} → {kpis?.endDate ?? '?'})
          </p>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-sm font-medium ${
            DEMO_DATA_ENABLED ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800'
          }`}
        >
          {DEMO_DATA_ENABLED
            ? 'Data source: DEMO dataset (Plants format)'
            : 'Data source: Plants harvest events'}
        </span>
      </div>

      {DEMO_DATA_ENABLED ? (
        <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Demo mode is on: charts with no records fall back to a synthetic set (LOT-DEMO). Real API
          data is always used when present. The mock set lives in
          frontend/src/features/producttransformation/data/dashboardDemoData.js.
        </div>
      ) : null}

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {kpiCards.map((card, index) => (
          <PTKpiCard key={card.label} {...card} delay={index * 60} />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <HarvestTrendChart data={charts.harvestTrend} />
        <ProductionOutputChart data={charts.productionOutput} />

        <div className="xl:col-span-2">
          <GradeDistributionChart data={gradeDistribution} demo={demo.gradeDistribution} />
        </div>

        <BatchProcessingChart data={batchStatus} demo={demo.batchStatus} />
        <QualityTrendChart data={qualityTrend} demo={demo.qualityTrend} />

        <div className="xl:col-span-2">
          <ProcessingPerformanceChart
            data={processingPerformance}
            demo={demo.processingPerformance}
          />
        </div>

        <EquipmentUtilizationChart data={charts.equipmentUtilization} />
        <EnergyBreakdownChart data={charts.energyBreakdown} />
      </div>
    </div>
  );
};

export default Dashboard;
