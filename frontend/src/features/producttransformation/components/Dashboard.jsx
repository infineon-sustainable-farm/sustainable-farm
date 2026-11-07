/**
 * Product Transformation - Dashboard Component
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
 * @version 1.1.0
 */

import { useEffect, useState } from 'react';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Package, TrendingUp, CheckCircle, Zap, Droplets, Factory } from 'lucide-react';

import { dashboardApi } from '../api/dashboardApi';
import { formatNumber, formatWeight, formatPercentage } from '../utils/format';

const CHART_COLORS = ['#0a8276', '#ffc658', '#4caf50', '#1e88e5', '#8e24aa', '#c62828'];

// Build-time flag. When enabled, the dashboard says out loud that it displays a
// synthetic data set (db/demo/pt_demo_seed.sql) instead of real Plants harvests.
const DEMO_DATA_ENABLED = import.meta.env.VITE_DEMO_DATA === 'true';

const hasRows = (rows) => Array.isArray(rows) && rows.length > 0;

const ChartCard = ({ title, subtitle, children }) => (
  <div className="bg-white rounded-lg shadow p-6">
    <h3 className="font-medium text-gray-500 mb-1">{title}</h3>
    {subtitle ? <p className="text-xs text-gray-400 mb-4">{subtitle}</p> : <div className="mb-4" />}
    {children}
  </div>
);

const EmptyChart = ({ message }) => (
  <div className="h-64 flex items-center justify-center text-gray-400 text-sm">
    {message}
  </div>
);

const KpiCard = ({ title, value, icon: Icon, color, hint }) => (
  <div className="bg-white rounded-lg shadow p-6">
    <div className="flex items-center justify-between">
      <div>
        <h3 className="font-medium text-gray-500 mb-2">{title}</h3>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
        {hint ? <p className="text-xs text-gray-400 mt-1">{hint}</p> : null}
      </div>
      <div className={`${color} p-3 rounded-lg`}>
        <Icon className="w-6 h-6 text-white" />
      </div>
    </div>
  </div>
);

