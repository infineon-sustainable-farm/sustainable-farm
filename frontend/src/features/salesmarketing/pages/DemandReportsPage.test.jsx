// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DemandReportsPage from './DemandReportsPage';

const api = vi.hoisted(() => ({
  getDemandReports: vi.fn(),
  getReportFiles: vi.fn(),
  getDemandReportById: vi.fn(),
  createDemandReport: vi.fn(),
  updateDemandReport: vi.fn(),
  deleteDemandReport: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

const report = {
  reportId: 7,
  reportType: 'Regional',
  periodStart: '2026-01-01',
  periodEnd: '2026-03-31',
  totalForecastedVolumeT: 9000,
  variancePct: -2.8,
  status: 'ready',
  isScheduled: false,
  generatedBy: 'Analyst',
};

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getDemandReports.mockResolvedValue([report]);
  api.getReportFiles.mockResolvedValue([]);
  api.createDemandReport.mockResolvedValue({});
  api.deleteDemandReport.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <DemandReportsPage />
    </MemoryRouter>
  );
}

describe('DemandReportsPage', () => {
  it('shows a report card and its table row with the computed summary', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Regional Report' })).toBeInTheDocument();
    expect(screen.getAllByText('9,000 t forecasted · -2.8% variance')).toHaveLength(2);
    expect(screen.getByRole('table')).toHaveTextContent('Ready');
  });

  it('creates a report from the New Custom Report dialog', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Regional Report' });
    fireEvent.click(screen.getByRole('button', { name: 'New Custom Report' }));
    const dialog = screen.getByRole('dialog', { name: 'New Custom Report' });
    fireEvent.change(within(dialog).getByLabelText('Forecast (t)'), { target: { value: '1200' } });
    fireEvent.change(within(dialog).getByLabelText('Summary'), { target: { value: 'Quarterly view' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Report' }));

    await waitFor(() => expect(api.createDemandReport).toHaveBeenCalledTimes(1));
    expect(api.createDemandReport).toHaveBeenCalledWith(
      expect.objectContaining({ reportType: 'Forecasting', totalForecastedVolumeT: 1200, summary: 'Quarterly view', isScheduled: false })
    );
    expect(await screen.findByText('Report #new created successfully.')).toBeInTheDocument();
  });

  it('opens the dialog as a scheduled report from Schedule Report', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Regional Report' });
    fireEvent.click(screen.getByRole('button', { name: 'Schedule Report' }));
    const dialog = screen.getByRole('dialog', { name: 'New Custom Report' });
    expect(within(dialog).getByLabelText('Scheduled Report')).toBeChecked();
    expect(within(dialog).getByLabelText('Frequency')).toHaveValue('Weekly');
  });

  it('asks for confirmation before deleting a report', async () => {
    renderPage();
    await screen.findByRole('heading', { name: 'Regional Report' });
    fireEvent.click(screen.getByRole('button', { name: 'Delete report' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete report?' });
    expect(within(dialog).getByText(/Are you sure you want to delete Regional Report\?/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.deleteDemandReport).toHaveBeenCalledWith(7));
    expect(await screen.findByText('Report #7 deleted successfully.')).toBeInTheDocument();
  });
});
