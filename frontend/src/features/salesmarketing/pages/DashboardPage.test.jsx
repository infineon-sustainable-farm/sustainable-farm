// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DashboardPage from './DashboardPage';

const api = vi.hoisted(() => ({
  getDemandForecasts: vi.fn(),
  getAlerts: vi.fn(),
  getProductBatches: vi.fn(),
  getProducts: vi.fn(),
  getPricingHistory: vi.fn(),
  getCustomers: vi.fn(),
  getOrders: vi.fn(),
  createDemandForecast: vi.fn(),
  deleteAlert: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset().mockResolvedValue([]));
  api.getProducts.mockResolvedValue([{ productId: 3, name: 'Mango', variety: 'Kent' }]);
  api.getDemandForecasts.mockResolvedValue([
    { forecastId: 1, productId: 3, scenario: 'base', forecastedVolumeT: 1200, lowerBoundT: 1000, upperBoundT: 1500 },
  ]);
  api.getPricingHistory.mockResolvedValue([{ productId: 3, priceDate: '2026-01-01', priceEurPerKg: 2.5 }]);
  api.getAlerts.mockResolvedValue([{ alertId: 9, alertCode: 'AL-9', message: 'Stock low', severity: 'HIGH', status: 'ACTIVE' }]);
  api.getProductBatches.mockResolvedValue([{ batchId: 5, batchCode: 'B-5', stockT: 1500, status: 'processed' }]);
  api.createDemandForecast.mockResolvedValue({});
  api.deleteAlert.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>
  );
}

describe('DashboardPage', () => {
  it('shows the key figures, batches and alerts', async () => {
    renderPage();
    expect(await screen.findByText('1,200 t')).toBeInTheDocument();
    expect(screen.getByText('Range: 1,000 - 1,500 t')).toBeInTheDocument();
    expect(screen.getByText('EUR3.0M')).toBeInTheDocument();
    expect(screen.getByText('Latest price: EUR2.50/kg')).toBeInTheDocument();
    expect(screen.getByText('1 Active')).toBeInTheDocument();
    expect(screen.getByText('B-5')).toBeInTheDocument();
    expect(screen.getByText('Processed')).toBeInTheDocument();
  });

  it('creates a forecast from the New forecast dialog', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'NEW FORECAST' }));
    const dialog = screen.getByRole('dialog', { name: 'New Forecast' });
    fireEvent.change(within(dialog).getByLabelText(/Volume \(t\)/), { target: { value: '800' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(api.createDemandForecast).toHaveBeenCalledTimes(1));
    expect(api.createDemandForecast).toHaveBeenCalledWith(
      expect.objectContaining({ productId: 3, scenario: 'base', forecastedVolumeT: 800, isActual: false, modelVersion: 'v1' })
    );
    expect(await screen.findByText('Forecast created successfully.')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting an alert', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /AL-9/ }));
    const dialog = screen.getByRole('dialog', { name: 'Delete alert?' });
    expect(within(dialog).getByText(/Are you sure you want to delete alert AL-9\?/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.deleteAlert).toHaveBeenCalledWith(9));
    expect(await screen.findByText('Alert AL-9 deleted.')).toBeInTheDocument();
  });
});
