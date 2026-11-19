// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ComplianceScreen from './ComplianceScreen';
import PackagingScreen from './PackagingScreen';

const navigateMock = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => navigateMock };
});

vi.mock('./ComplianceRecordList', () => ({
  default: ({ initialBatchId }) => (
    <div data-testid="compliance-child">{initialBatchId || 'no-filter'}</div>
  ),
}));

vi.mock('./PackagingRecordList', () => ({
  default: ({ onOpenCompliance }) => (
    <div>
      <button type="button" onClick={() => onOpenCompliance('B-001')}>
        open batch
      </button>
      <button type="button" onClick={() => onOpenCompliance('')}>
        open all
      </button>
    </div>
  ),
}));

afterEach(cleanup);

beforeEach(() => {
  navigateMock.mockReset();
});

describe('ComplianceScreen', () => {
  it('passes the batchId query parameter to the compliance list', () => {
    render(
      <MemoryRouter initialEntries={['/product-transformation/compliance?batchId=B-002']}>
        <ComplianceScreen />
      </MemoryRouter>,
    );

    expect(screen.getByTestId('compliance-child').textContent).toBe('B-002');
  });

  it('passes an empty filter when no batchId is present', () => {
    render(
      <MemoryRouter initialEntries={['/product-transformation/compliance']}>
        <ComplianceScreen />
      </MemoryRouter>,
    );

    expect(screen.getByTestId('compliance-child').textContent).toBe('no-filter');
  });
});

describe('PackagingScreen', () => {
  it('opens compliance with an encoded batch filter', () => {
    render(
      <MemoryRouter>
        <PackagingScreen />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'open batch' }));

    expect(navigateMock).toHaveBeenCalledWith(
      '/product-transformation/compliance?batchId=B-001',
    );
  });

  it('opens compliance without a filter when the batch is missing', () => {
    render(
      <MemoryRouter>
        <PackagingScreen />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'open all' }));

    expect(navigateMock).toHaveBeenCalledWith('/product-transformation/compliance');
  });
});
