// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { justAfterLocalMidnight } from '../testing/nearMidnight';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PricingPage from './PricingPage';

const api = vi.hoisted(() => ({
  getPricingHistory: vi.fn(),
  getCompetitorPrices: vi.fn(),
  getPricingRecommendations: vi.fn(),
  getProducts: vi.fn(),
  createPricingHistory: vi.fn(),
}));

vi.mock('../api/api', () => ({ api }));

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.getPricingHistory.mockResolvedValue([
    { productId: 1, priceDate: '2026-05-01', priceEurPerKg: 5.5, stockTargetTons: 80 },
    { productId: 1, priceDate: '2026-06-01', priceEurPerKg: 5.8, harvestSeasonFactor: 0.9, stockLevelTons: 60, demandFactor: 1.1, stockTargetTons: 80 },
  ]);
  api.getCompetitorPrices.mockResolvedValue([{ competitorPriceId: 2, productId: 1, competitorName: 'Rival', priceEur: 6.1 }]);
  api.getPricingRecommendations.mockResolvedValue([{ productId: 1, suggestedPriceEur: 6, message: 'Raise slightly.' }]);
  api.getProducts.mockResolvedValue([]);
  api.createPricingHistory.mockResolvedValue({});
});

function renderPage() {
  return render(
    <MemoryRouter>
      <PricingPage />
    </MemoryRouter>
  );
}

describe('PricingPage', () => {
  it('shows the current price, its change, the model variables and the proposed price', async () => {
    renderPage();
    expect(await screen.findByText('€5.80')).toBeInTheDocument();
    expect(screen.getByText('+0.30 (5.5%)')).toBeInTheDocument();
    expect(screen.getByText('Peak')).toBeInTheDocument();
    expect(screen.getByText('60 T')).toBeInTheDocument();
    expect(screen.getByText('1.10 (High)')).toBeInTheDocument();
    expect(screen.getByText('Stock is below target 80T threshold.')).toBeInTheDocument();
    expect(screen.getByText('€6.00')).toBeInTheDocument();
    expect(screen.getByText('Raise slightly.')).toBeInTheDocument();
  });

  it('applies the proposed price with the English labels the API stores', async () => {
    renderPage();
    const apply = await screen.findByRole('button', { name: 'Apply New Pricing' });
    await waitFor(() => expect(apply).not.toBeDisabled());
    fireEvent.click(apply);
    await waitFor(() =>
      expect(api.createPricingHistory).toHaveBeenCalledWith(
        expect.objectContaining({
          productId: 1,
          priceEurPerKg: 6,
          seasonLabel: 'Peak',
          demandIndexLabel: '1.10 (High)',
          stockLevelTons: 60,
          isApplied: true,
        })
      )
    );
    expect(await screen.findByText('New pricing applied successfully.')).toBeInTheDocument();
  });

  it('dates an applied price with the local day just after midnight', async () => {
    const restore = justAfterLocalMidnight();
    try {
      renderPage();
      const apply = await screen.findByRole('button', { name: 'Apply New Pricing' });
      await waitFor(() => expect(apply).not.toBeDisabled());
      fireEvent.click(apply);
      await waitFor(() =>
        expect(api.createPricingHistory).toHaveBeenCalledWith(expect.objectContaining({ priceDate: '2026-04-01' }))
      );
    } finally {
      restore();
    }
  });
});
