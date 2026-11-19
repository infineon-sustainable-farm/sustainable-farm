// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DemandAlertsPage from './DemandAlertsPage';

const api = vi.hoisted(() => ({
  getAlerts: vi.fn(),
  getDemandForecasts: vi.fn(),
  getPricingHistory: vi.fn(),
  createAlert: vi.fn(),
  updateAlert: vi.fn(),
  deleteAlert: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

const alert = {
  alertId: 3,
  alertCode: 'ALT-0003',
  alertType: 'High Demand',
  severity: 'critical',
  message: 'Orders above forecast',
  region: 'Germany',
  origin: 'Burkina Faso',
  status: 'open',
};

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getAlerts.mockResolvedValue([alert]);
  api.getDemandForecasts.mockResolvedValue([{ productId: 1, forecastedVolumeT: 12, scenario: 'Base' }]);
  api.getPricingHistory.mockResolvedValue([{ productId: 1, priceEurPerKg: 2.5, priceDate: '2026-01-01' }]);
  api.createAlert.mockResolvedValue({});
  api.updateAlert.mockResolvedValue({});
  api.deleteAlert.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <DemandAlertsPage />
    </MemoryRouter>
  );
}

describe('DemandAlertsPage', () => {
  it('shows the KPIs, the counted filters and the alert', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Dried Mango - Germany' })).toBeInTheDocument();
    expect(screen.getByText('12 t')).toBeInTheDocument();
    expect(screen.getByText('€30,000')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'High Priority (1)' })).toBeInTheDocument();
    expect(screen.getByText('EU - Germany')).toBeInTheDocument();
  });

  it('creates an alert from the New Alert dialog', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Dried Mango - Germany' });
    fireEvent.click(screen.getByRole('button', { name: 'New Alert' }));
    const dialog = screen.getByRole('dialog', { name: 'New Alert' });
    fireEvent.change(within(dialog).getByLabelText(/Message/), { target: { value: 'Stock running low' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(api.createAlert).toHaveBeenCalledTimes(1));
    expect(api.createAlert).toHaveBeenCalledWith(
      expect.objectContaining({ alertType: 'High Demand', severity: 'critical', message: 'Stock running low', status: 'open' })
    );
    expect(await screen.findByText('Alert created.')).toBeInTheDocument();
  });

  it('invents no region nor origin for an alert that has none', async () => {
    api.getAlerts.mockResolvedValue([{ ...alert, region: null, origin: null }]);
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Edit' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit Alert #3' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Update' }));

    await waitFor(() => expect(api.updateAlert).toHaveBeenCalledTimes(1));
    expect(api.updateAlert).toHaveBeenCalledWith(3, expect.objectContaining({ region: null, origin: null }));
  });

  it('marks an alert resolved', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Review Production' }));
    await waitFor(() =>
      expect(api.updateAlert).toHaveBeenCalledWith(3, expect.objectContaining({ status: 'resolved' }))
    );
    expect(await screen.findByText('ALT-0003 marked resolved.')).toBeInTheDocument();
  });

  it('asks for confirmation before dismissing an alert', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Dried Mango - Germany' });
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));
    const dialog = screen.getByRole('dialog', { name: 'Dismiss alert?' });
    expect(within(dialog).getByText(/dismiss alert ALT-0003\?/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.deleteAlert).toHaveBeenCalledWith(3));
    expect(await screen.findByText('ALT-0003 deleted.')).toBeInTheDocument();
  });

  it('shows the count of active high priority alerts, not a Germany base case it has no data for', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Dried Mango - Germany' });
    expect(screen.queryByText('Germany Base Case')).not.toBeInTheDocument();
    expect(screen.getByText('High Priority Alerts').previousElementSibling).toHaveTextContent(/^1$/);
  });
});
