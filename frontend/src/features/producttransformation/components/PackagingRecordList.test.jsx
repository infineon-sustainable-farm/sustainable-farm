// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import PackagingRecordList from './PackagingRecordList';
import { packagingApi } from '../api/packagingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/packagingApi', () => ({
  packagingApi: {
    getAllPackagingRecords: vi.fn(),
    createPackagingRecord: vi.fn(),
    updatePackagingRecord: vi.fn(),
    deletePackagingRecord: vi.fn(),
    markExportReady: vi.fn(),
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
    getOperatorsByRole: vi.fn(),
  },
}));

afterEach(cleanup);

const RECORDS = [
  {
    recordId: 'PK-1',
    batchId: 'B-001',
    packageType: 'ONE_KG_BAG',
    packageQuantityKg: 100,
    lotCode: 'LOT-A',
    exportReady: true,
    packagingDate: '2026-10-01',
    equipmentId: 'EQ-1',
    operatorId: 'OP-1',
  },
  {
    recordId: 'PK-2',
    batchId: 'B-002',
    packageType: 'BULK',
    packageQuantityKg: 250.5,
    lotCode: 'LOT-B',
    exportReady: false,
    packagingDate: '2026-11-01',
    equipmentId: null,
    operatorId: null,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  packagingApi.getAllPackagingRecords.mockResolvedValue(RECORDS.map((item) => ({ ...item })));
  packagingApi.createPackagingRecord.mockResolvedValue({});
  packagingApi.updatePackagingRecord.mockResolvedValue({});
  packagingApi.deletePackagingRecord.mockResolvedValue(undefined);
  packagingApi.markExportReady.mockResolvedValue({});
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
  equipmentApi.getAllEquipment.mockResolvedValue([
    { equipmentId: 'EQ-1', equipmentName: 'Sealer 1' },
  ]);
  operatorApi.getOperatorsByRole.mockResolvedValue([
    { operatorId: 'OP-1', operatorName: 'Fatou Ndiaye', role: 'PACKAGER' },
  ]);
});

