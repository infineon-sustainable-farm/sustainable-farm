// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CampaignsPage from './CampaignsPage';

const api = vi.hoisted(() => ({
  getCampaigns: vi.fn(),
  getCampaignMetrics: vi.fn(),
  getCampaignById: vi.fn(),
  createCampaign: vi.fn(),
  updateCampaign: vi.fn(),
  deleteCampaign: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

const campaign = {
  campaignId: 4,
  name: 'Mango season',
  status: 'active',
  budgetEur: 12000,
  budgetSpentEur: 3500,
  estimatedReach: 25000,
  currency: 'EUR',
  startDate: '2026-03-05T12:00:00',
  endDate: '2026-04-30T12:00:00',
};

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getCampaigns.mockResolvedValue([campaign]);
  api.getCampaignMetrics.mockResolvedValue([]);
  api.createCampaign.mockResolvedValue({});
  api.deleteCampaign.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <CampaignsPage />
    </MemoryRouter>
  );
}

describe('CampaignsPage', () => {
  it('shows the campaigns with their budget and dates', async () => {
    renderPage();
    expect(await screen.findByRole('button', { name: 'Mango season' })).toBeInTheDocument();
    expect(screen.getByText('EUR12k')).toBeInTheDocument();
    expect(screen.getByText('Mar 5 - Apr 30')).toBeInTheDocument();
    expect(screen.getByText('EUR3,500')).toBeInTheDocument();
  });

  it('creates a campaign from the New campaign dialog', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'NEW CAMPAIGN' }));
    const dialog = screen.getByRole('dialog', { name: 'New Campaign' });
    fireEvent.change(within(dialog).getByLabelText(/Campaign Name/), { target: { value: 'Harvest fair' } });
    fireEvent.change(within(dialog).getByLabelText('Budget (EUR)'), { target: { value: '500' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(api.createCampaign).toHaveBeenCalledTimes(1));
    expect(api.createCampaign).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Harvest fair', status: 'draft', budgetEur: 500, budgetSpentEur: null })
    );
    expect(await screen.findByText('Campaign created successfully.')).toBeInTheDocument();
  });

  it('asks for confirmation before deleting a campaign', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Delete Mango season' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete campaign?' });
    expect(within(dialog).getByText(/Are you sure you want to delete Mango season\?/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(api.deleteCampaign).toHaveBeenCalledWith(4));
    expect(await screen.findByText('Campaign deleted successfully.')).toBeInTheDocument();
  });

  it('opens the edit dialog filled from the single campaign fetch', async () => {
    api.getCampaignById.mockResolvedValue({ ...campaign, name: 'Mango season 2026' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Mango season' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit Campaign #4' });
    expect(within(dialog).getByLabelText(/Campaign Name/)).toHaveValue('Mango season 2026');
    expect(api.getCampaignById).toHaveBeenCalledWith(4);
  });
});
