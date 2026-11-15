// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import RawIntakeList from './RawIntakeList';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { batchApi } from '../api/batchApi';

vi.mock('../api/rawIntakeApi', () => ({
  rawIntakeApi: {
    getAllRawIntakes: vi.fn(),
    createRawIntake: vi.fn(),
    updateRawIntake: vi.fn(),
    deleteRawIntake: vi.fn(),
  },
}));

vi.mock('../api/batchApi', () => ({
  batchApi: {
    getAllBatches: vi.fn(),
  },
}));

afterEach(cleanup);

const INTAKES = [
  {
    intakeId: 'RI-1',
    batchId: 'B-001',
    sourceFarm: 'Farm A',
    sourceBlock: 'Block A',
    intakeDate: '2026-10-01',
    receivedQuantityKg: 520,
    receivedVariety: 'KEITT',
    receivedGrade: 'A',
    intakeOperator: 'Moussa',
  },
  {
    intakeId: 'RI-2',
    batchId: 'B-002',
    sourceFarm: 'Farm B',
    sourceBlock: 'Block B',
    intakeDate: '2026-11-01',
    receivedQuantityKg: 480.5,
    receivedVariety: 'KENT',
    receivedGrade: 'B',
    intakeOperator: 'Awa',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  rawIntakeApi.getAllRawIntakes.mockResolvedValue(INTAKES.map((item) => ({ ...item })));
  rawIntakeApi.createRawIntake.mockResolvedValue({});
  rawIntakeApi.updateRawIntake.mockResolvedValue({});
  rawIntakeApi.deleteRawIntake.mockResolvedValue(undefined);
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
});

describe('RawIntakeList', () => {
  it('renders intakes with source, quantity and grade', async () => {
    render(<RawIntakeList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('RI-1')).toBeTruthy();
    expect(within(table).getByText('RI-2')).toBeTruthy();
    expect(within(table).getByText('520.00 kg')).toBeTruthy();
    expect(within(table).getByText('480.50 kg')).toBeTruthy();
    expect(within(table).getByText('Farm A / Block A')).toBeTruthy();
    expect(within(table).getByText('Grade A')).toBeTruthy();
    expect(within(table).getByText('Grade B')).toBeTruthy();
  });

  it('filters by batch and grade', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'B-002' } });
    expect(screen.queryByText('RI-1')).toBeNull();
    expect(screen.getByText('RI-2')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Filter by batch'), { target: { value: 'all' } });
    fireEvent.change(screen.getByLabelText('Filter by grade'), { target: { value: 'A' } });
    expect(screen.getByText('RI-1')).toBeTruthy();
    expect(screen.queryByText('RI-2')).toBeNull();
  });

  it('creates an intake through the API and reloads the list', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    fireEvent.click(screen.getByRole('button', { name: /New Raw Intake/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Intake ID'), { target: { value: 'RI-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-002' } });
    fireEvent.change(within(dialog).getByLabelText('Source Farm'), {
      target: { value: 'Farm C' },
    });
    fireEvent.change(within(dialog).getByLabelText('Source Block'), {
      target: { value: 'Block C' },
    });
    fireEvent.change(within(dialog).getByLabelText('Intake Date'), {
      target: { value: '2026-11-09' },
    });
    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), {
      target: { value: '150.25' },
    });
    fireEvent.change(within(dialog).getByLabelText('Variety'), { target: { value: 'TOMMY' } });
    fireEvent.change(within(dialog).getByLabelText('Grade'), { target: { value: 'C' } });
    fireEvent.change(within(dialog).getByLabelText('Intake Operator'), {
      target: { value: 'Issa' },
    });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(rawIntakeApi.createRawIntake).toHaveBeenCalledWith({
        intakeId: 'RI-3',
        batchId: 'B-002',
        sourceFarm: 'Farm C',
        sourceBlock: 'Block C',
        intakeDate: '2026-11-09',
        receivedQuantityKg: 150.25,
        receivedVariety: 'TOMMY',
        receivedGrade: 'C',
        intakeOperator: 'Issa',
      }),
    );
    await waitFor(() => expect(rawIntakeApi.getAllRawIntakes).toHaveBeenCalledTimes(2));
  });

  it('blocks creation when required fields are missing', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    fireEvent.click(screen.getByRole('button', { name: /New Raw Intake/i }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Create' }));

    expect(screen.getByText('Intake ID is required')).toBeTruthy();
    expect(rawIntakeApi.createRawIntake).not.toHaveBeenCalled();
  });

  it('rejects a non-positive received quantity', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    fireEvent.click(screen.getByRole('button', { name: /New Raw Intake/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Intake ID'), { target: { value: 'RI-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Source Farm'), {
      target: { value: 'Farm C' },
    });
    fireEvent.change(within(dialog).getByLabelText('Source Block'), {
      target: { value: 'Block C' },
    });
    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), { target: { value: '0' } });
    // jsdom enforces the native `min` constraint on click, so submit directly to
    // exercise the JavaScript validation backstop.
    fireEvent.submit(dialog.querySelector('form'));

    expect(screen.getByText('Received quantity must be greater than 0')).toBeTruthy();
    expect(rawIntakeApi.createRawIntake).not.toHaveBeenCalled();
  });

  it('edits an intake, keeping identity fields read-only', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    const row = screen.getByText('RI-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Intake ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), {
      target: { value: '530' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(rawIntakeApi.updateRawIntake).toHaveBeenCalledWith(
        'RI-1',
        expect.objectContaining({
          sourceFarm: 'Farm A',
          sourceBlock: 'Block A',
          intakeDate: '2026-10-01',
          receivedQuantityKg: 530,
          receivedVariety: 'KEITT',
          receivedGrade: 'A',
          intakeOperator: 'Moussa',
        }),
      ),
    );
    expect(rawIntakeApi.updateRawIntake.mock.calls[0][1]).not.toHaveProperty('intakeId');
    expect(rawIntakeApi.updateRawIntake.mock.calls[0][1]).not.toHaveProperty('batchId');
  });

  it('deletes an intake after confirmation', async () => {
    render(<RawIntakeList />);
    await screen.findByText('RI-1');

    const row = screen.getByText('RI-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete intake RI-1/)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(rawIntakeApi.deleteRawIntake).toHaveBeenCalledWith('RI-1'));
    await waitFor(() => expect(rawIntakeApi.getAllRawIntakes).toHaveBeenCalledTimes(2));
  });

  it('shows an error state and retries the load', async () => {
    rawIntakeApi.getAllRawIntakes.mockRejectedValueOnce(new Error('Network down'));

    render(<RawIntakeList />);

    expect(await screen.findByText('Network down')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('RI-1')).toBeTruthy();
  });
});
