// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CampaignsPage from './CampaignsPage';
import CustomersPage from './CustomersPage';
import DashboardPage from './DashboardPage';
import DeliveryPage from './DeliveryPage';
import DemandAlertsPage from './DemandAlertsPage';
import DemandReportsPage from './DemandReportsPage';
import ForecastingPage from './ForecastingPage';
import PricingPage from './PricingPage';
import SalesChannelsPage from './SalesChannelsPage';

const apiMocks = vi.hoisted(() => ({}));

vi.mock('../api/api', () => ({
  api: new Proxy(
    {},
    {
      get(_target, method) {
        if (typeof method !== 'string') return undefined;
        apiMocks[method] ??= vi.fn().mockResolvedValue([]);
        return apiMocks[method];
      },
    }
  ),
}));

afterEach(cleanup);

beforeEach(() => {
  Object.values(apiMocks).forEach((mock) => mock.mockReset().mockResolvedValue([]));
});

const pages = [
  { name: 'Campaigns', heading: 'Campaign Hub', apiMethod: 'getCampaigns', Component: CampaignsPage },
  { name: 'Customers', heading: 'Customers', apiMethod: 'getCustomers', Component: CustomersPage },
  { name: 'Dashboard', heading: 'Overview', apiMethod: 'getDemandForecasts', Component: DashboardPage },
  { name: 'Delivery', heading: 'Delivery Tracking', apiMethod: 'getShipments', Component: DeliveryPage },
  { name: 'Demand alerts', heading: 'Demand Alerts Center', apiMethod: 'getAlerts', Component: DemandAlertsPage },
  { name: 'Demand reports', heading: 'Demand Reports', apiMethod: 'getDemandReports', Component: DemandReportsPage },
  { name: 'Forecasting', heading: 'Demand Forecasting', apiMethod: 'getDemandForecasts', Component: ForecastingPage },
  { name: 'Pricing', heading: 'Pricing Model Variables', apiMethod: 'getPricingHistory', Component: PricingPage },
  { name: 'Sales channels', heading: 'Sales Channels', apiMethod: 'getSalesChannels', Component: SalesChannelsPage },
];

function renderPage(Component) {
  return render(
    <MemoryRouter>
      <Component />
    </MemoryRouter>
  );
}

describe('Sales and Marketing pages with the shared API response shape', () => {
  it.each(pages)('$name renders normally with an empty API response', async ({ heading, apiMethod, Component }) => {
    renderPage(Component);

    expect(await screen.findByRole('heading', { name: heading })).toBeTruthy();
    expect(apiMocks[apiMethod]).toHaveBeenCalled();
  });

  it.each(pages)('$name displays errors from the shared client', async ({ apiMethod, Component }) => {
    apiMocks[apiMethod] = vi.fn().mockRejectedValue(
      Object.assign(new Error('Mock API failure'), {
        status: 503,
        data: { message: 'Mock API failure' },
      })
    );

    renderPage(Component);

    expect(await screen.findByText(/Mock API failure/)).toBeTruthy();
  });
});
