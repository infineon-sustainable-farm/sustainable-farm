// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { dashboardApi } from '../api/dashboardApi';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { batchApi } from '../api/batchApi';
import { washSortApi } from '../api/washSortApi';
import { dryingApi } from '../api/dryingApi';

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

vi.mock('../api/rawIntakeApi', () => ({
  rawIntakeApi: { getAllRawIntakes: vi.fn() },
}));

vi.mock('../api/batchApi', () => ({
  batchApi: { getAllBatches: vi.fn() },
}));

vi.mock('../api/washSortApi', () => ({
  washSortApi: { getAllWashSortRecords: vi.fn() },
}));

vi.mock('../api/dryingApi', () => ({
  dryingApi: { getAllDryingRuns: vi.fn() },
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
    Bar: Container,
    LineChart: Container,
    Line: Empty,
    ComposedChart: Container,
    PieChart: Container,
    Pie: Container,
    Cell: Empty,
    XAxis: Empty,
    YAxis: Empty,
    CartesianGrid: Empty,
    Tooltip: Empty,
    Legend: Empty,
    ReferenceLine: Empty,
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

const RECORDS = {
  intakes: [
    { intakeId: 'I-1', batchId: 'B-001', receivedGrade: 'A', receivedQuantityKg: 120 },
    { intakeId: 'I-2', batchId: 'B-002', receivedGrade: 'B', receivedQuantityKg: 80 },
  ],
  batches: [
    { batchId: 'B-001', currentStatus: 'WASHING' },
    { batchId: 'B-002', currentStatus: 'DRYING' },
  ],
  wash: [{ batchId: 'B-001', inputQuantityKg: 120, outputQuantityKg: 100, yieldPercentage: 83.3 }],
  drying: [{ batchId: 'B-001', durationHours: 10, energyUsageKwh: 30 }],
};

const mockCharts = () => {
  dashboardApi.getHarvestTrendData.mockResolvedValue(CHARTS.harvest);
  dashboardApi.getProductionOutputData.mockResolvedValue(CHARTS.production);
  dashboardApi.getEnergyBreakdownData.mockResolvedValue(CHARTS.energy);
  dashboardApi.getEquipmentUtilizationData.mockResolvedValue(CHARTS.equipment);
  dashboardApi.getQualityTrendData.mockResolvedValue(CHARTS.quality);
};

const mockRecords = () => {
  rawIntakeApi.getAllRawIntakes.mockResolvedValue(RECORDS.intakes);
  batchApi.getAllBatches.mockResolvedValue(RECORDS.batches);
  washSortApi.getAllWashSortRecords.mockResolvedValue(RECORDS.wash);
  dryingApi.getAllDryingRuns.mockResolvedValue(RECORDS.drying);
};

const renderDashboard = () =>
  render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  dashboardApi.getDashboardStats.mockResolvedValue({ ...KPIS });
  mockCharts();
  mockRecords();
});

describe('Dashboard', () => {
  it('renders the KPIs and the reporting period', async () => {
    renderDashboard();

    expect(await screen.findByText('520 kg')).toBeTruthy();
    expect(screen.getByText('3')).toBeTruthy();
    expect(screen.getByText('118.5')).toBeTruthy();
    expect(screen.getByText('92.5%')).toBeTruthy();
    expect(screen.getByText('0.85 kWh/kg')).toBeTruthy();
    expect(screen.getByText('75%')).toBeTruthy();
    expect(screen.getByText('1.25 L/kg')).toBeTruthy();
    expect(screen.getByText(/Target: ON_TARGET/)).toBeTruthy();
    expect(
      screen.getByText(/Period: WEEK \(2026-11-03 → 2026-11-09\)/),
    ).toBeTruthy();
    expect(screen.getByText('Data source: Plants harvest events')).toBeTruthy();
  });

  it('renders the chart cards when data is available', async () => {
    renderDashboard();

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

  it('derives the grade, batch and processing charts from existing records', async () => {
    renderDashboard();

    expect(await screen.findByText('Grade Distribution')).toBeTruthy();
    expect(screen.getByText('Batch Processing Status')).toBeTruthy();
    expect(screen.getByText('Processing Performance')).toBeTruthy();
    expect(screen.getByText(/2 batches in the pipeline/)).toBeTruthy();
    expect(screen.queryByText('No grade distribution data for this period.')).toBeNull();
    expect(screen.queryByText('No batch status data for this period.')).toBeNull();
    expect(screen.queryByText('No processing performance data for this period.')).toBeNull();
  });

  it('renders every chart empty state when the API returns no rows', async () => {
    dashboardApi.getDashboardStats.mockResolvedValue({});
    dashboardApi.getHarvestTrendData.mockResolvedValue([]);
    dashboardApi.getProductionOutputData.mockResolvedValue(null);
    dashboardApi.getEnergyBreakdownData.mockRejectedValueOnce(new Error('chart down'));
    dashboardApi.getEquipmentUtilizationData.mockResolvedValue([]);
    dashboardApi.getQualityTrendData.mockResolvedValue([]);
    rawIntakeApi.getAllRawIntakes.mockResolvedValue([]);
    batchApi.getAllBatches.mockResolvedValue([]);
    washSortApi.getAllWashSortRecords.mockResolvedValue([]);
    dryingApi.getAllDryingRuns.mockResolvedValue([]);

    renderDashboard();

    expect(await screen.findByText('No harvest trend data for this period.')).toBeTruthy();
    expect(screen.getByText('No production output data for this period.')).toBeTruthy();
    expect(screen.getByText('No quality trend data for this period.')).toBeTruthy();
    expect(screen.getByText('No equipment data available.')).toBeTruthy();
    expect(screen.getByText('No energy breakdown data available.')).toBeTruthy();
    expect(screen.getByText('No grade distribution data for this period.')).toBeTruthy();
    expect(screen.getByText('No batch status data for this period.')).toBeTruthy();
    expect(screen.getByText('No processing performance data for this period.')).toBeTruthy();
    // A failed chart does not blank the page or trigger the KPI error state.
    expect(screen.queryByText('Retry')).toBeNull();
    expect(screen.getByText('0')).toBeTruthy();
  });

  it('shows an error state when the KPI call fails and retries it', async () => {
    dashboardApi.getDashboardStats.mockRejectedValueOnce(new Error('KPI down'));

    renderDashboard();

    expect(
      await screen.findByText('Failed to load dashboard KPIs. Please try again later.'),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByText('520 kg')).toBeTruthy();
    await waitFor(() =>
      expect(dashboardApi.getDashboardStats).toHaveBeenCalledTimes(2),
    );
  });

  it('renders a 403 on the KPI call as an error state', async () => {
    dashboardApi.getDashboardStats.mockRejectedValueOnce(
      Object.assign(new Error('Forbidden'), { status: 403 }),
    );

    renderDashboard();

    expect(await screen.findByText(/Failed to load dashboard KPIs/)).toBeTruthy();
  });
});
