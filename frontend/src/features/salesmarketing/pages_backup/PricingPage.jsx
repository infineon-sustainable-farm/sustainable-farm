
import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

function seasonFromValue(v) {
  if (v > 80) return 'Peak';
  if (v > 40) return 'Mid-Season';
  return 'Early';
}

function demandLabel(v) {
  const idx = (v / 100).toFixed(2);
  return `${idx}${v > 105 ? ' (High)' : ' (Norm)'}`;
}

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  if (data.message) return data.message;
  return fallback;
}

/**
 * Builds the SVG trend path from one product's price history.
 */
function buildTrendPath(history) {
  const points = history
    .filter((item) => item.priceEurPerKg != null)
    .sort((a, b) => new Date(a.priceDate) - new Date(b.priceDate))
    .slice(-30);

  if (points.length < 2) return null;

  const prices = points.map((item) => Number(item.priceEurPerKg));
  const min = Math.min(...prices);
  const max = Math.max(...prices);

  // Keep a little visual padding around the real values.
  const padding = Math.max((max - min) * 0.2, 0.05);
  const chartMin = min - padding;
  const chartMax = max + padding;
  const range = chartMax - chartMin || 1;

  const coordinates = prices.map((price, index) => {
    const x = (index / (prices.length - 1)) * 100;
    const y = 90 - ((price - chartMin) / range) * 75;

    return `${index === 0 ? 'M' : 'L'}${x},${y}`;
  });

  return {
    line: coordinates.join(' '),
    area: `${coordinates.join(' ')} L100,100 L0,100 Z`,
    min,
    max,
    points,
  };
}

function formatPrice(value) {
  return value != null ? `ââ€šÂ¬${Number(value).toFixed(2)}` : '—';
}

