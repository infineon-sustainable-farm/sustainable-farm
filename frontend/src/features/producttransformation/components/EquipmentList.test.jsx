// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import EquipmentList from './EquipmentList';
import { equipmentApi } from '../api/equipmentApi';

vi.mock('../api/equipmentApi', () => ({
  equipmentApi: {
    getAllEquipment: vi.fn(),
    createEquipment: vi.fn(),
    updateEquipment: vi.fn(),
    deleteEquipment: vi.fn(),
  },
}));

afterEach(cleanup);

const EQUIPMENT = [
  {
    equipmentId: 'EQ-1',
    equipmentName: 'Solar dryer 1',
    equipmentType: 'SOLAR_DRYER',
    capacityKgPerHour: 120,
    energyConsumptionKwhPerKg: 0.85,
    location: 'Unit PT',
    maintenanceStatus: 'ACTIVE',
    lastMaintenanceDate: '2026-10-20',
  },
  {
    equipmentId: 'EQ-2',
    equipmentName: 'Washer 1',
    equipmentType: 'WASHING',
    capacityKgPerHour: 80,
    energyConsumptionKwhPerKg: 1.1,
    location: 'Unit PT',
    maintenanceStatus: 'ACTIVE',
    lastMaintenanceDate: '2026-10-05',
  },
  {
    equipmentId: 'EQ-3',
    equipmentName: 'Conditionneuse 1',
    equipmentType: 'PACKAGING',
    capacityKgPerHour: 60,
    energyConsumptionKwhPerKg: 0.95,
    location: 'Unit PT',
    maintenanceStatus: 'MAINTENANCE',
    lastMaintenanceDate: '2026-11-04',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  equipmentApi.getAllEquipment.mockResolvedValue(EQUIPMENT.map((item) => ({ ...item })));
  equipmentApi.createEquipment.mockResolvedValue({});
  equipmentApi.updateEquipment.mockResolvedValue({});
  equipmentApi.deleteEquipment.mockResolvedValue(undefined);
});

