import { ArrowLeftRight, ChartLine, Check, Leaf, Package, Upload } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Card } from '../../../../shared/ui';
import { seasonKey, usePricingFormat } from './pricingModel';

function Slider({ id, icon: Icon, label, value, display, min, max, onChange, hint }) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-2">
        <label htmlFor={id} className="flex items-center gap-1 text-sm font-medium">
          <Icon size={16} aria-hidden="true" className="text-primary" />
          {label}
        </label>
        <span className="text-sm font-medium text-text-accent">{display}</span>
      </div>
      <input
        id={id}
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full cursor-pointer accent-primary"
      />
      {hint && <p className="mt-1 text-sm text-text-muted">{hint}</p>}
    </div>
  );
}

function CompetitorPicker({ competitors, activeId, onSelect }) {
  const { t } = useTranslation('salesmarketing');
  const format = usePricingFormat();
  return (
    <fieldset>
      <legend className="mb-4 flex items-center gap-1 text-sm font-medium">
        <ArrowLeftRight size={16} aria-hidden="true" className="text-primary" />
        {t('pricing.competitorShift')}
      </legend>
      <div className="grid grid-cols-3 gap-4">
        {competitors.slice(0, 3).map((comp) => {
          const active = comp.competitorPriceId === activeId;
          return (
            <button
              key={comp.competitorPriceId}
              type="button"
              aria-pressed={active}
              onClick={() => onSelect(comp.competitorPriceId)}
              className={`relative flex flex-col items-center justify-center rounded-control border p-2 transition duration-(--duration-fast) ease-standard ${
                active ? 'border-primary bg-primary-tint text-text-accent' : 'border-border-input bg-surface-raised hover:border-primary'
              }`}
            >
              {active && (
                <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-pill bg-primary text-on-primary">
                  <Check size={12} aria-hidden="true" />
                </span>
              )}
              <span className={`mb-1 text-xs ${active ? '' : 'text-text-muted'}`}>{comp.competitorName}</span>
              <span>{format.price(comp.priceEur)}</span>
            </button>
          );
        })}
        {competitors.length === 0 && <p className="col-span-3 text-sm text-text-muted">{t('pricing.noCompetitors')}</p>}
      </div>
    </fieldset>
  );
}

/** Model inputs (season, stock, demand, competitor), the proposed price and Apply. */
function PricingVariables({
  harvest,
  setHarvest,
  stock,
  setStock,
  demand,
  setDemand,
  stockTargetTons,
  competitors,
  activeCompId,
  onSelectCompetitor,
  proposed,
  proposedDelta,
  applying,
  canApply,
  onApply,
}) {
  const { t } = useTranslation('salesmarketing');
  const format = usePricingFormat();
  const belowTarget = stockTargetTons != null && stock < stockTargetTons;

  return (
    <Card title={t('pricing.variables')}>
      <div className="flex flex-col gap-6">
        <Slider
          id="pricing-harvest"
          icon={Leaf}
          label={t('pricing.harvestImpact')}
          value={harvest}
          display={t(`pricing.season.${seasonKey(harvest)}`)}
          min="1"
          max="100"
          onChange={setHarvest}
          hint={t('pricing.harvestHint')}
        />
        <Slider
          id="pricing-stock"
          icon={Package}
          label={t('pricing.stockLevel')}
          value={stock}
          display={t('pricing.tons', { value: format.number(stock) })}
          min="0"
          max="150"
          onChange={setStock}
          hint={t(belowTarget ? 'pricing.stockBelow' : 'pricing.stockAbove', {
            target: stockTargetTons ?? format.none,
          })}
        />
        <Slider
          id="pricing-demand"
          icon={ChartLine}
          label={t('pricing.demandIndex')}
          value={demand}
          display={format.demand(demand)}
          min="50"
          max="150"
          onChange={setDemand}
        />
        <CompetitorPicker competitors={competitors} activeId={activeCompId} onSelect={onSelectCompetitor} />
      </div>

      <div className="mt-8 flex flex-col items-center justify-between gap-6 sm:flex-row">
        <div>
          <p className="text-sm text-text-muted">{t('pricing.proposed')}</p>
          <p className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-text-accent">{format.price(proposed)}</span>
            <span className="text-sm font-medium text-warning-fg">
              {proposedDelta == null ? t('pricing.awaitingRecommendation') : format.priceDelta(proposedDelta)}
            </span>
          </p>
        </div>
        <Button size="lg" icon={Upload} loading={applying} disabled={!canApply} onClick={onApply}>
          {applying ? t('pricing.applying') : t('pricing.apply')}
        </Button>
      </div>
    </Card>
  );
}

export default PricingVariables;
