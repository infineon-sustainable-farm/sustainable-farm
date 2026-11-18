import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { Card, EmptyState } from '../../../../shared/ui';
import { usePricingFormat } from './pricingModel';

// Colours and sizes from the tokens of src/index.css, as the machinery chart.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};

/** Price per kg of the last 30 prices of the current product. */
function PriceTrendChart({ trend }) {
  const { t } = useTranslation('salesmarketing');
  const format = usePricingFormat();
  const data = (trend?.points ?? []).map((point, index) => ({
    index,
    month: point.priceDate ? format.month(point.priceDate) : '',
    price: Number(point.priceEurPerKg),
  }));

  return (
    <Card
      title={t('pricing.trend')}
      actions={
        <span className="flex items-center gap-1 text-sm text-text-muted">
          <span className="h-2 w-2 rounded-pill bg-primary" aria-hidden="true" />
          {t('pricing.avgPrice')}
        </span>
      }
    >
      {trend ? (
        <div className="h-60 w-full" role="img" aria-label={t('pricing.trendLabel')}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
              <XAxis
                dataKey="index"
                tickLine={false}
                axisLine={false}
                tick={AXIS_TICK}
                tickFormatter={(index) => data[index]?.month ?? ''}
              />
              <YAxis
                domain={trend.domain}
                tickLine={false}
                axisLine={false}
                width={56}
                tick={AXIS_TICK}
                tickFormatter={(value) => format.price(value)}
              />
              <Tooltip
                contentStyle={TOOLTIP_STYLE}
                labelFormatter={(index) => data[index]?.month ?? ''}
                formatter={(value) => [format.price(value), t('pricing.avgPrice')]}
              />
              <Area
                type="linear"
                dataKey="price"
                stroke="var(--color-chart-1)"
                strokeWidth={3}
                fill="var(--color-chart-1)"
                fillOpacity={0.1}
                dot={{ r: 3, strokeWidth: 0, fill: 'var(--color-chart-1)' }}
                isAnimationActive={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <EmptyState message={t('pricing.trendEmpty')} />
      )}
    </Card>
  );
}

export default PriceTrendChart;
