// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the other Product Transformation tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import OperatorList from './OperatorList';
import { operatorApi } from '../api/operatorApi';

vi.mock('../api/operatorApi', () => ({
  operatorApi: {
    getAllOperators: vi.fn(),
  },
}));

afterEach(cleanup);

// Shape returned by GET /api/operators.
const OPERATORS = [
  {
    operatorId: 'OP-DEMO-01',
    operatorName: 'Awa DEMO (QC)',
    role: 'QC_INSPECTOR',
    certifications: 'HACCP',
    activeStatus: 'ACTIVE',
    hireDate: '2025-10-07',
  },
  {
    operatorId: 'OP-DEMO-02',
    operatorName: 'Issa DEMO (Sechage)',
    role: 'DRYER',
    certifications: 'Sechage solaire',
    activeStatus: 'ACTIVE',
    hireDate: '2026-01-15',
  },
  {
    operatorId: 'OP-DEMO-03',
    operatorName: 'Fanta DEMO',
    role: 'PACKER',
    certifications: null,
    activeStatus: 'INACTIVE',
    hireDate: '2024-03-02',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  operatorApi.getAllOperators.mockResolvedValue(OPERATORS.map((item) => ({ ...item })));
});

describe('OperatorList', () => {
  it('renders every operator with role, certifications and status', async () => {
    render(<OperatorList />);

    const table = await screen.findByRole('table');
    expect(within(table).getByText('OP-DEMO-01')).toBeTruthy();
    expect(within(table).getByText('Awa DEMO (QC)')).toBeTruthy();
    expect(within(table).getByText('QC Inspector')).toBeTruthy();
    expect(within(table).getByText('HACCP')).toBeTruthy();
    // Two operators are ACTIVE, one INACTIVE: each renders its own badge.
    expect(within(table).getAllByText('Active')).toHaveLength(2);
    expect(within(table).getByText('Oct 7, 2025')).toBeTruthy();
    // Missing certifications fall back to N/A instead of rendering an empty cell.
    expect(within(table).getByText('N/A')).toBeTruthy();
    // Inactive status uses its own badge label.
    expect(within(table).getByText('Inactive')).toBeTruthy();
    expect(operatorApi.getAllOperators).toHaveBeenCalledTimes(1);
  });

  it('filters by role using the roles present in the data', async () => {
    render(<OperatorList />);
    await screen.findByText('OP-DEMO-01');

    // The select is built from the payload, not from a hard-coded enum copy.
    const roleSelect = screen.getByLabelText('Filter by role');
    expect(within(roleSelect).getAllByRole('option')).toHaveLength(4); // All roles + 3 roles

    fireEvent.change(roleSelect, { target: { value: 'DRYER' } });

    expect(screen.queryByText('OP-DEMO-01')).toBeNull();
    expect(screen.getByText('OP-DEMO-02')).toBeTruthy();
    expect(screen.queryByText('OP-DEMO-03')).toBeNull();
  });

  it('filters by status', async () => {
    render(<OperatorList />);
    await screen.findByText('OP-DEMO-01');

    fireEvent.change(screen.getByLabelText('Filter by status'), { target: { value: 'INACTIVE' } });

    expect(screen.getByText('OP-DEMO-03')).toBeTruthy();
    expect(screen.queryByText('OP-DEMO-01')).toBeNull();
  });

  it('searches by id, name or certification', async () => {
    render(<OperatorList />);
    await screen.findByText('OP-DEMO-01');

    const search = screen.getByLabelText('Search operators');

    fireEvent.change(search, { target: { value: 'haccp' } });
    expect(screen.getByText('OP-DEMO-01')).toBeTruthy();
    expect(screen.queryByText('OP-DEMO-02')).toBeNull();

    fireEvent.change(search, { target: { value: 'sechage' } });
    expect(screen.getByText('OP-DEMO-02')).toBeTruthy();
    expect(screen.queryByText('OP-DEMO-01')).toBeNull();
  });

  it('shows the filtered count', async () => {
    render(<OperatorList />);
    await screen.findByText('OP-DEMO-01');

    expect(screen.getByText('3 operators')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Filter by role'), { target: { value: 'DRYER' } });
    expect(screen.getByText('1 operator')).toBeTruthy();
  });

  it('shows an empty state when no operator matches the filters', async () => {
    render(<OperatorList />);
    await screen.findByText('OP-DEMO-01');

    fireEvent.change(screen.getByLabelText('Search operators'), { target: { value: 'zzz' } });

    expect(screen.getByText('No operator matches the filters')).toBeTruthy();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('shows an empty state when the backend returns no operator', async () => {
    operatorApi.getAllOperators.mockResolvedValue([]);

    render(<OperatorList />);

    expect(await screen.findByText('No operator registered')).toBeTruthy();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('shows the API error and retries', async () => {
    operatorApi.getAllOperators.mockRejectedValueOnce(new Error('Service unavailable'));

    render(<OperatorList />);

    expect(await screen.findByText('Service unavailable')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Operators' })).toBeTruthy();

    operatorApi.getAllOperators.mockResolvedValue(OPERATORS.map((item) => ({ ...item })));
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('OP-DEMO-01')).toBeTruthy();
    expect(operatorApi.getAllOperators).toHaveBeenCalledTimes(2);
  });
});
