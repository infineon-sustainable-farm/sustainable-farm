// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DeliveryPage from './DeliveryPage';

const api = vi.hoisted(() => ({ getShipments: vi.fn(), getShipmentEvents: vi.fn() }));

vi.mock('../api/api', () => ({ api }));

const shipments = [
  { shipmentId: 1, shipmentCode: 'SHP-A', origin: 'Abidjan', destination: 'Hamburg', status: 'IN_TRANSIT', vesselName: 'Aurora', voyageNumber: 'V12', containerCount: 2, containerType: 'reefer', cargoWeightKg: 24000, currentTempC: 8 },
  { shipmentId: 2, shipmentCode: 'SHP-B', origin: 'Dakar', destination: 'Rotterdam', status: 'DELIVERED' },
];

beforeEach(() => {
  api.getShipments.mockReset().mockResolvedValue(shipments);
  api.getShipmentEvents.mockReset().mockResolvedValue([
    { eventId: 1, shipmentId: 1, step: 'Loaded', eventTime: '2026-03-01T08:00:00' },
    { eventId: 2, shipmentId: 1, step: 'At sea', eventTime: '2026-03-03T08:00:00' },
  ]);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <DeliveryPage />
    </MemoryRouter>
  );
}

describe('DeliveryPage', () => {
  it('shows the first shipment with its cargo and timeline', async () => {
    renderPage();
    expect(await screen.findByText('Vessel: Aurora (V12)')).toBeInTheDocument();
    expect(screen.getByText('2 x reefer / 24,000 kg')).toBeInTheDocument();
    expect(screen.getByText('8.0°C')).toBeInTheDocument();
    expect(screen.getByText('Loaded')).toBeInTheDocument();
    expect(screen.getByText('At sea')).toBeInTheDocument();
  });

  it('filters the shipments by search and status, and resets with View all', async () => {
    renderPage();
    await screen.findByText('SHP-A');
    fireEvent.change(screen.getByPlaceholderText('Search by ID or destination...'), { target: { value: 'rotterdam' } });
    expect(screen.queryByText('SHP-A')).not.toBeInTheDocument();
    expect(screen.getByText('SHP-B')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('Search by ID or destination...'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'IN_TRANSIT' } });
    expect(screen.getByText('SHP-A')).toBeInTheDocument();
    expect(screen.queryByText('SHP-B')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'View All 2 Shipments' }));
    const list = screen.getByRole('button', { name: /SHP-B/ });
    expect(within(list).getByText('DELIVERED')).toBeInTheDocument();
  });

  it('shows the empty value, not an invented commodity or condition, when the shipment has none', async () => {
    renderPage();
    await screen.findByText('Vessel: Aurora (V12)');
    const cargo = screen.getByRole('heading', { name: 'Cargo Details' }).parentElement;
    expect(within(cargo).queryByText('Dried Mango')).not.toBeInTheDocument();
    expect(within(cargo).queryByText('OPTIMAL')).not.toBeInTheDocument();
    expect(within(cargo).getAllByText('-')).toHaveLength(2);
  });

  it('shows the commodity and condition the API gives', async () => {
    api.getShipments.mockResolvedValue([{ ...shipments[0], commodityDescription: 'Cashew kernels', tempStatus: 'STABLE' }]);
    renderPage();
    const cargo = (await screen.findByRole('heading', { name: 'Cargo Details' })).parentElement;
    expect(await within(cargo).findByText('Cashew kernels')).toBeInTheDocument();
    expect(within(cargo).getByText('STABLE')).toBeInTheDocument();
  });
});