const Dashboard = () => {
  const [kpis, setKpis] = useState(null);
  const [charts, setCharts] = useState({
    harvestTrend: [],
    productionOutput: [],
    energyBreakdown: [],
    equipmentUtilization: [],
    qualityTrend: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);

    const results = await Promise.allSettled([
      dashboardApi.getDashboardStats(),
      dashboardApi.getHarvestTrendData(),
      dashboardApi.getProductionOutputData(),
      dashboardApi.getEnergyBreakdownData(),
      dashboardApi.getEquipmentUtilizationData(),
      dashboardApi.getQualityTrendData(),
    ]);

    const [kpiResult, harvestTrend, productionOutput, energyBreakdown, equipmentUtilization, qualityTrend] =
      results;

    if (kpiResult.status === 'fulfilled') {
      setKpis(kpiResult.value);
    } else {
      console.error('Failed to fetch dashboard KPIs:', kpiResult.reason);
      setError('Failed to load dashboard KPIs. Please try again later.');
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
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="p-6">
        <h1 className="font-heading text-2xl font-bold mb-4">Product Transformation Dashboard</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((_, i) => (
            <div key={i} className="bg-white p-6 rounded-lg shadow animate-pulse h-28">
              <div className="h-full bg-gray-200 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <h1 className="font-heading text-2xl font-bold mb-4">Product Transformation Dashboard</h1>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-red-600 mb-4">{error}</p>
          <button
            onClick={fetchData}
            className="px-4 py-2 rounded-lg bg-primary text-white hover:opacity-90"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const kpiCards = [
    {
      title: 'Harvest Quantity',
      value: formatWeight(kpis?.harvestQuantity),
      icon: Package,
      color: 'bg-orange-500',
      hint: 'Plants harvest events',
    },
    {
      title: 'Active Batches',
      value: kpis?.activeBatches ?? 0,
      icon: TrendingUp,
      color: 'bg-green-500',
    },
    {
      title: 'Production Output',
      value: formatNumber(kpis?.productionOutput),
      icon: Factory,
      color: 'bg-blue-500',
    },
    {
      title: 'Quality Pass Rate',
      value: formatPercentage(kpis?.qualityPassRate),
      icon: CheckCircle,
      color: 'bg-emerald-500',
      hint: `Target: ${kpis?.qualityTargetStatus ?? 'n/a'}`,
    },
    {
      title: 'Equipment Utilization',
      value: formatPercentage(kpis?.equipmentUtilization),
      icon: Factory,
      color: 'bg-indigo-500',
    },
    {
      title: 'Energy Consumption',
      value: `${formatNumber(kpis?.energyConsumption)} kWh/kg`,
      icon: Zap,
      color: 'bg-yellow-500',
    },
    {
      title: 'Grade A Share',
      value: formatPercentage(kpis?.gradeAPercentage),
      icon: CheckCircle,
      color: 'bg-purple-500',
    },
    {
      title: 'Water Consumption',
      value: `${formatNumber(kpis?.waterConsumption)} L/kg`,
      icon: Droplets,
      color: 'bg-cyan-500',
    },
  ];

  return (
    <div className="p-6">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-6">
        <div>
          <h1 className="font-heading text-2xl font-bold">Product Transformation Dashboard</h1>
          <p className="text-sm text-gray-500">
            Period: {kpis?.period ?? 'n/a'} ({kpis?.startDate ?? '?'} → {kpis?.endDate ?? '?'})
          </p>
        </div>
        <span
          className={`px-3 py-1 rounded-full text-sm font-medium ${
            DEMO_DATA_ENABLED ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800'
          }`}
        >
          {DEMO_DATA_ENABLED
            ? 'Data source: DEMO dataset (Plants format)'
            : 'Data source: Plants harvest events'}
        </span>
      </div>

      {DEMO_DATA_ENABLED ? (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 mb-6 text-sm">
          Demo dataset: synthetic Product Transformation records (LOT-DEMO batches), not real Plants
          harvests. See db/demo/pt_demo_seed.sql.
        </div>
      ) : null}

      {/* KPI grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {kpiCards.map((card) => (
          <KpiCard key={card.title} {...card} />
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard title="Harvest Quantity Trend" subtitle="From Plants harvest events (kg)">
          {hasRows(charts.harvestTrend) ? (
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={charts.harvestTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Area
                  type="monotone"
                  dataKey="quantity"
                  name="Harvest quantity (kg)"
                  stroke="#0a8276"
                  fill="#0a8276"
                  fillOpacity={0.2}
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="No harvest trend data for this period." />
          )}
        </ChartCard>

        <ChartCard title="Production Output" subtitle="Output per week">
          {hasRows(charts.productionOutput) ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={charts.productionOutput}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="output" name="Production output" fill="#4caf50" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="No production output data for this period." />
          )}
        </ChartCard>

        <ChartCard title="Quality Pass Rate Trend" subtitle="QC checkpoints (%)">
          {hasRows(charts.qualityTrend) ? (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={charts.qualityTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis domain={[0, 100]} />
                <Tooltip />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="passRate"
                  name="Pass rate (%)"
                  stroke="#ff7300"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="No quality trend data for this period." />
          )}
        </ChartCard>

        <ChartCard title="Equipment Utilization" subtitle="Per equipment (%)">
          {hasRows(charts.equipmentUtilization) ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={charts.equipmentUtilization} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" domain={[0, 100]} />
                <YAxis dataKey="equipment" type="category" width={120} />
                <Tooltip />
                <Legend />
                <Bar dataKey="utilization" name="Utilization (%)" fill="#1e88e5" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="No equipment data available." />
          )}
        </ChartCard>

        <ChartCard title="Energy Consumption Breakdown" subtitle="Grid vs solar vs generator">
          {hasRows(charts.energyBreakdown) ? (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={charts.energyBreakdown}
                  cx="50%"
                  cy="50%"
                  outerRadius={90}
                  dataKey="value"
                  nameKey="name"
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                >
                  {charts.energyBreakdown.map((entry, index) => (
                    <Cell key={`cell-${entry.name}-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="No energy breakdown data available." />
          )}
        </ChartCard>
      </div>
    </div>
  );
};

export default Dashboard;
