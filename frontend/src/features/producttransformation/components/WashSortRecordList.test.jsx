// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import WashSortRecordList from './WashSortRecordList';
import { washSortApi } from '../api/washSortApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/washSortApi', () => ({
  washSortApi: {
    getAllWashSortRecords: vi.fn(),
    createWashSortRecord: vi.fn(),
    updateWashSortRecord: vi.fn(),
    deleteWashSortRecord: vi.fn(),
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

const RECORDS = [
  {
    recordId: 'WS-1',
    batchId: 'B-001',
    inputQuantityKg: 520,
    outputQuantityKg: 480,
    wasteQuantityKg: 40,
    waterUsageLiters: 1250,
    startTime: '2026-10-01T07:45:00',
    endTime: '2026-10-01T09:30:00',
    equipmentId: 'EQ-1',
    operatorId: 'OP-1',
    yieldPercentage: 92.3,
  },
  {
    recordId: 'WS-2',
    batchId: 'B-002',
    inputQuantityKg: 610.25,
    outputQuantityKg: 565,
    wasteQuantityKg: 45.25,
    waterUsageLiters: 1380,
    startTime: '2026-11-01T08:15:00',
    endTime: '2026-11-01T10:15:00',
    equipmentId: 'EQ-1',
    operatorId: 'OP-1',
    yieldPercentage: 92.6,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  washSortApi.getAllWashSortRecords.mockResolvedValue(RECORDS.map((item) => ({ ...item })));
  washSortApi.createWashSortRecord.mockResolvedValue({});
  washSortApi.updateWashSortRecord.mockResolvedValue({});
  washSortApi.deleteWashSortRecord.mockResolvedValue(undefined);
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
  equipmentApi.getAllEquipment.mockResolvedValue([
    {
      equipmentId: 'EQ-1',
      equipmentName: 'Washer 1',
      equipmentType: 'WASHING',
      maintenanceStatus: 'ACTIVE',
    },
    {
      equipmentId: 'EQ-2',
      equipmentName: 'Solar dryer 1',
      equipmentType: 'SOLAR_DRYER',
      maintenanceStatus: 'ACTIVE',
    },
  ]);
  operatorApi.getAllOperators.mockResolvedValue([
    { operatorId: 'OP-1', operatorName: 'Moussa', role: 'WASHER' },
    { operatorId: 'OP-2', operatorName: 'Issa', role: 'DRYER' },
  ]);
});

describe('WashSortRecordList', () => {
  it('renders yields, waste, water and resolved equipment/operator', async () => {
    render(<WashSortRecordList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('WS-1')).toBeTruthy();
    expect(within(table).getByText('92.3%')).toBeTruthy();
    expect(within(table).getByText('92.6%')).toBeTruthy();
    expect(within(table).getByText('1250 L')).toBeTruthy();
    expect(within(table).getAllByText('Washer 1').length).toBeGreaterThan(0);
    expect(within(table).getAllByText('Moussa').length).toBeGreaterThan(0);
    // Names are resolved from all operators, not just the assignable role.
    expect(operatorApi.getAllOperators).toHaveBeenCalled();
  });

  it('filters by batch and start date', async () => {
    render(<WashSortRecordList />);
    await screen.findByText('WS-1');

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'B-002' } });
    expect(screen.queryByText('WS-1')).toBeNull();
    expect(screen.getByText('WS-2')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'all' } });
    fireEvent.change(screen.getByLabelText('Filter by start date'), {
      target: { value: '2026-10-01' },
    });
    expect(screen.getByText('WS-1')).toBeTruthy();
    expect(screen.queryByText('WS-2')).toBeNull();
  });

  it('creates a wash record through the API and reloads the list', async () => {
    render(<WashSortRecordList />);
    await screen.findByText('WS-1');

    fireEvent.click(screen.getByRole('button', { name: /New Wash Record/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Record ID'), { target: { value: 'WS-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Input (kg)'), { target: { value: '300' } });
    fireEvent.change(within(dialog).getByLabelText('Output (kg)'), { target: { value: '270' } });
    fireEvent.change(within(dialog).getByLabelText('Waste (kg)'), { target: { value: '30' } });
    fireEvent.change(within(dialog).getByLabelText('Water (L)'), { target: { value: '800' } });
    fireEvent.change(within(dialog).getByLabelText('Start Time'), {
      target: { value: '2026-11-09T08:00' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Time'), {
      target: { value: '2026-11-09T10:00' },
    });
    fireEvent.change(within(dialog).getByLabelText('Equipment'), { target: { value: 'EQ-1' } });
    fireEvent.change(within(dialog).getByLabelText('Operator'), { target: { value: 'OP-1' } });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(washSortApi.createWashSortRecord).toHaveBeenCalledWith({
        recordId: 'WS-3',
        batchId: 'B-001',
        inputQuantityKg: 300,
        outputQuantityKg: 270,
        wasteQuantityKg: 30,
        waterUsageLiters: 800,
        startTime: '2026-11-09T08:00:00',
        endTime: '2026-11-09T10:00:00',
        equipmentId: 'EQ-1',
        operatorId: 'OP-1',
      }),
    );
    await waitFor(() => expect(washSortApi.getAllWashSortRecords).toHaveBeenCalledTimes(2));
  });

  it('rejects an end time before the start time', async () => {
    render(<WashSortRecordList />);
    await screen.findByText('WS-1');

    fireEvent.click(screen.getByRole('button', { name: /New Wash Record/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Record ID'), { target: { value: 'WS-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Input (kg)'), { target: { value: '300' } });
    fireEvent.change(within(dialog).getByLabelText('Output (kg)'), { target: { value: '270' } });
    fireEvent.change(within(dialog).getByLabelText('Waste (kg)'), { target: { value: '30' } });
    fireEvent.change(within(dialog).getByLabelText('Water (L)'), { target: { value: '800' } });
    fireEvent.change(within(dialog).getByLabelText('Start Time'), {
      target: { value: '2026-11-09T10:00' },
    });
    fireEvent.change(within(dialog).getByLabelText('End Time'), {
      target: { value: '2026-11-09T09:00' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    expect(screen.getByText('End time must be after start time')).toBeTruthy();
    expect(washSortApi.createWashSortRecord).not.toHaveBeenCalled();
  });

  it('edits a record, keeping identity fields read-only', async () => {
    render(<WashSortRecordList />);
    await screen.findByText('WS-1');

    const row = screen.getByText('WS-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Record ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Water (L)'), {
      target: { value: '900' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(washSortApi.updateWashSortRecord).toHaveBeenCalledWith(
        'WS-1',
        expect.objectContaining({
          inputQuantityKg: 520,
          outputQuantityKg: 480,
          wasteQuantityKg: 40,
          waterUsageLiters: 900,
          startTime: '2026-10-01T07:45:00',
          endTime: '2026-10-01T09:30:00',
        }),
      ),
    );
    expect(washSortApi.updateWashSortRecord.mock.calls[0][1]).not.toHaveProperty('recordId');
    expect(washSortApi.updateWashSortRecord.mock.calls[0][1]).not.toHaveProperty('batchId');
  });

  it('deletes a record after confirmation', async () => {
    render(<WashSortRecordList />);
    await screen.findByText('WS-1');

    const row = screen.getByText('WS-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete record WS-1/)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(washSortApi.deleteWashSortRecord).toHaveBeenCalledWith('WS-1'));
    await waitFor(() => expect(washSortApi.getAllWashSortRecords).toHaveBeenCalledTimes(2));
  });

  it('shows an error state and retries the load', async () => {
    washSortApi.getAllWashSortRecords.mockRejectedValueOnce(new Error('Network down'));

    render(<WashSortRecordList />);

    expect(await screen.findByText('Network down')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('WS-1')).toBeTruthy();
  });
});