describe('EquipmentList', () => {
  it('renders equipment with type, capacity, energy and maintenance status', async () => {
    render(<EquipmentList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('EQ-1')).toBeTruthy();
    expect(within(table).getByText('Solar dryer 1')).toBeTruthy();
    expect(within(table).getByText('Solar dryer')).toBeTruthy();
    expect(within(table).getByText('120 kg/h')).toBeTruthy();
    expect(within(table).getByText('0.85 kWh/kg')).toBeTruthy();
    expect(within(table).getAllByText('Active')).toHaveLength(2);
    expect(within(table).getByText('Maintenance')).toBeTruthy();
  });

  it('filters by type, status and search', async () => {
    render(<EquipmentList />);
    await screen.findByText('EQ-1');

    fireEvent.change(screen.getByLabelText('Filter by type'), {
      target: { value: 'SOLAR_DRYER' },
    });
    expect(screen.queryByText('EQ-2')).toBeNull();
    expect(screen.getByText('EQ-1')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Filter by type'), { target: { value: 'all' } });
    fireEvent.change(screen.getByLabelText('Filter by status'), {
      target: { value: 'MAINTENANCE' },
    });
    expect(screen.getByText('EQ-3')).toBeTruthy();
    expect(screen.queryByText('EQ-1')).toBeNull();

    fireEvent.change(screen.getByLabelText('Filter by status'), { target: { value: 'all' } });
    fireEvent.change(screen.getByLabelText('Search equipment'), {
      target: { value: 'washer' },
    });
    expect(screen.getByText('EQ-2')).toBeTruthy();
    expect(screen.queryByText('EQ-1')).toBeNull();
  });

  it('creates equipment through the API and reloads the list', async () => {
    render(<EquipmentList />);
    await screen.findByText('EQ-1');

    fireEvent.click(screen.getByRole('button', { name: /New Equipment/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Equipment ID'), {
      target: { value: 'EQ-4' },
    });
    fireEvent.change(within(dialog).getByLabelText('Name'), {
      target: { value: 'Tunnel dryer 1' },
    });
    fireEvent.change(within(dialog).getByLabelText('Type'), {
      target: { value: 'TUNNEL_DRYER' },
    });
    fireEvent.change(within(dialog).getByLabelText('Maintenance Status'), {
      target: { value: 'ACTIVE' },
    });
    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), {
      target: { value: '95' },
    });
    fireEvent.change(within(dialog).getByLabelText('Energy (kWh/kg)'), {
      target: { value: '0.9' },
    });
    fireEvent.change(within(dialog).getByLabelText('Location'), {
      target: { value: 'Unit PT' },
    });
    fireEvent.change(within(dialog).getByLabelText('Last Maintenance'), {
      target: { value: '2026-11-09' },
    });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(equipmentApi.createEquipment).toHaveBeenCalledWith({
        equipmentId: 'EQ-4',
        equipmentName: 'Tunnel dryer 1',
        equipmentType: 'TUNNEL_DRYER',
        capacityKgPerHour: 95,
        energyConsumptionKwhPerKg: 0.9,
        location: 'Unit PT',
        maintenanceStatus: 'ACTIVE',
        lastMaintenanceDate: '2026-11-09',
      }),
    );
    await waitFor(() => expect(equipmentApi.getAllEquipment).toHaveBeenCalledTimes(2));
  });

  it('rejects a non-positive capacity', async () => {
    render(<EquipmentList />);
    await screen.findByText('EQ-1');

    fireEvent.click(screen.getByRole('button', { name: /New Equipment/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Equipment ID'), {
      target: { value: 'EQ-4' },
    });
    fireEvent.change(within(dialog).getByLabelText('Name'), {
      target: { value: 'Tunnel dryer 1' },
    });
    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), {
      target: { value: '0' },
    });
    fireEvent.change(within(dialog).getByLabelText('Energy (kWh/kg)'), {
      target: { value: '0.9' },
    });
    // jsdom enforces the native `min` constraint on click, so submit directly to
    // exercise the JavaScript validation backstop.
    fireEvent.submit(dialog.querySelector('form'));

    expect(screen.getByText('Capacity must be greater than 0')).toBeTruthy();
    expect(equipmentApi.createEquipment).not.toHaveBeenCalled();
  });

  it('edits equipment, keeping the identifier read-only', async () => {
    render(<EquipmentList />);
    await screen.findByText('EQ-1');

    const row = screen.getByText('EQ-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Equipment ID').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), {
      target: { value: '130' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(equipmentApi.updateEquipment).toHaveBeenCalledWith(
        'EQ-1',
        expect.objectContaining({
          equipmentName: 'Solar dryer 1',
          equipmentType: 'SOLAR_DRYER',
          capacityKgPerHour: 130,
          energyConsumptionKwhPerKg: 0.85,
          location: 'Unit PT',
          maintenanceStatus: 'ACTIVE',
          lastMaintenanceDate: '2026-10-20',
        }),
      ),
    );
    expect(equipmentApi.updateEquipment.mock.calls[0][1]).not.toHaveProperty('equipmentId');
  });

  it('deletes equipment after confirmation', async () => {
    render(<EquipmentList />);
    await screen.findByText('EQ-1');

    const row = screen.getByText('EQ-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete equipment EQ-1/)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(equipmentApi.deleteEquipment).toHaveBeenCalledWith('EQ-1'));
    await waitFor(() => expect(equipmentApi.getAllEquipment).toHaveBeenCalledTimes(2));
  });

  it('shows an error state and retries the load', async () => {
    equipmentApi.getAllEquipment.mockRejectedValueOnce(new Error('Network down'));

    render(<EquipmentList />);

    expect(await screen.findByText('Network down')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Equipment' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('EQ-1')).toBeTruthy();
  });
});
