// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import QcCheckpointList from './QcCheckpointList';
import { qcApi } from '../api/qcApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/qcApi', () => ({
  qcApi: {
    getAllQcCheckpoints: vi.fn(),
    createQcCheckpoint: vi.fn(),
    updateQcCheckpoint: vi.fn(),
    deleteQcCheckpoint: vi.fn(),
  },
}));

vi.mock('../api/batchApi', () => ({
  batchApi: {
    getAllBatches: vi.fn(),
  },
}));

vi.mock('../api/operatorApi', () => ({
  operatorApi: {
    getOperatorsByRole: vi.fn(),
  },
}));

afterEach(cleanup);

const CHECKPOINTS = [
  {
    checkpointId: 'QC-1',
    batchId: 'B-001',
    stage: 'WASHING',
    result: 'PASS',
    defectsCount: 0,
    inspectorId: 'OP-1',
    checkpointTime: '2026-11-08T09:30:00',
    mandatory: true,
  },
  {
    checkpointId: 'QC-2',
    batchId: 'B-002',
    stage: 'FINAL',
    result: 'FAIL',
    defectsCount: 3,
    inspectorId: null,
    checkpointTime: '2026-11-09T10:00:00',
    mandatory: false,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  qcApi.getAllQcCheckpoints.mockResolvedValue(CHECKPOINTS.map((item) => ({ ...item })));
  qcApi.createQcCheckpoint.mockResolvedValue({});
  qcApi.updateQcCheckpoint.mockResolvedValue({});
  qcApi.deleteQcCheckpoint.mockResolvedValue(undefined);
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
  operatorApi.getOperatorsByRole.mockResolvedValue([
    { operatorId: 'OP-1', operatorName: 'Awa Diop', role: 'QC_INSPECTOR' },
  ]);
});

describe('QcCheckpointList', () => {
  it('renders the checkpoints with the inspector name and mandatory flag', async () => {
    render(<QcCheckpointList />);

    expect(await screen.findByText('QC-1')).toBeTruthy();
    expect(screen.getByText('QC-2')).toBeTruthy();
    expect(within(screen.getByRole('table')).getByText('Awa Diop')).toBeTruthy();
    expect(screen.getByText('Mandatory')).toBeTruthy();
    // Only QC checkpoints, and the inspector list is requested for that role.
    expect(operatorApi.getOperatorsByRole).toHaveBeenCalledWith('QC_INSPECTOR');
  });

  it('filters by stage', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    fireEvent.change(screen.getByLabelText('Filter by stage'), { target: { value: 'FINAL' } });

    expect(screen.queryByText('QC-1')).toBeNull();
    expect(screen.getByText('QC-2')).toBeTruthy();
    expect(screen.getByText('1 checkpoints')).toBeTruthy();
  });

  it('filters by inspector', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    fireEvent.change(screen.getByLabelText('Filter by inspector'), { target: { value: 'OP-1' } });

    expect(screen.getByText('QC-1')).toBeTruthy();
    expect(screen.queryByText('QC-2')).toBeNull();
  });

  it('filters by checkpoint date', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    fireEvent.change(screen.getByLabelText('Filter by date'), { target: { value: '2026-11-08' } });

    expect(screen.getByText('QC-1')).toBeTruthy();
    expect(screen.queryByText('QC-2')).toBeNull();
  });

  it('creates a checkpoint through the API and reloads the list', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    fireEvent.click(screen.getByRole('button', { name: /New QC Checkpoint/i }));

    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Checkpoint ID'), { target: { value: 'QC-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-002' } });
    fireEvent.change(within(dialog).getByLabelText('Stage'), { target: { value: 'COOLING' } });
    fireEvent.change(within(dialog).getByLabelText('Result'), { target: { value: 'PASS' } });
    fireEvent.change(within(dialog).getByLabelText('Defects Count'), { target: { value: '1' } });
    fireEvent.change(within(dialog).getByLabelText('Inspector'), { target: { value: 'OP-1' } });
    fireEvent.change(within(dialog).getByLabelText('Checkpoint Time'), {
      target: { value: '2026-11-09T12:15' },
    });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(qcApi.createQcCheckpoint).toHaveBeenCalledWith({
        checkpointId: 'QC-3',
        batchId: 'B-002',
        stage: 'COOLING',
        result: 'PASS',
        defects: null,
        defectsCount: 1,
        inspectorId: 'OP-1',
        checkpointTime: '2026-11-09T12:15',
        notes: null,
      }),
    );
    await waitFor(() => expect(qcApi.getAllQcCheckpoints).toHaveBeenCalledTimes(2));
  });

  it('blocks creation when required fields are missing', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    fireEvent.click(screen.getByRole('button', { name: /New QC Checkpoint/i }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Create' }));

    expect(screen.getByText('Checkpoint ID is required')).toBeTruthy();
    expect(qcApi.createQcCheckpoint).not.toHaveBeenCalled();
  });

  it('edits an existing checkpoint', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    const row = screen.getByText('QC-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Checkpoint ID').value).toBe('QC-1');
    expect(within(dialog).getByLabelText('Checkpoint ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Result'), { target: { value: 'FAIL' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(qcApi.updateQcCheckpoint).toHaveBeenCalledWith(
        'QC-1',
        expect.objectContaining({ result: 'FAIL', stage: 'WASHING', defectsCount: 0 }),
      ),
    );
  });

  it('deletes a checkpoint after confirmation', async () => {
    render(<QcCheckpointList />);
    await screen.findByText('QC-1');

    const row = screen.getByText('QC-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete checkpoint QC-1/)).toBeTruthy();

    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(qcApi.deleteQcCheckpoint).toHaveBeenCalledWith('QC-1'));
    await waitFor(() => expect(qcApi.getAllQcCheckpoints).toHaveBeenCalledTimes(2));
  });

  it('shows the empty state when there are no QC checkpoints', async () => {
    qcApi.getAllQcCheckpoints.mockResolvedValueOnce([]);

    render(<QcCheckpointList />);

    expect(await screen.findByText('No QC checkpoints found')).toBeTruthy();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('shows an error state and retries the load', async () => {
    qcApi.getAllQcCheckpoints.mockRejectedValueOnce(new Error('Network down'));

    render(<QcCheckpointList />);

    expect(await screen.findByText('Network down')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Quality Control' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('QC-1')).toBeTruthy();
  });
});
