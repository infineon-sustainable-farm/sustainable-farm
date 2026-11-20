import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/api';
import { getErrorMessage } from './forecastingData';

function list(value) {
  return Array.isArray(value) ? value : [];
}

/**
 * Loads forecasts, model runs, products, orders and order items, and holds the model parameters,
 * which start from the latest model run.
 */
export function useForecastingData() {
  const { t } = useTranslation('salesmarketing');
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [forecasts, setForecasts] = useState([]);
  const [modelRuns, setModelRuns] = useState([]);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [orderItems, setOrderItems] = useState([]);

  const [selectedProductId, setSelectedProductId] = useState('');
  const [region, setRegion] = useState('');
  const [confidenceInterval, setConfidenceInterval] = useState(95);
  const [weatherPatterns, setWeatherPatterns] = useState(true);
  const [commodityPrices, setCommodityPrices] = useState(true);
  const [geopoliticalIndex, setGeopoliticalIndex] = useState(false);

  const fetchForecastingData = useCallback(async () => {
    const fallback = t('forecasting.requestFailed');
    try {
      const [forecastRes, modelRunRes, productRes, orderRes, orderItemRes] = await Promise.allSettled([
        api.getDemandForecasts(),
        api.getForecastModelRuns(),
        api.getProducts(),
        api.getOrders(),
        api.getOrderItems(),
      ]);
      setApiError(null);
      const failures = [];
      const fail = (key, res) => failures.push(t(key, { message: getErrorMessage(res.reason, fallback) }));

      if (forecastRes.status === 'fulfilled') {
        setForecasts(list(forecastRes.value));
      } else {
        setForecasts([]);
        fail('forecasting.failures.forecasts', forecastRes);
      }

      if (modelRunRes.status === 'fulfilled') {
        const runs = list(modelRunRes.value);
        setModelRuns(runs);
        const latest = [...runs].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))[0];
        if (latest) {
          if (latest.region) setRegion(latest.region);
          if (latest.confidenceIntervalPct != null) setConfidenceInterval(Number(latest.confidenceIntervalPct));
          if (latest.useWeatherData != null) setWeatherPatterns(!!latest.useWeatherData);
          if (latest.useCommodityPrices != null) setCommodityPrices(!!latest.useCommodityPrices);
          if (latest.useGeopoliticalIndex != null) setGeopoliticalIndex(!!latest.useGeopoliticalIndex);
        }
      } else {
        setModelRuns([]);
        fail('forecasting.failures.modelRuns', modelRunRes);
      }

      if (productRes.status === 'fulfilled') {
        const products = list(productRes.value);
        setProducts(products);
        setSelectedProductId((current) =>
          current || !products[0]?.productId ? current : String(products[0].productId)
        );
      } else {
        setProducts([]);
        fail('forecasting.failures.products', productRes);
      }

      if (orderRes.status === 'fulfilled') {
        setOrders(list(orderRes.value));
      } else {
        setOrders([]);
        fail('forecasting.failures.orders', orderRes);
      }

      if (orderItemRes.status === 'fulfilled') {
        setOrderItems(list(orderItemRes.value));
      } else {
        setOrderItems([]);
        fail('forecasting.failures.orderItems', orderItemRes);
      }

      if (failures.length > 0) setApiError(failures.join(t('forecasting.failures.separator')));
    } catch (err) {
      setApiError(getErrorMessage(err, t('forecasting.loadFailed')));
      setForecasts([]);
      setModelRuns([]);
      setOrders([]);
      setOrderItems([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchForecastingData);
  }, [fetchForecastingData]);

  return {
    loading,
    apiError,
    forecasts,
    modelRuns,
    products,
    orders,
    orderItems,
    refetch: fetchForecastingData,
    params: {
      selectedProductId,
      setSelectedProductId,
      region,
      setRegion,
      confidenceInterval,
      setConfidenceInterval,
      weatherPatterns,
      setWeatherPatterns,
      commodityPrices,
      setCommodityPrices,
      geopoliticalIndex,
      setGeopoliticalIndex,
    },
  };
}
