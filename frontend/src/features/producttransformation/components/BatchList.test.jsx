// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BatchList from './BatchList';
import { batchApi } from '../api/batchApi';

vi.mock('../api/batchApi', () => ({
  batchApi: {
    getAllBatches: vi.fn(),
    advanceBatchStatus: vi.fn(),
    createBatch: vi.fn(),
  },
}));

afterEach(cleanup);

// The list navigates to the batch timeline, so it needs a Router.
const renderList = () =>
  render(
    <MemoryRouter initialEntries={['/product-transformation/batches']}>
      <Routes>
        <Route path="/product-transformation/batches" element={<BatchList />} />
        <Route
          path="/product-transformation/batches/:batchId"
          element={<p>timeline placeholder</p>}
        />
      </Routes>
    </MemoryRouter>,
  );

const BATCHES = [
  {
    batchId: 'B-001',
    harvestDate: '2026-10-01',
    mangoVariety: 'KEITT',
    harvestQuantityKg: 520,
    currentStatus: 'WASHING',
    farmId: 'FARM-1',
    blockId: 'BLOCK-1',
  },
  {
    batchId: 'B-002',
    harvestDate: '2026-10-02',
    mangoVariety: 'KENT',
    harvestQuantityKg: 480.5,
    currentStatus: 'SHIPPED',
    farmId: 'FARM-2',
    blockId: 'BLOCK-2',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  batchApi.getAllBatches.mockResolvedValue(BATCHES.map((batch) => ({ ...batch })));
  batchApi.advanceBatchStatus.mockResolvedValue({});
  batchApi.createBatch.mockResolvedValue({});
});

