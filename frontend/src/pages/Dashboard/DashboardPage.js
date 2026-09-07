import React from 'react';
import { dashboardApi } from '../../services/api';
import Card from '../../components/common/Card';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import './DashboardPage.css';

const DashboardPage = () => {
  const [kpiData, setKpiData] = React.useState(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  React.useEffect(() => {
    fetchDashboardKPIs();
  }, []);

  const fetchDashboardKPIs = async () => {
    try {
      setLoading(true);
      const response = await dashboardApi.getDashboardKPIs();
      setKpiData(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching dashboard KPIs:', err);
      setError('Failed to load dashboard data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchDashboardKPIs} />;

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <h1>Product Transformation Dashboard</h1>
        <p className="dashboard-subtitle">BIT × Infineon Excellence Program</p>
      </div>

      <div className="dashboard-grid">
        <KPICard
          label="Harvest Quantity"
          value={`${kpiData?.harvestQuantity || 0} kg`}
          trend="+12% vs last week"
          trendDirection="up"
        />
        <KPICard
          label="Active Batches"
          value={kpiData?.activeBatches || 0}
          trend="Currently in production"
        />
        <KPICard
          label="Production Output"
          value={`${kpiData?.productionOutput || 0} kg`}
          trend="+8% efficiency"
          trendDirection="up"
        />
        <KPICard
          label="Quality Pass Rate"
          value={`${kpiData?.qualityPassRate || 0}%`}
          trend="Above target"
          trendDirection="up"
        />
        <KPICard
          label="Water Consumption"
          value={`${kpiData?.waterConsumption || 0} L/kg`}
          trend="On target"
        />
        <KPICard
          label="Energy Consumption"
          value={`${kpiData?.energyConsumption || 0} kWh/kg`}
          trend="On target"
        />
        <KPICard
          label="Solar Energy Share"
          value={`${kpiData?.solarEnergyShare || 0}%`}
          trend="Eco-friendly"
        />
        <KPICard
          label="Equipment Utilization"
          value={`${kpiData?.equipmentUtilization || 0}%`}
          trend="Operational"
        />
        <KPICard
          label="Grade A Percentage"
          value={`${kpiData?.gradeAPercentage || 0}%`}
          trend={kpiData?.qualityTargetStatus === 'above' ? 'Above target' : 'Below target'}
          trendDirection={kpiData?.qualityTargetStatus === 'above' ? 'up' : 'down'}
        />
        <KPICard
          label="Total Energy Today"
          value={`${kpiData?.totalEnergyToday || 0} kWh`}
          trend="Real-time"
        />
        <KPICard
          label="Production Efficiency"
          value={`${kpiData?.productionEfficiency || 0}%`}
          trend="Optimizing"
        />
      </div>

      <div className="dashboard-footer">
        <p className="update-time">Last updated: {kpiData?.generatedAt || 'Unknown'}</p>
        <button onClick={fetchDashboardKPIs} className="refresh-button">Refresh Data</button>
      </div>
    </div>
  );
};

function KPICard({ label, value, trend, trendDirection }) {
  return (
    <Card className="kpi-card">
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      <div className={`kpi-trend ${trendDirection || ''}`}>
        {trendDirection === 'up' && <span className="trend-icon">↑</span>}
        {trendDirection === 'down' && <span className="trend-icon">↓</span>}
        {trend}
      </div>
    </Card>
  );
}

export default DashboardPage;