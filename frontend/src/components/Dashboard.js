import React, { useState, useEffect } from 'react';
import { dashboardApi } from '../services/api';
import './Dashboard.css';

function Dashboard() {
  const [kpiData, setKpiData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
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

  if (loading) {
    return (
      <div className="dashboard-container">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading dashboard data...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-container">
        <div className="error-state">
          <p>{error}</p>
          <button onClick={fetchDashboardKPIs} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>Product Transformation Dashboard</h1>
        <p className="dashboard-subtitle">BIT × Infineon Excellence Program</p>
      </div>

      <div className="dashboard-grid">
        {/* Operational KPIs */}
        <KPICard
          label="Harvest Quantity"
          value={`${kpiData.harvestQuantity || 0} kg`}
          trend="+12% vs last week"
          trendDirection="up"
        />
        <KPICard
          label="Active Batches"
          value={kpiData.activeBatches || 0}
          trend="Currently in production"
        />
        <KPICard
          label="Production Output"
          value={`${kpiData.productionOutput || 0} kg`}
          trend="+8% efficiency"
          trendDirection="up"
        />
        <KPICard
          label="Quality Pass Rate"
          value={`${kpiData.qualityPassRate || 0}%`}
          trend="Above target"
          trendDirection="up"
        />

        {/* Resource KPIs */}
        <KPICard
          label="Water Consumption"
          value={`${kpiData.waterConsumption || 0} L/kg`}
          trend="On target"
        />
        <KPICard
          label="Energy Consumption"
          value={`${kpiData.energyConsumption || 0} kWh/kg`}
          trend="On target"
        />
        <KPICard
          label="Solar Energy Share"
          value={`${kpiData.solarEnergyShare || 0}%`}
          trend="Eco-friendly"
        />
        <KPICard
          label="Equipment Utilization"
          value={`${kpiData.equipmentUtilization || 0}%`}
          trend="Operational"
        />

        {/* Quality KPIs */}
        <KPICard
          label="Grade A Percentage"
          value={`${kpiData.gradeAPercentage || 0}%`}
          trend={kpiData.qualityTargetStatus === 'above' ? 'Above target' : 'Below target'}
          trendDirection={kpiData.qualityTargetStatus === 'above' ? 'up' : 'down'}
        />

        {/* Production Analytics KPIs */}
        <KPICard
          label="Total Energy Today"
          value={`${kpiData.totalEnergyToday || 0} kWh`}
          trend="Real-time"
        />
        <KPICard
          label="Production Efficiency"
          value={`${kpiData.productionEfficiency || 0}%`}
          trend="Optimizing"
        />
      </div>

      <div className="dashboard-footer">
        <p className="update-time">
          Last updated: {kpiData.generatedAt || 'Unknown'}
        </p>
        <button onClick={fetchDashboardKPIs} className="refresh-button">
          Refresh Data
        </button>
      </div>
    </div>
  );
}

function KPICard({ label, value, trend, trendDirection }) {
  return (
    <div className="kpi-card">
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      <div className={`kpi-trend ${trendDirection || ''}`}>
        {trendDirection === 'up' && <span className="trend-icon">↑</span>}
        {trendDirection === 'down' && <span className="trend-icon">↓</span>}
        {trend}
      </div>
    </div>
  );
}

export default Dashboard;