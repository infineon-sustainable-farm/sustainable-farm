// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import DryingRunList from './DryingRunList';
import { dryingApi } from '../api/dryingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/dryingApi', () => ({
  dryingApi: {
    getAllDryingRuns: vi.fn(),
    createDryingRun: vi.fn(),
    updateDryingRun: vi.fn(),
    deleteDryingRun: vi.fn(),
  },
}));

vi.mock('../api/batchApi', () => ({
  batchApi: {
    getAllBatches: vi.fn(),
  },
}));

vi.mock('../api/equipmentApi', () => ({
  equipmentApi: {
    getAllEquipment: vi.fn(),
  },
}));

vi.mock('../api/operatorApi', () => ({
  operatorApi: {
    getAllOperators: vi.fn(),
  },
}));

afterEach(cleanup);

const RUNS = [
  {
    runId: 'DR-1',
    batchId: 'B-001',
    durationHours: 6.5,
    targetTemperatureC: 55,
    actualTemperatureC: 54.5,
    startMoisturePct: 78,
    endMoisturePct: 12,
    energyUsageKwh: 118.4,
    startTime: '2026-10-01T08:00:00',
    endTime: '2026-10-01T14:30:00',
    equipmentId: 'EQ-1',
    operatorId: 'OP-1',
  },
  {
    runId: 'DR-2',
    batchId: 'B-002',
    durationHours: 5,
    targetTemperatureC: 55,
    actualTemperatureC: 55.2,
    startMoisturePct: 76,
    endMoisturePct: 11.5,
    energyUsageKwh: 96.75,
    startTime: '2026-11-01T08:00:00',
    endTime: '2026-11-01T13:00:00',
    equipmentId: 'EQ-1',
    operatorId: 'OP-1',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  dryingApi.getAllDryingRuns.mockResolvedValue(RUNS.map((item) => ({ ...item })));
  dryingApi.createDryingRun.mockResolvedValue({});
  dryingApi.updateDryingRun.mockResolvedValue({});
  dryingApi.deleteDryingRun.mockResolvedValue(undefined);
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
  equipmentApi.getAllEquipment.mockResolvedValue([
    {
      equipmentId: 'EQ-1',
      equipmentName: 'Solar dryer 1',
      equipmentType: 'SOLAR_DRYER',
      maintenanceStatus: 'ACTIVE',
    },
    {
      equipmentId: 'EQ-2',
      equipmentName: 'Washer 1',
      equipmentType: 'WASHING',
      maintenanceStatus: 'ACTIVE',
    },
  ]);
  operatorApi.getAllOperators.mockResolvedValue([
    { operatorId: 'OP-1', operatorName: 'Issa', role: 'DRYER' },
    { operatorId: 'OP-2', operatorName: 'Moussa', role: 'WASHER' },
  ]);
});