export default function PricingPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  const [history, setHistory] = useState([]);
  const [competitors, setCompetitors] = useState([]);
  const [recommendation, setRecommendation] = useState(null);
  const [products, setProducts] = useState([]);

  const [selectedCompId, setSelectedCompId] = useState(null);
  const [applying, setApplying] = useState(false);

  const [harvest, setHarvest] = useState(1);
  const [stock, setStock] = useState(1000);
  const [demand, setDemand] = useState(50);

  useEffect(() => {
    fetchPricingData();
  }, []);

  const flashSuccess = (message) => {
    setActionSuccess(message);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (message) => {
    setActionError(message);
    setActionSuccess(null);
  };

  const fetchPricingData = async () => {
    setLoading(true);
    setApiError(null);

    try {
      const [histRes, compRes, recoRes, productRes] = await Promise.allSettled([
        api.getPricingHistory(),
        api.getCompetitorPrices(),
        api.getPricingRecommendations(),
        api.getProducts(),
      ]);

      const failures = [];

      // -----------------------------
      // Pricing history
      // -----------------------------
      if (histRes.status === 'fulfilled' && Array.isArray(histRes.value.data)) {
        const sorted = [...histRes.value.data].sort(
          (a, b) => new Date(a.priceDate) - new Date(b.priceDate)
        );

        setHistory(sorted);

        if (sorted.length > 0) {
          const latest = sorted[sorted.length - 1];

          if (latest.harvestSeasonFactor != null) {
            setHarvest(
              Math.round(Number(latest.harvestSeasonFactor) * 100)
            );
          }

          if (latest.stockLevelTons != null) {
            setStock(Number(latest.stockLevelTons));
          }

          if (latest.demandFactor != null) {
            setDemand(
              Math.round(Number(latest.demandFactor) * 100)
            );
          }
        }
      } else {
        setHistory([]);

        if (histRes.status === 'rejected') {
          failures.push(
            `Pricing history: ${getErrorMessage(histRes.reason)}`
          );
        }
      }

      // -----------------------------
      // Competitors
      // -----------------------------
      if (compRes.status === 'fulfilled' && Array.isArray(compRes.value.data)) {
        setCompetitors(compRes.value.data);
      } else {
        setCompetitors([]);

        if (compRes.status === 'rejected') {
          failures.push(
            `Competitors: ${getErrorMessage(compRes.reason)}`
          );
        }
      }

      // -----------------------------
      // Recommendations
      // -----------------------------
      if (recoRes.status === 'fulfilled' && Array.isArray(recoRes.value.data)) {
        setRecommendation(
          recoRes.value.data.length > 0
            ? recoRes.value.data[recoRes.value.data.length - 1]
            : null
        );
      } else {
        setRecommendation(null);

        if (recoRes.status === 'rejected') {
          failures.push(
            `Recommendations: ${getErrorMessage(recoRes.reason)}`
          );
        }
      }

      // -----------------------------
      // Products
      // -----------------------------
      if (productRes.status === 'fulfilled') {
        setProducts(
          Array.isArray(productRes.value.data)
            ? productRes.value.data
            : []
        );
      } else {
        setProducts([]);

        failures.push(
          `Products: ${getErrorMessage(productRes.reason)}`
        );
      }

      if (failures.length > 0) {
        setApiError(`Partial load — ${failures.join(' · ')}`);
      }
    } catch (err) {
      setApiError(
        getErrorMessage(
          err,
          'Error connecting to Spring Boot backend API at http://localhost:8080/api'
        )
      );
    } finally {
      setLoading(false);
    }
  };

  // --------------------------------------------------
  // Current product / latest pricing
  // --------------------------------------------------

  const latest = history.length > 0
    ? history[history.length - 1]
    : null;

  const currentProductId = latest?.productId ?? null;

  // Only use pricing history for the current product.
  const productHistory = currentProductId != null
    ? history.filter(
        (item) => item.productId === currentProductId
      )
    : [];

  const previous = productHistory.length > 1
    ? productHistory[productHistory.length - 2]
    : null;

  const currentPrice =
    latest?.priceEurPerKg != null
      ? Number(latest.priceEurPerKg)
      : null;

  const prevPrice =
    previous?.priceEurPerKg != null
      ? Number(previous.priceEurPerKg)
      : null;

  const delta =
    currentPrice != null && prevPrice != null
      ? currentPrice - prevPrice
      : null;

  const deltaPct =
    delta != null && prevPrice
      ? (delta / prevPrice) * 100
      : null;

  // --------------------------------------------------
  // Recommendation for current product
  // --------------------------------------------------

  const productRecommendation =
    currentProductId != null
      ? recommendation?.productId === currentProductId
        ? recommendation
        : null
      : recommendation;

  const proposed =
    productRecommendation?.suggestedPriceEur != null
      ? Number(productRecommendation.suggestedPriceEur)
      : null;

  const proposedDelta =
    proposed != null && currentPrice != null
      ? proposed - currentPrice
      : null;

  // --------------------------------------------------
  // Trend
  // --------------------------------------------------

  const trendHistory = productHistory;

  const trend = buildTrendPath(trendHistory);

  // --------------------------------------------------
  // Competitors for current product
  // --------------------------------------------------

  const productCompetitors =
    currentProductId != null
      ? competitors.filter(
          (comp) => comp.productId === currentProductId
        )
      : competitors;

  const activeCompId =
    selectedCompId != null &&
    productCompetitors.some(
      (comp) => comp.competitorPriceId === selectedCompId
    )
      ? selectedCompId
      : productCompetitors.find((comp) => comp.isTarget)?.competitorPriceId ??
        productCompetitors[0]?.competitorPriceId;

  const selectedCompetitor = productCompetitors.find(
    (comp) => comp.competitorPriceId === activeCompId
  );

  const pricingProductId =
    currentProductId ??
    selectedCompetitor?.productId ??
    products[0]?.productId;

  // --------------------------------------------------
  // Apply pricing
  // --------------------------------------------------

  const handleApplyPricing = async () => {
    if (proposed == null || pricingProductId == null) {
      flashError(
        'A suggested price and product are required before pricing can be applied.'
      );
      return;
    }

    setApplying(true);
    setActionError(null);

    try {
      await api.createPricingHistory({
        productId: pricingProductId,
        priceDate: new Date().toISOString().slice(0, 10),
        priceEurPerKg: proposed,
        harvestSeasonFactor: harvest / 100,
        seasonLabel: seasonFromValue(harvest),
        stockLevelTons: stock,
        stockTargetTons: latest?.stockTargetTons ?? null,
        demandFactor: demand / 100,
        demandIndexLabel: demandLabel(demand),
        isApplied: true,
      });

      flashSuccess('New pricing applied successfully.');
      await fetchPricingData();
    } catch (err) {
      flashError(
        getErrorMessage(err, 'Failed to apply new pricing')
      );
    } finally {
      setApplying(false);
    }
  };

  // --------------------------------------------------
  // Sliders
  // --------------------------------------------------

  const stockPct = Math.min(
    100,
    Math.max(0, (stock / 150) * 100)
  );

  const demandPct = Math.min(
    100,
    Math.max(0, ((demand - 50) / 100) * 100)
  );

  // --------------------------------------------------
  // Trend axis
  // --------------------------------------------------

  const trendMin = trend?.min ?? 5.7;
  const trendMax = trend?.max ?? 6.0;

  const trendPadding = Math.max(
    (trendMax - trendMin) * 0.2,
    0.05
  );



const axisBottom = trendMin;
const axisTop = trendMax;
const axisMiddle = (trendMax + trendMin) / 2;

  const trendPoints = trend?.points ?? [];

  const firstTrendMonth =
    trendPoints[0]?.priceDate
      ? new Date(trendPoints[0].priceDate).toLocaleDateString(
          'en-US',
          { month: 'short' }
        )
      : 'Jun';

  const middleTrendMonth =
    trendPoints.length >= 2
      ? new Date(
          trendPoints[Math.floor((trendPoints.length - 1) / 2)].priceDate
        ).toLocaleDateString('en-US', { month: 'short' })
      : 'Jul';
const thirdTrendMonth =
  trendPoints.length >= 3
    ? new Date(
        trendPoints[Math.floor((trendPoints.length - 1) * 0.67)].priceDate
      ).toLocaleDateString('en-US', { month: 'short' })
    : 'Aug';
  const lastTrendMonth =
    trendPoints[trendPoints.length - 1]?.priceDate
      ? new Date(
          trendPoints[trendPoints.length - 1].priceDate
        ).toLocaleDateString('en-US', { month: 'short' })
      : 'Aug';

  return (
    <div className="flex flex-col w-full relative">
      <div className="flex flex-col lg:flex-row gap-gutter p-margin-mobile lg:p-margin-desktop min-h-[calc(100vh-80px)] items-start">

        {apiError && (
          <div className="w-full absolute top-4 left-0 px-margin-desktop z-20">
            <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm">
              <div className="flex items-center gap-md">
                <span className="material-symbols-outlined text-secondary text-[24px]">
                  wifi_off
                </span>
                <div>
                  <p className="font-label-md text-label-md text-on-surface font-bold">
                    API Connection Notice
                  </p>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">
                    {apiError}
                  </p>
                </div>
              </div>

              <button
                onClick={fetchPricingData}
                className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
              >
                <span className="material-symbols-outlined text-[16px]">
                  refresh
                </span>
                Retry
              </button>
            </div>
          </div>
        )}

        {actionError && (
          <div className="w-full absolute top-4 left-0 px-margin-desktop z-20">
            <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm">
              <p className="font-body-sm text-body-sm text-on-error-container">
                {actionError}
              </p>

              <button
                type="button"
                onClick={() => setActionError(null)}
                className="text-on-error-container px-sm"
              >
                <span className="material-symbols-outlined">
                  close
                </span>
              </button>
            </div>
          </div>
        )}

        {actionSuccess && (
          <div className="w-full absolute top-4 left-0 px-margin-desktop z-20">
            <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm">
              <p className="font-body-sm text-body-sm text-[#1b5e20]">
                {actionSuccess}
              </p>

              <button
                type="button"
                onClick={() => setActionSuccess(null)}
                className="text-[#1b5e20] px-sm"
              >
                <span className="material-symbols-outlined">
                  close
                </span>
              </button>
            </div>
          </div>
        )}

        {/* LEFT COLUMN */}
        <div className="w-full lg:w-5/12 flex flex-col gap-gutter">

          {/* Current Price */}
          <div className="bg-surface-container-lowest rounded-xl shadow-md p-lg flex flex-col relative overflow-hidden group hover:-translate-y-1 transition-transform duration-300">
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-bl-full -mr-8 -mt-8 transition-transform group-hover:scale-110" />

            <div className="flex items-center gap-xs mb-sm">
              <span className="material-symbols-outlined text-primary text-[20px]">
                sell
              </span>

              <span className="font-label-md text-label-md text-primary uppercase tracking-wider">
                Dynamic Pricing Engine
              </span>
            </div>

            <div className="mt-md">
              <span className="font-body-sm text-body-sm text-on-surface-variant mb-unit block">
                Current Market Price (FOB)
              </span>

              <div className="flex items-baseline gap-sm">
                <span className="font-display-lg text-display-lg text-primary">
                  {loading
                    ? 'ââ‚¬Â¦'
                    : currentPrice != null
                      ? `ââ€šÂ¬${currentPrice.toFixed(2)}`
                      : '—'}
                </span>

                <span className="font-body-md text-body-md text-on-surface-variant">
                  / kg
                </span>
              </div>
            </div>

            <div className="flex items-center gap-sm mt-md bg-surface-container-low w-fit px-md py-sm rounded-full">
              <span className="material-symbols-outlined text-secondary text-[18px]">
                {delta == null || delta >= 0
                  ? 'trending_up'
                  : 'trending_down'}
              </span>

              <span className="font-label-md text-label-md text-secondary">
                {deltaPct == null
                  ? 'Awaiting price history'
                  : `${delta >= 0 ? '+' : ''}${delta.toFixed(2)} (${deltaPct.toFixed(1)}%)`}
              </span>

              <span className="font-mono-label text-mono-label text-on-surface-variant opacity-70 ml-xs">
                vs previous month
              </span>
            </div>
          </div>
{/* Monthly Price Trend */}
<div className="bg-surface-container-lowest rounded-xl shadow-md p-lg flex-1 min-h-[300px] flex flex-col">
  <div className="flex justify-between items-center mb-md">
    <h2 className="font-headline-sm text-headline-sm text-on-surface">
      Monthly Price Trend
    </h2>

    <div className="flex gap-xs items-center">
      <span className="w-2 h-2 rounded-full bg-primary" />

      <span className="font-label-md text-label-md text-on-surface-variant">
        Avg Price
      </span>
    </div>
  </div>

  <div className="relative w-full h-[240px] mt-md">

    {/* Y AXIS */}
    <div className="absolute left-0 top-0 bottom-8 w-14 flex flex-col justify-between text-on-surface-variant font-mono-label text-mono-label opacity-60">
      <span>{formatPrice(axisTop)}</span>
      <span>{formatPrice(axisMiddle)}</span>
      <span>{formatPrice(axisBottom)}</span>
    </div>

    {/* CHART AREA */}
    <div className="absolute left-16 right-0 top-0 bottom-8">

      {/* GRID */}
      <div className="absolute inset-0 flex flex-col justify-between pointer-events-none">
        <div className="w-full border-t border-surface-container-highest" />
        <div className="w-full border-t border-surface-container-highest" />
        <div className="w-full border-t border-surface-container-highest" />
      </div>

      {/* SVG */}
      {trend ? (
        <svg
          className="absolute inset-0 w-full h-full overflow-visible"
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          aria-label="Monthly price trend"
        >
          {/* Area */}
          <path
            d={trend.area}
            fill="rgba(10, 130, 118, 0.10)"
            stroke="none"
          />

          {/* Line */}
          <path
            d={trend.line}
            fill="none"
            stroke="#0A8276"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
          />

          {/* Points */}
          {trendPoints.map((point, index) => {
            if (trendPoints.length < 2) return null;

            const x =
              (index / (trendPoints.length - 1)) * 100;

            const prices = trendPoints.map((p) =>
              Number(p.priceEurPerKg)
            );

            const min = Math.min(...prices);
            const max = Math.max(...prices);

            const padding = Math.max(
              (max - min) * 0.2,
              0.05
            );

            const chartMin = min - padding;
            const chartMax = max + padding;
            const range = chartMax - chartMin || 1;

            const y =
              90 -
              ((Number(point.priceEurPerKg) - chartMin) /
                range) *
                75;

            return (
              <circle
                key={`${point.priceDate}-${index}`}
                cx={x}
                cy={y}
                r="3"
                fill="#0A8276"
                vectorEffect="non-scaling-stroke"
              />
            );
          })}
        </svg>
      ) : (
        <div className="absolute inset-0 flex items-center justify-center font-body-sm text-on-surface-variant">
          Not enough price history to draw a trend.
        </div>
      )}
    </div>

    {/* X AXIS */}
    <div className="absolute left-16 right-0 bottom-0 h-8 flex justify-between items-end font-mono-label text-mono-label text-on-surface-variant opacity-60">
      <span>{firstTrendMonth}</span>
<span>{middleTrendMonth}</span>
<span>{thirdTrendMonth}</span>
<span>{lastTrendMonth}</span>
    </div>
  </div>
</div>
          {/* Recommendation */}
          <div className="bg-primary text-on-primary rounded-xl shadow-lg p-lg relative overflow-hidden">
            <div className="absolute -right-4 -bottom-4 w-24 h-24 bg-on-primary/10 rounded-full blur-xl" />

            <div className="flex items-start gap-md relative z-10">
              <span className="material-symbols-outlined text-[24px]">
                lightbulb
              </span>

              <div>
                <h3 className="font-label-md text-label-md uppercase tracking-wider mb-xs">
                  Engine Recommendation
                </h3>

                <p className="font-body-sm text-body-sm opacity-90">
                  {productRecommendation?.message ||
                    'No pricing recommendation is available yet.'}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN */}
        <div className="w-full lg:w-7/12 flex flex-col gap-gutter">

          <div className="bg-surface-container-lowest rounded-xl shadow-md p-lg lg:p-xl flex flex-col">

            <h2 className="font-headline-sm text-headline-sm text-on-surface mb-lg">
              Pricing Model Variables
            </h2>

            <div className="flex flex-col gap-xl">

              {/* HARVEST */}
              <div className="group">
                <div className="flex justify-between items-center mb-sm">
                  <label className="font-label-md text-label-md text-on-surface flex items-center gap-xs">
                    <span className="material-symbols-outlined text-[16px] text-primary">
                      eco
                    </span>
                    Harvest Season Impact
                  </label>

                  <span className="font-mono-label text-mono-label text-primary">
                    {seasonFromValue(harvest)}
                  </span>
                </div>

                <div className="relative w-full h-2 bg-surface-container-highest rounded-full">
                  <div
                    className="absolute left-0 top-0 h-full bg-primary rounded-full transition-all duration-300"
                    style={{ width: `${harvest}%` }}
                  />

                  <input
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    type="range"
                    min="1"
                    max="100"
                    value={harvest}
                    onChange={(e) =>
                      setHarvest(Number(e.target.value))
                    }
                  />

                  <div
                    className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 bg-surface-container-lowest rounded-full shadow-[0_1px_3px_rgba(10,130,118,0.4)] pointer-events-none group-hover:scale-110 transition-transform"
                    style={{ left: `${harvest}%` }}
                  />
                </div>

                <p className="font-body-sm text-body-sm text-on-surface-variant opacity-70 mt-xs">
                  Current yield volume applying downward pressure.
                </p>
              </div>

              {/* STOCK */}
              <div className="group">
                <div className="flex justify-between items-center mb-sm">
                  <label className="font-label-md text-label-md text-on-surface flex items-center gap-xs">
                    <span className="material-symbols-outlined text-[16px] text-primary">
                      inventory_2
                    </span>
                    Current Stock Level (Tons)
                  </label>

                  <span className="font-mono-label text-mono-label text-primary">
                    {Number(stock).toLocaleString()} T
                  </span>
                </div>

                <div className="relative w-full h-2 bg-surface-container-highest rounded-full">
                  <div
                    className="absolute left-0 top-0 h-full bg-secondary rounded-full transition-all duration-300"
                    style={{ width: `${stockPct}%` }}
                  />

                  <input
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    type="range"
                    min="0"
                    max="150"
                    value={stock}
                    onChange={(e) =>
                      setStock(Number(e.target.value))
                    }
                  />

                  <div
                    className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 bg-surface-container-lowest rounded-full shadow-[0_1px_3px_rgba(156,68,0,0.4)] pointer-events-none group-hover:scale-110 transition-transform"
                    style={{ left: `${stockPct}%` }}
                  />
                </div>

                <p className="font-body-sm text-body-sm text-on-surface-variant opacity-70 mt-xs">
                  Stock is{' '}
                  {latest?.stockTargetTons != null &&
                  stock < latest.stockTargetTons
                    ? 'below'
                    : 'at/above'}{' '}
                  target {latest?.stockTargetTons ?? '—'}T threshold.
                </p>
              </div>

              {/* DEMAND */}
              <div className="group">
                <div className="flex justify-between items-center mb-sm">
                  <label className="font-label-md text-label-md text-on-surface flex items-center gap-xs">
                    <span className="material-symbols-outlined text-[16px] text-primary">
                      monitoring
                    </span>
                    Forward Demand Index
                  </label>

                  <span className="font-mono-label text-mono-label text-primary">
                    {demandLabel(demand)}
                  </span>
                </div>

                <div className="relative w-full h-2 bg-surface-container-highest rounded-full">
                  <div
                    className="absolute left-0 top-0 h-full bg-primary rounded-full transition-all duration-300"
                    style={{ width: `${demandPct}%` }}
                  />

                  <input
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    type="range"
                    min="50"
                    max="150"
                    value={demand}
                    onChange={(e) =>
                      setDemand(Number(e.target.value))
                    }
                  />

                  <div
                    className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 bg-surface-container-lowest rounded-full shadow-[0_1px_3px_rgba(10,130,118,0.4)] pointer-events-none group-hover:scale-110 transition-transform"
                    style={{ left: `${demandPct}%` }}
                  />
                </div>
              </div>

              {/* COMPETITORS */}
              <div className="pt-md mt-sm border-t border-surface-container-highest relative">
                <label className="font-label-md text-label-md text-on-surface flex items-center gap-xs mb-md">
                  <span className="material-symbols-outlined text-[16px] text-primary">
                    compare_arrows
                  </span>
                  Competitor Baseline Shift (ââ€šÂ¬)
                </label>

                <div className="grid grid-cols-3 gap-md">
                  {productCompetitors.slice(0, 3).map((comp) => {
                    const active =
                      comp.competitorPriceId === activeCompId;

                    return (
                      <button
                        key={comp.competitorPriceId}
                        type="button"
                        onClick={() =>
                          setSelectedCompId(
                            comp.competitorPriceId
                          )
                        }
                        className={`p-sm rounded-lg flex flex-col items-center justify-center border transition-colors relative ${
                          active
                            ? 'bg-primary/5 border-primary'
                            : 'bg-surface border-surface-container-highest hover:border-primary/50'
                        }`}
                      >
                        {active && (
                          <div className="absolute -top-2 -right-2 w-4 h-4 bg-primary text-on-primary rounded-full flex items-center justify-center">
                            <span className="material-symbols-outlined text-[10px]">
                              check
                            </span>
                          </div>
                        )}

                        <span
                          className={`font-mono-label text-mono-label mb-xs ${
                            active
                              ? 'text-primary'
                              : 'text-on-surface-variant'
                          }`}
                        >
                          {comp.competitorName}
                        </span>

                        <span
                          className={`font-body-md text-body-md ${
                            active
                              ? 'text-primary'
                              : comp.competitorName?.includes('A')
                                ? 'text-secondary'
                                : 'text-on-surface'
                          }`}
                        >
                          ââ€šÂ¬{Number(comp.priceEur).toFixed(2)}
                        </span>
                      </button>
                    );
                  })}

                  {productCompetitors.length === 0 && (
                    <p className="col-span-3 font-body-sm text-body-sm text-on-surface-variant">
                      No competitor prices available.
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* PROPOSED PRICE */}
            <div className="mt-xxl pt-lg border-t border-surface-container-highest flex flex-col sm:flex-row items-center justify-between gap-lg relative">
              <div className="flex flex-col">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase">
                  Proposed Price
                </span>

                <div className="flex items-baseline gap-sm">
                  <span className="font-headline-lg text-headline-lg text-primary">
                    {proposed != null
                      ? `ââ€šÂ¬${proposed.toFixed(2)}`
                      : '—'}
                  </span>

                  <span className="font-body-sm text-body-sm text-secondary font-medium">
                    {proposedDelta == null
                      ? 'Awaiting recommendation'
                      : `${proposedDelta >= 0 ? '+' : ''}ââ€šÂ¬${proposedDelta.toFixed(2)}`}
                  </span>
                </div>
              </div>

              <button
                type="button"
                disabled={
                  applying ||
                  proposed == null ||
                  pricingProductId == null
                }
                onClick={handleApplyPricing}
                className="bg-primary text-on-primary font-headline-sm text-headline-sm px-xl py-md rounded-xl hover:bg-on-primary-fixed-variant transition-colors shadow-md hover:shadow-lg w-full sm:w-auto flex items-center justify-center gap-sm disabled:opacity-60"
              >
                <span className="material-symbols-outlined">
                  publish
                </span>

                {applying
                  ? 'Applyingââ‚¬Â¦'
                  : 'Apply New Pricing'}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="absolute top-1/4 right-0 w-[40vw] h-[40vw] bg-tertiary-fixed-dim/20 rounded-full blur-[100px] pointer-events-none -z-10 -mr-[20vw]" />
    </div>
  );
}
