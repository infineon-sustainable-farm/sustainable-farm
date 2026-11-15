// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import Dashboard from './Dashboard';
import { dashboardApi } from '../api/dashboardApi';

vi.mock('../api/dashboardApi', () => ({
  dashboardApi: {
    getDashboardStats: vi.fn(),
    getHarvestTrendData: vi.fn(),
    getProductionOutputData: vi.fn(),
    getEnergyBreakdownData: vi.fn(),
    getEquipmentUtilizationData: vi.fn(),
    getQualityTrendData: vi.fn(),
  },
}));

// Recharts needs a real layout engine; the dashboard assertions only need to prove
// which chart or empty state is mounted, so the chart primitives are stubbed.
vi.mock('recharts', () => {
  const Container = ({ children }) => <div>{children}</div>;
  const Empty = () => null;
  return {
    AreaChart: Container,
    Area: Empty,
    BarChart: Container,
    Bar: Empty,
    LineChart: Container,
    Line: Empty,
    PieChart: Container,
    Pie: Container,
    Cell: Empty,
    XAxis: Empty,
    YAxis: Empty,
    CartesianGrid: Empty,
    Tooltip: Empty,
    Legend: Empty,
    ResponsiveContainer: Container,
  };
});

afterEach(cleanup);

const KPIS = {
  harvestQuantity: 520,
  activeBatches: 3,
  productionOutput: 118.5,
  qualityPassRate: 92.5,
  waterConsumption: 1.25,
  energyConsumption: 0.85,
  solarEnergyShare: 42,
  equipmentUtilization: 67,
  gradeAPercentage: 75,
  qualityTargetStatus: 'ON_TARGET',
  period: 'WEEK',
  startDate: '2026-11-03',
  endDate: '2026-11-09',
};

const CHARTS = {
  harvest: [{ date: '2026-11-03', quantity: 120 }],
  production: [{ period: 'W45', output: 90 }],
  energy: [{ name: 'Grid', value: 60 }],
  equipment: [{ equipment: 'Solar dryer 1', utilization: 67 }],
  quality: [{ date: '2026-11-03', passRate: 92.5 }],
};

const mockCharts = () => {
  dashboardApi.getHarvestTrendData.mockResolvedValue(CHARTS.harvest);
  dashboardApi.getProductionOutputData.mockResolvedValue(CHARTS.production);
  dashboardApi.getEnergyBreakdownData.mockResolvedValue(CHARTS.energy);
  dashboardApi.getEquipmentUtilizationData.mockResolvedValue(CHARTS.equipment);
  dashboardApi.getQualityTrendData.mockResolvedValue(CHARTS.quality);
};

beforeEach(() => {
  vi.clearAllMocks();
  dashboardApi.getDashboardStats.mockResolvedValue({ ...KPIS });
  mockCharts();
});

describe('Dashboard', () => {
  it('renders the KPIs and the reporting period', async () => {
    render(<Dashboard />);

    expect(await screen.findByText('520.00 kg')).toBeTruthy();
    expect(screen.getByText('3')).toBeTruthy();
    expect(screen.getByText('118.50')).toBeTruthy();
    expect(screen.getByText('92.50%')).toBeTruthy();
    expect(screen.getByText('0.85 kWh/kg')).toBeTruthy();
    expect(screen.getByText('75.00%')).toBeTruthy();
    expect(screen.getByText('1.25 L/kg')).toBeTruthy();
    expect(screen.getByText(/Target: ON_TARGET/)).toBeTruthy();
    expect(
      screen.getByText(/Period: WEEK \(2026-11-03 → 2026-11-09\)/),
    ).toBeTruthy();
    expect(screen.getByText('Data source: Plants harvest events')).toBeTruthy();
  });

  it('renders the chart cards when data is available', async () => {
    render(<Dashboard />);

    expect(await screen.findByText('Harvest Quantity Trend')).toBeTruthy();
    expect(screen.getAllByText('Production Output').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('Quality Pass Rate Trend')).toBeTruthy();
    expect(screen.getAllByText('Equipment Utilization').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('Energy Consumption Breakdown')).toBeTruthy();
    expect(screen.queryByText('No harvest trend data for this period.')).toBeNull();
    expect(screen.queryByText('No production output data for this period.')).toBeNull();
    expect(screen.queryByText('No quality trend data for this period.')).toBeNull();
    expect(screen.queryByText('No equipment data available.')).toBeNull();
    expect(screen.queryByText('No energy breakdown data available.')).toBeNull();
  });

  it('renders every chart empty state when the API returns no rows', async () => {
    dashboardApi.getDashboardStats.mockResolvedValue({});
    dashboardApi.getHarvestTrendData.mockResolvedValue([]);
    dashboardApi.getProductionOutputData.mockResolvedValue(null);
    dashboardApi.getEnergyBreakdownData.mockRejectedValueOnce(new Error('chart down'));
    dashboardApi.getEquipmentUtilizationData.mockResolvedValue([]);
    dashboardApi.getQualityTrendData.mockResolvedValue([]);

    render(<Dashboard />);

    expect(await screen.findByText('No harvest trend data for this period.')).toBeTruthy();
    expect(screen.getByText('No production output data for this period.')).toBeTruthy();
    expect(screen.getByText('No quality trend data for this period.')).toBeTruthy();
    expect(screen.getByText('No equipment data available.')).toBeTruthy();
    expect(screen.getByText('No energy breakdown data available.')).toBeTruthy();
    // A failed chart does not blank the page or trigger the KPI error state.
    expect(screen.queryByText('Retry')).toBeNull();
    expect(screen.getByText('0')).toBeTruthy();
  });

  it('shows an error state when the KPI call fails and retries it', async () => {
    dashboardApi.getDashboardStats.mockRejectedValueOnce(new Error('KPI down'));

    render(<Dashboard />);

    expect(
      await screen.findByText('Failed to load dashboard KPIs. Please try again later.'),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByText('520.00 kg')).toBeTruthy();
    await waitFor(() =>
      expect(dashboardApi.getDashboardStats).toHaveBeenCalledTimes(2),
    );
  });

  it('renders a 403 on the KPI call as an error state', async () => {
    dashboardApi.getDashboardStats.mockRejectedValueOnce(
      Object.assign(new Error('Forbidden'), { status: 403 }),
    );

    render(<Dashboard />);

    expect(await screen.findByText(/Failed to load dashboard KPIs/)).toBeTruthy();
  });
});