describe('PackagingRecordList', () => {
  it('renders records, quantities and per-type summary', async () => {
    render(<PackagingRecordList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('PK-1')).toBeTruthy();
    expect(within(table).getByText('PK-2')).toBeTruthy();
    expect(within(table).getByText('100.00 kg')).toBeTruthy();
    expect(within(table).getByText('250.50 kg')).toBeTruthy();
    expect(within(table).getByText('Export ready')).toBeTruthy();
    expect(within(table).getByText('Domestic')).toBeTruthy();
    // Only packagers are requested for the operator selector.
    expect(operatorApi.getOperatorsByRole).toHaveBeenCalledWith('PACKAGER');
    // Quantities are aggregated per package type (including empty types).
    expect(screen.getByText('0.00 kg')).toBeTruthy();
    expect(screen.getByText('0 records')).toBeTruthy();
  });

  it('filters by package type', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.change(screen.getByLabelText('Filter by package type'), {
      target: { value: 'BULK' },
    });

    expect(screen.queryByText('PK-1')).toBeNull();
    expect(screen.getByText('PK-2')).toBeTruthy();
    expect(screen.getByText('1 records')).toBeTruthy();
  });

  it('filters by export readiness', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.change(screen.getByLabelText('Filter by export readiness'), {
      target: { value: 'no' },
    });

    expect(screen.queryByText('PK-1')).toBeNull();
    expect(screen.getByText('PK-2')).toBeTruthy();
  });

  it('filters by packaging date', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.change(screen.getByLabelText('Filter by packaging date'), {
      target: { value: '2026-10-01' },
    });

    expect(screen.getByText('PK-1')).toBeTruthy();
    expect(screen.queryByText('PK-2')).toBeNull();
  });

  it('suggests an editable lot code without overwriting a manual value', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.click(screen.getByRole('button', { name: /New Packaging Record/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.click(within(dialog).getByRole('button', { name: /Suggest/i }));

    const lotCode = within(dialog).getByLabelText('Lot Code');
    expect(lotCode.value).toMatch(/^LOT-B-001-\d{8}-\d{6}$/);

    fireEvent.change(lotCode, { target: { value: 'MANUAL-LOT' } });
    expect(lotCode.value).toBe('MANUAL-LOT');
  });

  it('creates a record through the API and reloads the list', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.click(screen.getByRole('button', { name: /New Packaging Record/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Record ID'), { target: { value: 'PK-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-002' } });
    fireEvent.change(within(dialog).getByLabelText('Package Type'), {
      target: { value: 'TWO_KG_BAG' },
    });
    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), {
      target: { value: '42.5' },
    });
    fireEvent.change(within(dialog).getByLabelText('Lot Code'), {
      target: { value: 'LOT-C' },
    });
    fireEvent.change(within(dialog).getByLabelText('Packaging Date'), {
      target: { value: '2026-11-09' },
    });
    fireEvent.click(within(dialog).getByLabelText('Export ready'));
    fireEvent.change(within(dialog).getByLabelText('Equipment'), {
      target: { value: 'EQ-1' },
    });
    fireEvent.change(within(dialog).getByLabelText('Operator'), {
      target: { value: 'OP-1' },
    });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(packagingApi.createPackagingRecord).toHaveBeenCalledWith({
        recordId: 'PK-3',
        batchId: 'B-002',
        packageType: 'TWO_KG_BAG',
        packageQuantityKg: 42.5,
        exportReady: true,
        packagingDate: '2026-11-09',
        equipmentId: 'EQ-1',
        operatorId: 'OP-1',
        lotCode: 'LOT-C',
      }),
    );
    await waitFor(() => expect(packagingApi.getAllPackagingRecords).toHaveBeenCalledTimes(2));
  });

  it('blocks creation when required fields are missing', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.click(screen.getByRole('button', { name: /New Packaging Record/i }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Create' }));

    expect(screen.getByText('Record ID is required')).toBeTruthy();
    expect(packagingApi.createPackagingRecord).not.toHaveBeenCalled();
  });

  it('rejects a non-positive package quantity', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    fireEvent.click(screen.getByRole('button', { name: /New Packaging Record/i }));
    const dialog = screen.getByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Record ID'), { target: { value: 'PK-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-001' } });
    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), { target: { value: '0' } });
    fireEvent.change(within(dialog).getByLabelText('Lot Code'), {
      target: { value: 'LOT-C' },
    });
    // jsdom enforces the native `min` constraint on click, so submit directly to
    // exercise the JavaScript validation backstop.
    fireEvent.submit(dialog.querySelector('form'));

    expect(screen.getByText('Package quantity must be greater than 0')).toBeTruthy();
    expect(packagingApi.createPackagingRecord).not.toHaveBeenCalled();
  });

  it('edits an existing record, keeping traceability fields read-only', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    const row = screen.getByText('PK-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Record ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Lot Code').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Quantity (kg)'), {
      target: { value: '120' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(packagingApi.updatePackagingRecord).toHaveBeenCalledWith(
        'PK-1',
        expect.objectContaining({
          packageType: 'ONE_KG_BAG',
          packageQuantityKg: 120,
          exportReady: true,
          packagingDate: '2026-10-01',
        }),
      ),
    );
    // Lot code is not part of the update contract.
    expect(packagingApi.updatePackagingRecord.mock.calls[0][1]).not.toHaveProperty('lotCode');
  });

  it('marks a record as export ready', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-2');

    const row = screen.getByText('PK-2').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Mark ready' }));

    await waitFor(() => expect(packagingApi.markExportReady).toHaveBeenCalledWith('PK-2'));
    await waitFor(() => expect(packagingApi.getAllPackagingRecords).toHaveBeenCalledTimes(2));
  });

  it('deletes a record after confirmation', async () => {
    render(<PackagingRecordList />);
    await screen.findByText('PK-1');

    const row = screen.getByText('PK-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete record PK-1/)).toBeTruthy();

    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(packagingApi.deletePackagingRecord).toHaveBeenCalledWith('PK-1'));
    await waitFor(() => expect(packagingApi.getAllPackagingRecords).toHaveBeenCalledTimes(2));
  });

  it('opens the compliance view for a record batch', async () => {
    const onOpenCompliance = vi.fn();
    render(<PackagingRecordList onOpenCompliance={onOpenCompliance} />);
    await screen.findByText('PK-1');

    const row = screen.getByText('PK-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Compliance' }));

    expect(onOpenCompliance).toHaveBeenCalledWith('B-001');
  });

  it('shows an error state and retries the load', async () => {
    packagingApi.getAllPackagingRecords.mockRejectedValueOnce(new Error('Network down'));

    render(<PackagingRecordList />);

    expect(await screen.findByText('Network down')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('PK-1')).toBeTruthy();
  });
});
