// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import EquipmentList from './EquipmentList';
import { equipmentApi } from '../api/equipmentApi';

vi.mock('../api/equipmentApi', () => ({
  equipmentApi: {
    getAllEquipment: vi.fn(),
    updateEquipment: vi.fn(),
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

const renderList = () => render(<EquipmentList />, { wrapper: MemoryRouter });

const MACHINERY_CREATE = '/machinery/equipment-registry?create=1';

beforeEach(() => {
  vi.clearAllMocks();
  equipmentApi.getAllEquipment.mockResolvedValue(EQUIPMENT.map((item) => ({ ...item })));
  equipmentApi.updateEquipment.mockResolvedValue({});
});

describe('EquipmentList', () => {
  it('renders equipment with type, capacity, energy and maintenance status', async () => {
    renderList();

    // The loading state is a skeleton table: wait for the rows before reading the table.
    await screen.findByText('EQ-1');
    const table = screen.getByRole('table');
    expect(within(table).getByText('EQ-1')).toBeTruthy();
    expect(within(table).getByText('Solar dryer 1')).toBeTruthy();
    expect(within(table).getByText('Solar dryer')).toBeTruthy();
    expect(within(table).getByText('120.0 kg/h')).toBeTruthy();
    expect(within(table).getByText('0.85 kWh/kg')).toBeTruthy();
    expect(within(table).getAllByText('Active')).toHaveLength(2);
    expect(within(table).getByText('Maintenance')).toBeTruthy();
  });

  it('creates and deletes no machine: Machinery owns them, the screen links to it', async () => {
    renderList();
    await screen.findByText('EQ-1');

    expect(screen.queryByRole('button', { name: /New Equipment/i })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
    expect(screen.getByText('Not in the list? Ask the Machinery module.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Add it in Machinery' }).getAttribute('href')).toBe(MACHINERY_CREATE);
  });

  it('sends the user to Machinery when it has no processing machine yet', async () => {
    equipmentApi.getAllEquipment.mockResolvedValue([]);
    renderList();

    expect(await screen.findByText('No processing or drying machine in Machinery yet')).toBeTruthy();
    expect(screen.getByText('Ask the Machinery module to add it.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Add it in Machinery' }).getAttribute('href')).toBe(MACHINERY_CREATE);
  });

  it('lists a machine registered from Machinery without its PT attributes and edits its specs', async () => {
    equipmentApi.getAllEquipment.mockResolvedValue([
      {
        equipmentId: 'EQ-00007',
        equipmentName: 'Drum dryer',
        equipmentType: null,
        capacityKgPerHour: null,
        energyConsumptionKwhPerKg: null,
        location: null,
        maintenanceStatus: 'ACTIVE',
        lastMaintenanceDate: null,
      },
    ]);
    renderList();

    // The loading state is a skeleton table: wait for the rows before reading the table.
    await screen.findByText('EQ-00007');
    const table = screen.getByRole('table');
    expect(within(table).getByText('Drum dryer')).toBeTruthy();
    expect(within(table).getByText('N/A kg/h')).toBeTruthy();

    fireEvent.click(within(table).getByRole('button', { name: 'Edit specs' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit equipment specs' });
    fireEvent.change(within(dialog).getByLabelText('Type'), { target: { value: 'TUNNEL_DRYER' } });
    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), { target: { value: '90' } });
    fireEvent.change(within(dialog).getByLabelText('Energy (kWh/kg)'), { target: { value: '1.2' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(equipmentApi.updateEquipment).toHaveBeenCalledWith('EQ-00007', {
        equipmentType: 'TUNNEL_DRYER',
        capacityKgPerHour: 90,
        energyConsumptionKwhPerKg: 1.2,
        location: null,
        lastMaintenanceDate: null,
      }),
    );
  });

  it('edits only the specs: ID, name and status stay read-only and are not sent', async () => {
    renderList();
    await screen.findByText('EQ-1');

    const row = screen.getByText('EQ-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit specs' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Equipment ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Name').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Maintenance Status').disabled).toBe(true);
    expect(within(dialog).getByText('The ID, name and status of a machine are managed in Machinery.')).toBeTruthy();

    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), { target: { value: '130' } });
    fireEvent.change(within(dialog).getByLabelText('Location'), { target: { value: 'Hall B' } });
    fireEvent.change(within(dialog).getByLabelText('Last Maintenance'), { target: { value: '2026-11-09' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(equipmentApi.updateEquipment).toHaveBeenCalledWith('EQ-1', {
        equipmentType: 'SOLAR_DRYER',
        capacityKgPerHour: 130,
        energyConsumptionKwhPerKg: 0.85,
        location: 'Hall B',
        lastMaintenanceDate: '2026-11-09',
      }),
    );
    await waitFor(() => expect(equipmentApi.getAllEquipment).toHaveBeenCalledTimes(2));
  });

  it('rejects a non-positive capacity', async () => {
    renderList();
    await screen.findByText('EQ-1');

    const row = screen.getByText('EQ-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit specs' }));
    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Capacity (kg/h)'), { target: { value: '0' } });
    // jsdom enforces the native `min` constraint on click, so submit directly to
    // exercise the JavaScript validation backstop.
    fireEvent.submit(dialog.querySelector('form'));

    expect(screen.getByText('Capacity must be greater than 0')).toBeTruthy();
    expect(equipmentApi.updateEquipment).not.toHaveBeenCalled();
  });

  it('filters by type, status and search', async () => {
    renderList();
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

  it('shows an error state and retries the load', async () => {
    equipmentApi.getAllEquipment.mockRejectedValueOnce(new Error('Network down'));

    renderList();

    expect(await screen.findByText('Network down')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('EQ-1')).toBeTruthy();
  });

  it('shows the server error in the form and closes it on Cancel', async () => {
    equipmentApi.updateEquipment.mockRejectedValueOnce(new Error(''));
    renderList();
    await screen.findByText('EQ-1');

    const row = screen.getByText('EQ-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit specs' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit equipment specs' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await within(dialog).findByText('Failed to save the equipment')).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });
});
