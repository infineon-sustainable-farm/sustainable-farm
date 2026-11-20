import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import { PageHeader } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import { useLater } from '../components/common/useLater';
import CurrentPriceCard from '../components/pricing/CurrentPriceCard';
import PriceTrendChart from '../components/pricing/PriceTrendChart';
import PricingVariables from '../components/pricing/PricingVariables';
import RecommendationCard from '../components/pricing/RecommendationCard';
import { buildTrend, demandLabel, getErrorMessage, seasonFromValue } from '../components/pricing/pricingModel';

export default function PricingPage() {
  const { t } = useTranslation('salesmarketing');
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

  const later = useLater();
  const flashSuccess = (message) => {
    setActionSuccess(message);
    setActionError(null);
    later(() => setActionSuccess(null), 4000);
  };

  const flashError = (message) => {
    setActionError(message);
    setActionSuccess(null);
  };

  const fetchPricingData = useCallback(async () => {
    const requestFailed = t('pricing.requestFailed');
    try {
      const [histRes, compRes, recoRes, productRes] = await Promise.allSettled([
        api.getPricingHistory(),
        api.getCompetitorPrices(),
        api.getPricingRecommendations(),
        api.getProducts(),
      ]);
      setApiError(null);

      const failures = [];

      if (histRes.status === 'fulfilled' && Array.isArray(histRes.value)) {
        const sorted = [...histRes.value].sort((a, b) => new Date(a.priceDate) - new Date(b.priceDate));
        setHistory(sorted);
        if (sorted.length > 0) {
          const latest = sorted[sorted.length - 1];
          if (latest.harvestSeasonFactor != null) setHarvest(Math.round(Number(latest.harvestSeasonFactor) * 100));
          if (latest.stockLevelTons != null) setStock(Number(latest.stockLevelTons));
          if (latest.demandFactor != null) setDemand(Math.round(Number(latest.demandFactor) * 100));
        }
      } else {
        setHistory([]);
        if (histRes.status === 'rejected') {
          failures.push(t('pricing.failure.history', { message: getErrorMessage(histRes.reason, requestFailed) }));
        }
      }

      if (compRes.status === 'fulfilled' && Array.isArray(compRes.value)) {
        setCompetitors(compRes.value);
      } else {
        setCompetitors([]);
        if (compRes.status === 'rejected') {
          failures.push(t('pricing.failure.competitors', { message: getErrorMessage(compRes.reason, requestFailed) }));
        }
      }

      if (recoRes.status === 'fulfilled' && Array.isArray(recoRes.value)) {
        setRecommendation(recoRes.value.length > 0 ? recoRes.value[recoRes.value.length - 1] : null);
      } else {
        setRecommendation(null);
        if (recoRes.status === 'rejected') {
          failures.push(
            t('pricing.failure.recommendations', { message: getErrorMessage(recoRes.reason, requestFailed) })
          );
        }
      }

      if (productRes.status === 'fulfilled') {
        setProducts(Array.isArray(productRes.value) ? productRes.value : []);
      } else {
        setProducts([]);
        failures.push(t('pricing.failure.products', { message: getErrorMessage(productRes.reason, requestFailed) }));
      }

      if (failures.length > 0) {
        setApiError(t('pricing.partialLoad', { failures: failures.join(t('pricing.failureSeparator')) }));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('pricing.loadDataFailed')));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchPricingData);
  }, [fetchPricingData]);

  // Current product: the product of the latest price.
  const latest = history.length > 0 ? history[history.length - 1] : null;
  const currentProductId = latest?.productId ?? null;
  const productHistory = currentProductId != null ? history.filter((item) => item.productId === currentProductId) : [];
  const previous = productHistory.length > 1 ? productHistory[productHistory.length - 2] : null;
  const currentPrice = latest?.priceEurPerKg != null ? Number(latest.priceEurPerKg) : null;
  const prevPrice = previous?.priceEurPerKg != null ? Number(previous.priceEurPerKg) : null;
  const delta = currentPrice != null && prevPrice != null ? currentPrice - prevPrice : null;
  const deltaPct = delta != null && prevPrice ? (delta / prevPrice) * 100 : null;

  const productRecommendation =
    currentProductId != null
      ? recommendation?.productId === currentProductId
        ? recommendation
        : null
      : recommendation;
  const proposed =
    productRecommendation?.suggestedPriceEur != null ? Number(productRecommendation.suggestedPriceEur) : null;
  const proposedDelta = proposed != null && currentPrice != null ? proposed - currentPrice : null;

  const trend = buildTrend(productHistory);

  const productCompetitors =
    currentProductId != null ? competitors.filter((comp) => comp.productId === currentProductId) : competitors;
  const activeCompId =
    selectedCompId != null && productCompetitors.some((comp) => comp.competitorPriceId === selectedCompId)
      ? selectedCompId
      : productCompetitors.find((comp) => comp.isTarget)?.competitorPriceId ?? productCompetitors[0]?.competitorPriceId;
  const selectedCompetitor = productCompetitors.find((comp) => comp.competitorPriceId === activeCompId);
  const pricingProductId = currentProductId ?? selectedCompetitor?.productId ?? products[0]?.productId;

  const handleApplyPricing = async () => {
    if (proposed == null || pricingProductId == null) {
      flashError(t('pricing.applyRequirements'));
      return;
    }
    setApplying(true);
    setActionError(null);
    try {
      await api.createPricingHistory({
        productId: pricingProductId,
        priceDate: todayIsoDate(),
        priceEurPerKg: proposed,
        harvestSeasonFactor: harvest / 100,
        seasonLabel: seasonFromValue(harvest),
        stockLevelTons: stock,
        stockTargetTons: latest?.stockTargetTons ?? null,
        demandFactor: demand / 100,
        demandIndexLabel: demandLabel(demand),
        isApplied: true,
      });
      flashSuccess(t('pricing.applied'));
      await fetchPricingData();
    } catch (err) {
      flashError(getErrorMessage(err, t('pricing.applyFailed')));
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t('nav.pricing')} />

      {apiError && (
        <Notice tone="offline" title={t('pricing.apiNotice')} onRetry={fetchPricingData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      <div className="flex flex-col items-start gap-6 lg:flex-row">
        <div className="flex w-full flex-col gap-6 lg:w-5/12">
          <CurrentPriceCard loading={loading} currentPrice={currentPrice} delta={delta} deltaPct={deltaPct} />
          <PriceTrendChart trend={trend} />
          <RecommendationCard recommendation={productRecommendation} />
        </div>
        <div className="w-full lg:w-7/12">
          <PricingVariables
            harvest={harvest}
            setHarvest={setHarvest}
            stock={stock}
            setStock={setStock}
            demand={demand}
            setDemand={setDemand}
            stockTargetTons={latest?.stockTargetTons}
            competitors={productCompetitors}
            activeCompId={activeCompId}
            onSelectCompetitor={setSelectedCompId}
            proposed={proposed}
            proposedDelta={proposedDelta}
            applying={applying}
            canApply={proposed != null && pricingProductId != null}
            onApply={handleApplyPricing}
          />
        </div>
      </div>
    </div>
  );
}
