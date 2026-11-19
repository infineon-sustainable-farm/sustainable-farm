// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import ComplianceRecordList from './ComplianceRecordList';
import { complianceApi } from '../api/complianceApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/complianceApi', () => ({
  complianceApi: {
    getAllComplianceRecords: vi.fn(),
    createComplianceRecord: vi.fn(),
    updateComplianceRecord: vi.fn(),
    deleteComplianceRecord: vi.fn(),
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

const RECORDS = [
  {
    recordId: 'CR-1',
    batchId: 'B-001',
    complianceType: 'HACCP',
    requirement: 'HACCP plan in place',
    result: 'COMPLIANT',
    evidence: 'Plan v2',
    auditorId: 'OP-2',
    auditDate: '2026-10-01',
    nextAuditDate: '2027-04-01',
    compliant: true,
    auditOverdue: false,
  },
  {
    recordId: 'CR-2',
    batchId: 'B-002',
    complianceType: 'EU_EXPORT',
    requirement: 'EU phytosanitary certificate',
    result: 'PENDING',
    evidence: null,
    auditorId: null,
    auditDate: '2026-11-01',
    nextAuditDate: '2026-11-05',
    compliant: false,
    auditOverdue: true,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  complianceApi.getAllComplianceRecords.mockResolvedValue(RECORDS.map((item) => ({ ...item })));
  complianceApi.createComplianceRecord.mockResolvedValue({});
  complianceApi.updateComplianceRecord.mockResolvedValue({});
  complianceApi.deleteComplianceRecord.mockResolvedValue(undefined);
  batchApi.getAllBatches.mockResolvedValue([{ batchId: 'B-001' }, { batchId: 'B-002' }]);
  operatorApi.getOperatorsByRole.mockResolvedValue([
    { operatorId: 'OP-2', operatorName: 'Awa Diop', role: 'AUDITOR' },
  ]);
});

describe('ComplianceRecordList', () => {
  it('renders the records with auditor, type and overdue status', async () => {
    render(<ComplianceRecordList />);

    expect(await screen.findByText('CR-1')).toBeTruthy();
    expect(screen.getByText('CR-2')).toBeTruthy();
    expect(within(screen.getByRole('table')).getByText('Awa Diop')).toBeTruthy();
    expect(within(screen.getByRole('table')).getByText('EU Export')).toBeTruthy();
    expect(screen.getByText('Overdue')).toBeTruthy();
    // Only auditors are requested for the auditor selector.
    expect(operatorApi.getOperatorsByRole).toHaveBeenCalledWith('AUDITOR');
  });

  it('opens pre-filtered when initialBatchId is provided', async () => {
    render(<ComplianceRecordList initialBatchId="B-001" />);

    expect(await screen.findByText('CR-1')).toBeTruthy();
    expect(screen.queryByText('CR-2')).toBeNull();
    expect(screen.getByLabelText('Filter by batch').value).toBe('B-001');
  });

  it('filters by compliance type', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    fireEvent.change(screen.getByLabelText('Filter by type'), { target: { value: 'EU_EXPORT' } });

    expect(screen.queryByText('CR-1')).toBeNull();
    expect(screen.getByText('CR-2')).toBeTruthy();
    expect(screen.getByText('1 records')).toBeTruthy();
  });

  it('filters by audit date', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    fireEvent.change(screen.getByLabelText('Filter by audit date'), {
      target: { value: '2026-10-01' },
    });

    expect(screen.getByText('CR-1')).toBeTruthy();
    expect(screen.queryByText('CR-2')).toBeNull();
  });

  it('filters overdue records only', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    fireEvent.click(screen.getByLabelText('Overdue only'));

    expect(screen.queryByText('CR-1')).toBeNull();
    expect(screen.getByText('CR-2')).toBeTruthy();
  });

  it('creates a record through the API and reloads the list', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    fireEvent.click(screen.getByRole('button', { name: /New Compliance Record/i }));

    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Record ID'), { target: { value: 'CR-3' } });
    fireEvent.change(within(dialog).getByLabelText('Batch'), { target: { value: 'B-002' } });
    fireEvent.change(within(dialog).getByLabelText('Compliance Type'), {
      target: { value: 'FOOD_SAFETY' },
    });
    fireEvent.change(within(dialog).getByLabelText('Requirement'), {
      target: { value: 'Cold chain log' },
    });
    fireEvent.change(within(dialog).getByLabelText('Result'), {
      target: { value: 'COMPLIANT' },
    });
    fireEvent.change(within(dialog).getByLabelText('Audit Date'), {
      target: { value: '2026-11-09' },
    });
    fireEvent.change(within(dialog).getByLabelText('Next Audit Date'), {
      target: { value: '2027-05-09' },
    });
    fireEvent.change(within(dialog).getByLabelText('Auditor'), {
      target: { value: 'OP-2' },
    });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(complianceApi.createComplianceRecord).toHaveBeenCalledWith({
        recordId: 'CR-3',
        batchId: 'B-002',
        complianceType: 'FOOD_SAFETY',
        requirement: 'Cold chain log',
        result: 'COMPLIANT',
        evidence: null,
        auditorId: 'OP-2',
        auditDate: '2026-11-09',
        nextAuditDate: '2027-05-09',
      }),
    );
    await waitFor(() => expect(complianceApi.getAllComplianceRecords).toHaveBeenCalledTimes(2));
  });

  it('blocks creation when required fields are missing', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    fireEvent.click(screen.getByRole('button', { name: /New Compliance Record/i }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Create' }));

    expect(screen.getByText('Record ID is required')).toBeTruthy();
    expect(complianceApi.createComplianceRecord).not.toHaveBeenCalled();
  });

  it('edits an existing record', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    const row = screen.getByText('CR-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Edit' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Record ID').value).toBe('CR-1');
    expect(within(dialog).getByLabelText('Record ID').disabled).toBe(true);
    expect(within(dialog).getByLabelText('Batch').disabled).toBe(true);

    fireEvent.change(within(dialog).getByLabelText('Result'), {
      target: { value: 'NON_COMPLIANT' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(complianceApi.updateComplianceRecord).toHaveBeenCalledWith(
        'CR-1',
        expect.objectContaining({
          result: 'NON_COMPLIANT',
          complianceType: 'HACCP',
          requirement: 'HACCP plan in place',
          auditDate: '2026-10-01',
        }),
      ),
    );
  });

  it('shows the full record details', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    const row = screen.getByText('CR-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'View' }));

    const dialog = screen.getByRole('dialog', { name: /Compliance record details/ });
    expect(within(dialog).getByText('HACCP plan in place')).toBeTruthy();
    expect(within(dialog).getByText('Plan v2')).toBeTruthy();
    expect(within(dialog).getByText('Awa Diop')).toBeTruthy();
  });

  it('deletes a record after confirmation', async () => {
    render(<ComplianceRecordList />);
    await screen.findByText('CR-1');

    const row = screen.getByText('CR-1').closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: 'Delete' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Delete record CR-1/)).toBeTruthy();

    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(complianceApi.deleteComplianceRecord).toHaveBeenCalledWith('CR-1'));
    await waitFor(() => expect(complianceApi.getAllComplianceRecords).toHaveBeenCalledTimes(2));
  });

  it('shows an error state and retries the load', async () => {
    complianceApi.getAllComplianceRecords.mockRejectedValueOnce(new Error('Network down'));

    render(<ComplianceRecordList />);

    expect(await screen.findByText('Network down')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Compliance' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    expect(await screen.findByText('CR-1')).toBeTruthy();
  });
});
