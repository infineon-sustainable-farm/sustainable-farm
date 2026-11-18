// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SalesChannelsPage from './SalesChannelsPage';

const api = vi.hoisted(() => ({
  getSalesChannels: vi.fn(),
  getSalesChannelTargets: vi.fn(),
  getOrders: vi.fn(),
  getSalesChannelById: vi.fn(),
  createSalesChannel: vi.fn(),
  updateSalesChannel: vi.fn(),
  deleteSalesChannel: vi.fn(),
  getSalesChannelTargetById: vi.fn(),
  createSalesChannelTarget: vi.fn(),
  updateSalesChannelTarget: vi.fn(),
  deleteSalesChannelTarget: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

const channel = { channelId: 3, name: 'Online Store' };
const target = { targetId: 9, channelId: 3, periodStart: '2026-01-01', periodEnd: '2026-03-31', revenueTargetEur: 450000 };

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getSalesChannels.mockResolvedValue([channel]);
  api.getSalesChannelTargets.mockResolvedValue([target]);
  api.getOrders.mockResolvedValue([]);
  api.createSalesChannel.mockResolvedValue({});
  api.createSalesChannelTarget.mockResolvedValue({});
  api.deleteSalesChannel.mockResolvedValue(null);
  api.deleteSalesChannelTarget.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <SalesChannelsPage />
    </MemoryRouter>
  );
}

describe('SalesChannelsPage', () => {
  it('shows each channel with the revenue target of its latest period', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Online Store' })).toBeInTheDocument();
    expect(screen.getAllByText('€450K').length).toBeGreaterThan(0);
  });

  it('creates a channel from the New Channel dialog', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'New Channel' }));
    const dialog = screen.getByRole('dialog', { name: 'New Sales Channel' });
    fireEvent.change(within(dialog).getByLabelText(/Channel Name/), { target: { value: 'Export Direct' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Channel' }));

    await waitFor(() => expect(api.createSalesChannel).toHaveBeenCalledWith({ name: 'Export Direct' }));
    expect(await screen.findByText('Sales channel "Export Direct" created successfully.')).toBeInTheDocument();
  });

  it('creates a target for a channel from its menu', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Online Store' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Add Target' }));
    const dialog = screen.getByRole('dialog', { name: 'New Sales Target' });
    fireEvent.change(within(dialog).getByLabelText(/Revenue Target/), { target: { value: '1000' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Target' }));

    await waitFor(() =>
      expect(api.createSalesChannelTarget).toHaveBeenCalledWith(expect.objectContaining({ channelId: 3, revenueTargetEur: 1000 }))
    );
    expect(await screen.findByText('Target created successfully.')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting a target', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Manage Targets' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete target' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete target?' });
    expect(within(dialog).getByText(/2026-01-01 - 2026-03-31 \(€450K\)/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(api.deleteSalesChannelTarget).toHaveBeenCalledWith(9));
  });

  it('asks for confirmation before deleting a channel', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Online Store' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Delete Channel' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete channel?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(api.deleteSalesChannel).toHaveBeenCalledWith(3));
    expect(await screen.findByText('Sales channel "Online Store" deleted successfully.')).toBeInTheDocument();
  });

  it('fills the channel edit form from the single channel fetch', async () => {
    api.getSalesChannelById.mockResolvedValue({ channelId: 3, name: 'Online Shop' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Online Store' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Edit Channel' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit Sales Channel #3' });
    expect(within(dialog).getByLabelText(/Channel Name/)).toHaveValue('Online Shop');
  });

  it('fills the target edit form from the single target fetch', async () => {
    api.getSalesChannelTargetById.mockResolvedValue({ ...target, revenueTargetEur: 500000 });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Manage Targets' }));
    fireEvent.click(screen.getByRole('button', { name: 'Edit target' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit Target #9' });
    expect(within(dialog).getByLabelText(/Revenue Target/)).toHaveValue(500000);
  });
});
