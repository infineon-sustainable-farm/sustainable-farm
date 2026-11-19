// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { justAfterLocalMidnight } from '../testing/nearMidnight';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
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

const varietyApi = vi.hoisted(() => ({ getVarietyReferences: vi.fn() }));

vi.mock('../api/api', () => ({ api }));
vi.mock('../api/varietyApi', () => ({ varietyApi }));

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset().mockResolvedValue([]));
  varietyApi.getVarietyReferences.mockReset().mockResolvedValue([]);
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

function Where() {
  return <p data-testid="where">{useLocation().pathname}</p>;
}

function renderInModule() {
  return render(
    <MemoryRouter initialEntries={['/salesmarketing']}>
      <Routes>
        <Route path="/salesmarketing" element={<DashboardPage />} />
        <Route path="*" element={<Where />} />
      </Routes>
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

  it('shows the harvest months of the product varieties from the plants catalogue', async () => {
    varietyApi.getVarietyReferences.mockResolvedValue([
      { varietyKey: 'KENT', varietyName: 'Kent', harvestStartMonth: 4, harvestEndMonth: 5 },
      { varietyKey: 'KEITT', varietyName: 'Keitt', harvestStartMonth: 5, harvestEndMonth: 7 },
    ]);
    renderPage();
    expect(await screen.findByText('April - May')).toBeInTheDocument();
  });

  it('shows no harvest months without a catalogue entry for the products', async () => {
    renderPage();
    expect(await screen.findByText('No harvest months known for the products yet.')).toBeInTheDocument();
    expect(screen.queryByText('March - July')).not.toBeInTheDocument();
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

  it('links View all alerts to the module alerts screen', async () => {
    renderInModule();
    fireEvent.click(await screen.findByRole('link', { name: 'VIEW ALL ALERTS' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/salesmarketing/demand-alerts');
  });

  it.each(['Historical Data', 'Model Settings', 'Seasonal Planning'])('sends the %s shortcut to the module forecasting screen', async (title) => {
    renderInModule();
    fireEvent.click(await screen.findByRole('button', { name: new RegExp(title) }));
    expect(screen.getByTestId('where')).toHaveTextContent('/salesmarketing/forecasting');
  });

  it('reports a full failure, not a partial load, when all eight requests fail', async () => {
    Object.values(api).forEach((mock) => mock.mockRejectedValue(new Error('offline')));
    varietyApi.getVarietyReferences.mockRejectedValue(new Error('offline'));
    renderPage();
    expect(await screen.findByText('Forecasts: offline', { exact: false })).toBeInTheDocument();
    expect(screen.queryByText(/Partial load/)).not.toBeInTheDocument();
  });

  it('reports a partial load when only some requests fail', async () => {
    api.getOrders.mockRejectedValue(new Error('offline'));
    api.getCustomers.mockRejectedValue(new Error('offline'));
    renderPage();
    expect(await screen.findByText(/Partial load - Customers: offline/)).toBeInTheDocument();
  });

  it('defaults the forecast month to the local month just after midnight', async () => {
    const restore = justAfterLocalMidnight();
    try {
      renderPage();
      fireEvent.click(await screen.findByRole('button', { name: 'NEW FORECAST' }));
      const dialog = screen.getByRole('dialog', { name: 'New Forecast' });
      fireEvent.change(within(dialog).getByLabelText(/Volume \(t\)/), { target: { value: '800' } });
      fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));
      await waitFor(() =>
        expect(api.createDemandForecast).toHaveBeenCalledWith(expect.objectContaining({ forecastMonth: '2026-04-01' }))
      );
    } finally {
      restore();
    }
  });
});
