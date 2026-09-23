import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../services/api';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';

const CHART_COLORS = ['#0a8276', '#ffc658', '#4caf50', '#1e88e5', '#8e24aa', '#c62828'];

const getChartData = async () => {
  const [harvestTrend, productionOutput, energyBreakdown, equipmentUtilization, qualityTrend] = await Promise.all([
    dashboardApi.getHarvestTrendChart(),
    dashboardApi.getProductionOutputChart(),
    dashboardApi.getEnergyBreakdownChart(),
    dashboardApi.getEquipmentUtilizationChart(),
    dashboardApi.getQualityTrendChart()
  ]);

  return {
    harvestTrend: harvestTrend.data,
    productionOutput: productionOutput.data,
    energyBreakdown: energyBreakdown.data,
    equipmentUtilization: equipmentUtilization.data,
    qualityTrend: qualityTrend.data
  };
};

// Dashboard Charts Component
function DashboardCharts() {
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchChartData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getChartData();
      setChartData(data);
    } catch (err) {
      console.error('Error fetching chart data:', err);
      setError('Failed to load chart data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChartData();
  }, []);

  if (loading) {
    return (
      <div className="dashboard-charts">
        <div className="chart-loading-state">
          <div className="spinner"></div>
          <p>Loading chart data...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-charts">
        <div className="chart-error-state">
          <p>{error}</p>
          <button onClick={fetchChartData} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!chartData || Object.keys(chartData).every((key) => chartData[key].length === 0)) {
    return (
      <div className="dashboard-charts">
        <div className="chart-empty-state">
          <p>No chart data available. Please try again later.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-charts">
      <div className="charts-grid">
        {/* Harvest Quantity Trend */}
        <div className="chart-card">
          <h3>Harvest Quantity Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData.harvestTrend || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="quantity"
                stroke="#8884d8"
                strokeWidth={2}
                name="Harvest Quantity (kg)"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Production Output Comparison */}
        <div className="chart-card">
          <h3>Production Output Comparison</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData.productionOutput || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="period" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="output" fill="#82ca9d" name="Production Output (kg)" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Energy Consumption Breakdown */}
        <div className="chart-card">
          <h3>Energy Consumption Breakdown</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={chartData.energyBreakdown || []}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {(chartData.energyBreakdown || []).map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Equipment Utilization */}
        <div className="chart-card">
          <h3>Equipment Utilization</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData.equipmentUtilization || []} layout="horizontal">
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" />
              <YAxis dataKey="equipment" type="category" width={100} />
              <Tooltip />
              <Legend />
              <Bar dataKey="utilization" fill="#ffc658" name="Utilization %" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Quality Pass Rate Trend */}
        <div className="chart-card">
          <h3>Quality Pass Rate Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData.qualityTrend || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="passRate"
                stroke="#ff7300"
                strokeWidth={2}
                name="Quality Pass Rate (%)"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}

export default DashboardCharts;