describe('DryingRunList', () => {
  it('renders duration, moisture, energy and resolved equipment/operator', async () => {
    render(<DryingRunList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('DR-1')).toBeTruthy();
    expect(within(table).getByText('6.50 h')).toBeTruthy();
    expect(within(table).getByText('118.40 kWh')).toBeTruthy();
    expect(within(table).getByText(/78.0% → 12.0%/)).toBeTruthy();
    expect(within(table).getAllByText('Solar dryer 1').length).toBeGreaterThan(0);
    expect(within(table).getAllByText('Issa').length).toBeGreaterThan(0);
    // Names are resolved from all operators, not just the assignable role.
    expect(operatorApi.getAllOperators).toHaveBeenCalled();
  });

  it('filters by batch and start date', async () => {
    render(<DryingRunList />);
    await screen.findByText('DR-1');

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'B-002' } });
    expect(screen.queryByText('DR-1')).toBeNull();
    expect(screen.getByText('DR-2')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'all' } });
    fireEvent.change(screen.getByLabelText('Filter by start date'), {
      target: { value: '2026-10-01' },
    });
    expect(screen.getByText('DR-1')).toBeTruthy();
    expect(screen.queryByText('DR-2')).toBeNull();
  });

  it('creates a drying run through the API and reloads the list', async () => {
    render(<DryingRunList />);
    await screen.findByText('DR-1');

    fireEvent.click(screen.getByRole('button', { name: /New Drying Run/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Run ID'), { target: { value: 'DR-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Duration (h)'), {
      target: { value: '4.5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Target Temp (°C)'), {
      target: { value: '55' },
    });
    fireEvent.change(within(dialog).getByLabelText('Actual Temp (°C)'), {
      target: { value: '54' },
    });
    fireEvent.change(within(dialog).getByLabelText('Start Moisture (%)'), {
      target: { value: '70' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Moisture (%)'), {
      target: { value: '13' },
    });
    fireEvent.change(within(dialog).getByLabelText('Energy (kWh)'), {
      target: { value: '80.5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Start Time'), {
      target: { value: '2026-11-09T08:00' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Time'), {
      target: { value: '2026-11-09T12:30' },
    });
    fireEvent.change(within(dialog).getByLabelText('Equipment'), { target: { value: 'EQ-1' } });
    fireEvent.change(within(dialog).getByLabelText('Operator'), { target: { value: 'OP-1' } });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(dryingApi.createDryingRun).toHaveBeenCalledWith({
        runId: 'DR-3',
        batchId: 'B-001',
        durationHours: 4.5,
        targetTemperatureC: 55,
        actualTemperatureC: 54,
        startMoisturePct: 70,
        endMoisturePct: 13,
        energyUsageKwh: 80.5,
        startTime: '2026-11-09T08:00:00',
        endTime: '2026-11-09T12:30:00',
        equipmentId: 'EQ-1',
        operatorId: 'OP-1',
      }),
    );
    await waitFor(() => expect(dryingApi.getAllDryingRuns).toHaveBeenCalledTimes(2));
  });

  it('rejects an end moisture outside the 6–18% EU range', async () => {
    render(<DryingRunList />);
    await screen.findByText('DR-1');

    fireEvent.click(screen.getByRole('button', { name: /New Drying Run/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Run ID'), { target: { value: 'DR-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Duration (h)'), {
      target: { value: '4.5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Target Temp (°C)'), {
      target: { value: '55' },
    });
    fireEvent.change(within(dialog).getByLabelText('Actual Temp (°C)'), {
      target: { value: '54' },
    });
    fireEvent.change(within(dialog).getByLabelText('Start Moisture (%)'), {
      target: { value: '70' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Moisture (%)'), {
      target: { value: '5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Energy (kWh)'), {
      target: { value: '80.5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Start Time'), {
      target: { value: '2026-11-09T08:00' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Time'), {
      target: { value: '2026-11-09T12:30' },
    });
    // jsdom enforces the native min/max constraints on click, so submit directly to
    // exercise the JavaScript validation backstop.
    fireEvent.submit(dialog.querySelector('form'));

    expect(
      screen.getByText('End moisture must be between 6% and 18%'),
    ).toBeTruthy();
    expect(dryingApi.createDryingRun).not.toHaveBeenCalled();
  });

  it('edits a run, keeping identity fields read-only', async () => {
    render(<DryingRunList />);
    await screen.findByText('DR-1');

    const row = screen.getByText('DR-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Run ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Energy (kWh)'), {
      target: { value: '140' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(dryingApi.updateDryingRun).toHaveBeenCalledWith(
        'DR-1',
        expect.objectContaining({
          durationHours: 6.5,
          targetTemperatureC: 55,
          actualTemperatureC: 54.5,
          startMoisturePct: 78,
          endMoisturePct: 12,
          energyUsageKwh: 140,
          startTime: '2026-10-01T08:00:00',
          endTime: '2026-10-01T14:30:00',
        }),
      ),
    );
    expect(dryingApi.updateDryingRun.mock.calls[0][1]).not.toHaveProperty('runId');
    expect(dryingApi.updateDryingRun.mock.calls[0][1]).not.toHaveProperty('batchId');
  });

  it('deletes a run after confirmation', async () => {
    render(<DryingRunList />);
    await screen.findByText('DR-1');

    const row = screen.getByText('DR-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete run DR-1/)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(dryingApi.deleteDryingRun).toHaveBeenCalledWith('DR-1'));
    await waitFor(() => expect(dryingApi.getAllDryingRuns).toHaveBeenCalledTimes(2));
  });

  it('renders a 403 from the API as an error state instead of crashing', async () => {
    dryingApi.getAllDryingRuns.mockRejectedValueOnce(
      Object.assign(new Error('Forbidden'), { status: 403 }),
    );

    render(<DryingRunList />);

    expect(await screen.findByText('Forbidden')).toBeTruthy();
    expect(screen.getByRole('button', { name: /Try again/i })).toBeTruthy();
  });

  it('shows an error state and retries the load', async () => {
    dryingApi.getAllDryingRuns.mockRejectedValueOnce(new Error('Network down'));

    render(<DryingRunList />);

    expect(await screen.findByText('Network down')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('DR-1')).toBeTruthy();
  });
});