describe('BatchList', () => {
  it('opens the traceability timeline when a row is clicked', async () => {
    renderList();
    await screen.findByText('B-001');

    // Click a plain cell, not the ID link, to prove the whole row is clickable.
    fireEvent.click(screen.getByText('KEITT'));

    expect(await screen.findByText('timeline placeholder')).toBeTruthy();
  });
  it('loads and renders batches with quantity, status and origin', async () => {
    renderList();

    const table = await screen.findByRole('table');
    expect(within(table).getByText('B-001')).toBeTruthy();
    expect(within(table).getByText('B-002')).toBeTruthy();
    expect(within(table).getByText('KEITT')).toBeTruthy();
    expect(within(table).getByText('520 kg')).toBeTruthy();
    expect(within(table).getByText('480.5 kg')).toBeTruthy();
    expect(within(table).getByText('WASHING')).toBeTruthy();
    expect(within(table).getByText('FARM-1 / BLOCK-1')).toBeTruthy();
  });

  it('filters batches by status', async () => {
    renderList();
    await screen.findByText('B-001');

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'SHIPPED' } });

    expect(screen.queryByText('B-001')).toBeNull();
    expect(screen.getByText('B-002')).toBeTruthy();
    expect(screen.getByText('1 batches')).toBeTruthy();
  });

  it('advances an eligible batch and reloads the list', async () => {
    renderList();
    await screen.findByText('B-001');

    const row = screen.getByText('B-001').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: /Advance/i }));

    await waitFor(() => expect(batchApi.advanceBatchStatus).toHaveBeenCalledWith('B-001'));
    await waitFor(() => expect(batchApi.getAllBatches).toHaveBeenCalledTimes(2));
  });

  it('does not offer Advance for shipped or rejected batches', async () => {
    renderList();
    await screen.findByText('B-002');

    const shippedRow = screen.getByText('B-002').closest('tr');
    expect(within(shippedRow).queryByRole('button', { name: /Advance/i })).toBeNull();
  });

  it('shows the empty state when there are no batches', async () => {
    batchApi.getAllBatches.mockResolvedValueOnce([]);

    renderList();

    expect(await screen.findByText('No batches found')).toBeTruthy();
  });

  it('surfaces a failed status advance without losing the table', async () => {
    batchApi.advanceBatchStatus.mockRejectedValueOnce(new Error('QC pending'));

    renderList();
    await screen.findByText('B-001');

    const row = screen.getByText('B-001').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: /Advance/i }));

    expect(
      await screen.findByText(
        'Failed to advance batch status. Please check QC requirements.',
      ),
    ).toBeTruthy();
    expect(screen.getByText('B-001')).toBeTruthy();
  });

  it('shows an error state and retries the load', async () => {
    batchApi.getAllBatches.mockRejectedValueOnce(new Error('Network down'));

    renderList();

    expect(await screen.findByText('Network down')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Batch Management' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('B-001')).toBeTruthy();
  });

  it('renders a 401 from the API as an error state instead of crashing', async () => {
    const unauthorized = Object.assign(new Error('Unauthorized'), { status: 401 });
    batchApi.getAllBatches.mockRejectedValueOnce(unauthorized);

    renderList();

    expect(await screen.findByText('Unauthorized')).toBeTruthy();
    expect(screen.getByRole('button', { name: /Try again/i })).toBeTruthy();
  });

  it('opens the New Batch form and closes it with Cancel', async () => {
    renderList();
    await screen.findByText('B-001');

    fireEvent.click(screen.getByRole('button', { name: /New Batch/i }));

    expect(screen.getByRole('dialog', { name: /New batch/i })).toBeTruthy();
    expect(screen.getByLabelText('Batch ID')).toBeTruthy();
    expect(screen.getByLabelText('Harvest date')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(batchApi.createBatch).not.toHaveBeenCalled();
  });

  it('creates a batch from the form and reloads the list', async () => {
    renderList();
    await screen.findByText('B-001');

    fireEvent.click(screen.getByRole('button', { name: /New Batch/i }));

    fireEvent.change(screen.getByLabelText('Batch ID'), { target: { value: ' B-003 ' } });
    fireEvent.change(screen.getByLabelText('Harvest date'), { target: { value: '2026-11-12' } });
    fireEvent.change(screen.getByLabelText('Mango variety'), { target: { value: 'KENT' } });
    fireEvent.change(screen.getByLabelText('Harvest quantity (kg)'), { target: { value: '100' } });
    fireEvent.change(screen.getByLabelText('Farm ID'), { target: { value: 'FARM-3' } });
    fireEvent.change(screen.getByLabelText('Block ID'), { target: { value: 'BLOCK-3' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(batchApi.createBatch).toHaveBeenCalledWith({
        batchId: 'B-003',
        harvestDate: '2026-11-12',
        mangoVariety: 'KENT',
        harvestQuantityKg: 100,
        farmId: 'FARM-3',
        blockId: 'BLOCK-3',
      }),
    );
    await waitFor(() => expect(batchApi.getAllBatches).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('blocks the submission when the harvest quantity is missing', async () => {
    renderList();
    await screen.findByText('B-001');

    fireEvent.click(screen.getByRole('button', { name: /New Batch/i }));
    fireEvent.change(screen.getByLabelText('Batch ID'), { target: { value: 'B-004' } });
    fireEvent.change(screen.getByLabelText('Farm ID'), { target: { value: 'FARM-3' } });
    fireEvent.change(screen.getByLabelText('Block ID'), { target: { value: 'BLOCK-3' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Harvest quantity must be greater than 0')).toBeTruthy();
    expect(batchApi.createBatch).not.toHaveBeenCalled();
  });

  it('surfaces a duplicate batch id returned by the API', async () => {
    batchApi.createBatch.mockRejectedValueOnce(
      new Error('Batch already exists: B-001'),
    );

    renderList();
    await screen.findByText('B-001');

    fireEvent.click(screen.getByRole('button', { name: /New Batch/i }));
    fireEvent.change(screen.getByLabelText('Batch ID'), { target: { value: 'B-001' } });
    fireEvent.change(screen.getByLabelText('Harvest date'), { target: { value: '2026-11-12' } });
    fireEvent.change(screen.getByLabelText('Harvest quantity (kg)'), { target: { value: '50' } });
    fireEvent.change(screen.getByLabelText('Farm ID'), { target: { value: 'FARM-1' } });
    fireEvent.change(screen.getByLabelText('Block ID'), { target: { value: 'BLOCK-1' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Batch already exists: B-001')).toBeTruthy();
    expect(screen.getByRole('dialog')).toBeTruthy();
  });
});
