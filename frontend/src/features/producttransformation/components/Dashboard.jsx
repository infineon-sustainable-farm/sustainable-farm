/**
 * Product Transformation - Dashboard screen
 * Displays key metrics and statistics for Product Transformation.
 *
 * Data contract (backend /api/product-transformation/dashboard):
 *  - /kpis                : { harvestQuantity, activeBatches, productionOutput, qualityPassRate,
 *                            waterConsumption, energyConsumption, solarEnergyShare, equipmentUtilization,
 *                            gradeAPercentage, qualityTargetStatus, totalEnergyToday, productionEfficiency,
 *                            startDate, endDate, period, generatedAt }
 *  - /charts/harvest-trend      : [{ date, quantity }]
 *  - /charts/production-output  : [{ period, output }]
 *  - /charts/energy-breakdown   : [{ name, value }]
 *  - /charts/equipment-utilization : [{ equipment, utilization }]
 *  - /charts/quality-trend      : [{ date, passRate }]
 *
 * Harvest KPIs are fed by the Plants harvest events mirrored in Product Transformation.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Badge, Card, ErrorState, PageHeader, Skeleton } from '../../../shared/ui';
import { dashboardApi } from '../api/dashboardApi';
import DashboardCharts from './DashboardCharts';
import DashboardKpis from './DashboardKpis';

const SKELETON_TILES = [1, 2, 3, 4, 5, 6, 7, 8];

const loadDashboard = () =>
  Promise.allSettled([
    dashboardApi.getDashboardStats(),
    dashboardApi.getHarvestTrendData(),
    dashboardApi.getProductionOutputData(),
    dashboardApi.getEnergyBreakdownData(),
    dashboardApi.getEquipmentUtilizationData(),
    dashboardApi.getQualityTrendData(),
  ]);

const Dashboard = () => {
  const { t } = useTranslation('producttransformation');
  const [kpis, setKpis] = useState(null);
  const [charts, setCharts] = useState({
    harvestTrend: [],
    productionOutput: [],
    energyBreakdown: [],
    equipmentUtilization: [],
    qualityTrend: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const applyResults = useCallback((results) => {
    const [kpiResult, harvestTrend, productionOutput, energyBreakdown, equipmentUtilization, qualityTrend] =
      results;

    if (kpiResult.status === 'fulfilled') {
      setKpis(kpiResult.value);
    } else {
      console.error('Failed to fetch dashboard KPIs:', kpiResult.reason);
      setError(true);
    }

    const valueOrEmpty = (result) => (result.status === 'fulfilled' && Array.isArray(result.value) ? result.value : []);

    setCharts({
      harvestTrend: valueOrEmpty(harvestTrend),
      productionOutput: valueOrEmpty(productionOutput),
      energyBreakdown: valueOrEmpty(energyBreakdown),
      equipmentUtilization: valueOrEmpty(equipmentUtilization),
      qualityTrend: valueOrEmpty(qualityTrend),
    });

    setLoading(false);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(false);
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

  const title = t('dashboard.title');

  if (loading) {
    return (
      <div className="p-6">
        <PageHeader className="mb-4" title={title} />
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
          {SKELETON_TILES.map((tile) => (
            <Skeleton key={tile} radius="card" className="h-28" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <PageHeader className="mb-4" title={title} />
        <Card>
          <ErrorState message={t('dashboard.loadError')} onRetry={fetchData} />
        </Card>
      </div>
    );
  }

  const naShort = t('dashboard.naShort');

  return (
    <div className="p-6">
      <PageHeader
        className="mb-6"
        title={title}
        subtitle={t('dashboard.period', {
          period: kpis?.period ?? naShort,
          start: kpis?.startDate ?? '?',
          end: kpis?.endDate ?? '?',
        })}
        actions={
          <Badge tone="success" size="md">
            {t('dashboard.sourcePlants')}
          </Badge>
        }
      />

      <DashboardKpis kpis={kpis} />
      <DashboardCharts charts={charts} />
    </div>
  );
};

export default Dashboard;
