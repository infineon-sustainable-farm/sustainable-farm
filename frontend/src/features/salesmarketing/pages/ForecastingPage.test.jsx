// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { justAfterLocalMidnight } from '../../../test/nearMidnight';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ForecastingPage from './ForecastingPage';

const api = vi.hoisted(() => ({
  getDemandForecasts: vi.fn(),
  getForecastModelRuns: vi.fn(),
  getProducts: vi.fn(),
  getOrders: vi.fn(),
  getOrderItems: vi.fn(),
  createDemandForecast: vi.fn(),
  updateDemandForecast: vi.fn(),
  deleteDemandForecast: vi.fn(),
  createForecastModelRun: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

const forecasts = [
  { forecastId: 1, productId: 3, forecastMonth: '2026-06-15', scenario: 'base', forecastedVolumeT: 20, capacityRisk: 'low' },
  { forecastId: 2, productId: 3, forecastMonth: '2026-07-15', scenario: 'base', forecastedVolumeT: 25, capacityRisk: 'high' },
];

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getDemandForecasts.mockResolvedValue(forecasts);
  api.getForecastModelRuns.mockResolvedValue([]);
  api.getProducts.mockResolvedValue([{ productId: 3, name: 'Mango', variety: 'Amelie' }]);
  api.getOrders.mockResolvedValue([{ orderId: 9, orderDate: '2026-06-02' }]);
  api.getOrderItems.mockResolvedValue([{ orderId: 9, productId: 3, quantityKg: 18000 }]);
  api.createDemandForecast.mockResolvedValue({});
  api.deleteDemandForecast.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <ForecastingPage />
    </MemoryRouter>
  );
}

describe('ForecastingPage', () => {
  it('shows the monthly rows with their growth and risk, and the accuracy of the base forecasts', async () => {
    renderPage();
    expect(await screen.findByRole('table', { name: 'Monthly Forecast Data' })).toBeInTheDocument();
    expect(screen.getByText('+25.0%')).toBeInTheDocument();
    expect(screen.getByText('High Risk')).toBeInTheDocument();
    const accuracy = screen.getByRole('table', { name: 'Forecast Accuracy' });
    expect(within(accuracy).getByText('18.0 MT')).toBeInTheDocument();
    expect(within(accuracy).getByText('90.0%')).toBeInTheDocument();
  });

  it('creates a forecast from the Add Forecast dialog', async () => {
    renderPage();
    await screen.findByRole('table', { name: 'Monthly Forecast Data' });
    fireEvent.click(screen.getByRole('button', { name: 'Add Forecast' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Forecast' });
    fireEvent.change(within(dialog).getByLabelText(/Volume \(t\)/), { target: { value: '30' } });
    fireEvent.change(within(dialog).getByLabelText('Lower'), { target: { value: '27' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(api.createDemandForecast).toHaveBeenCalledTimes(1));
    expect(api.createDemandForecast).toHaveBeenCalledWith(
      expect.objectContaining({ productId: 3, scenario: 'base', forecastedVolumeT: 30, lowerBoundT: 27, upperBoundT: null })
    );
    expect(await screen.findByText('Forecast created.')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting a forecast', async () => {
    renderPage();
    await screen.findByRole('table', { name: 'Monthly Forecast Data' });
    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0]);
    const dialog = screen.getByRole('dialog', { name: 'Delete forecast?' });
    expect(within(dialog).getByText(/Are you sure you want to delete the forecast for .* \(base\)\?/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.deleteDemandForecast).toHaveBeenCalledWith(1));
    expect(await screen.findByText('Forecast #1 deleted.')).toBeInTheDocument();
  });

  it('defaults the forecast month to the local month just after midnight', async () => {
    const restore = justAfterLocalMidnight();
    try {
      renderPage();
      await screen.findByRole('table', { name: 'Monthly Forecast Data' });
      fireEvent.click(screen.getByRole('button', { name: 'Add Forecast' }));
      const dialog = screen.getByRole('dialog', { name: 'Add Forecast' });
      fireEvent.change(within(dialog).getByLabelText(/Volume \(t\)/), { target: { value: '30' } });
      fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));
      await waitFor(() =>
        expect(api.createDemandForecast).toHaveBeenCalledWith(expect.objectContaining({ forecastMonth: '2026-04-01' }))
      );
    } finally {
      restore();
    }
  });
});
