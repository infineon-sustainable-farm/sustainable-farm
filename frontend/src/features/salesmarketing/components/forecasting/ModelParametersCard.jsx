import { SlidersHorizontal } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, Field, Select } from '../../../../shared/ui';

const MIN_CONFIDENCE = 80;
const MAX_CONFIDENCE = 99;

function Switch({ label, checked, onChange, muted }) {
  return (
    <label className="flex cursor-pointer items-center gap-3">
      <input
        type="checkbox"
        role="switch"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 shrink-0 cursor-pointer accent-primary"
      />
      <span className={`text-sm ${muted ? 'text-text-muted' : 'text-text'}`}>{label}</span>
    </label>
  );
}

/** Product, region, scenario filter, confidence interval and external variables of the model. */
function ModelParametersCard({ products, regions, params, scenarioFilter, onScenarioFilterChange }) {
  const { t } = useTranslation('salesmarketing');
  const {
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
  } = params;
  const regionOptions = region && !regions.includes(region) ? [region, ...regions] : regions;

  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <SlidersHorizontal size={20} aria-hidden="true" className="text-primary" />
          {t('forecasting.parameters.title')}
        </span>
      }
    >
      <div className="flex flex-col gap-4">
        <Field label={t('forecasting.parameters.cropVariety')}>
          <Select value={selectedProductId} onChange={(event) => setSelectedProductId(event.target.value)}>
            <option value="">{t('forecasting.parameters.allProducts')}</option>
            {products.map((p) => (
              <option key={p.productId} value={String(p.productId)}>
                {p.variety || p.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label={t('forecasting.parameters.region')}>
          <Select value={region} onChange={(event) => setRegion(event.target.value)}>
            {!region && <option value="" disabled />}
            {regionOptions.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </Select>
        </Field>

        <Field label={t('forecasting.parameters.scenarioFilter')}>
          <Select value={scenarioFilter} onChange={(event) => onScenarioFilterChange(event.target.value)}>
            <option value="all">{t('forecasting.parameters.allScenarios')}</option>
            <option value="base">{t('forecasting.scenario.base')}</option>
            <option value="optimistic">{t('forecasting.scenario.optimistic')}</option>
            <option value="pessimistic">{t('forecasting.scenario.pessimistic')}</option>
          </Select>
        </Field>

        <div>
          <div className="mb-1 flex items-center justify-between">
            <label htmlFor="forecasting-confidence" className="text-sm font-medium">
              {t('forecasting.parameters.confidence')}
            </label>
            <span className="text-sm font-semibold text-text-accent">
              {t('forecasting.percentValue', { value: confidenceInterval })}
            </span>
          </div>
          <input
            id="forecasting-confidence"
            type="range"
            min={MIN_CONFIDENCE}
            max={MAX_CONFIDENCE}
            value={confidenceInterval}
            onChange={(event) => setConfidenceInterval(Number(event.target.value))}
            className="w-full cursor-pointer accent-primary"
          />
          <div className="mt-1 flex justify-between text-xs text-text-muted">
            <span>{t('forecasting.percentValue', { value: MIN_CONFIDENCE })}</span>
            <span>{t('forecasting.percentValue', { value: MAX_CONFIDENCE })}</span>
          </div>
        </div>

        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('forecasting.parameters.externalVariables')}</legend>
          <div className="space-y-2">
            <Switch label={t('forecasting.parameters.weather')} checked={weatherPatterns} onChange={setWeatherPatterns} />
            <Switch label={t('forecasting.parameters.commodity')} checked={commodityPrices} onChange={setCommodityPrices} />
            <Switch
              label={t('forecasting.parameters.geopolitical')}
              checked={geopoliticalIndex}
              onChange={setGeopoliticalIndex}
              muted
            />
          </div>
        </fieldset>
      </div>
    </Card>
  );
}

export default ModelParametersCard;
