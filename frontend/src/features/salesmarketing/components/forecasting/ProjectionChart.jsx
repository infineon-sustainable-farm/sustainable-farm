import { LineChart as LineChartIcon } from 'lucide-react';
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useTranslation } from 'react-i18next';
import { Card, EmptyState } from '../../../../shared/ui';
import { useForecastFormat } from './forecastFormat';

// Colours and sizes from the tokens of src/index.css, as the machinery chart.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' };
const TODAY_LABEL = { position: 'insideTopRight', fill: 'var(--color-chart-3)', fontSize: TEXT_XS, fontWeight: 700 };

/** History, forecast and confidence band of up to 10 forecast points, with a Today marker. */
function ProjectionChart({ chart, rowCount, confidenceInterval, onAddForecast, className }) {
  const { t } = useTranslation('salesmarketing');
  const format = useForecastFormat();

  function axisLabel(index) {
    const point = chart.points[index];
    if (!point) return '';
    return index === 0 ? format.month(point.forecastMonth) : format.shortMonth(point.forecastMonth);
  }

  return (
    <Card
      className={className}
      title={t('forecasting.chart.title')}
      subtitle={
        rowCount > 0
          ? t('forecasting.chart.subtitleRows', { count: rowCount })
          : t('forecasting.chart.subtitle')
      }
    >
      {!chart ? (
        <EmptyState
          icon={LineChartIcon}
          message={t('forecasting.chart.empty')}
          actionLabel={t('forecasting.chart.addRows')}
          onAction={onAddForecast}
        />
      ) : (
        <div className="h-96 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chart.points} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--color-chart-grid)" />
              <XAxis dataKey="index" tickFormatter={axisLabel} tickLine={false} axisLine={false} tick={AXIS_TICK} />
              <YAxis tickLine={false} axisLine={false} width={48} tick={AXIS_TICK} />
              <Tooltip contentStyle={TOOLTIP_STYLE} labelFormatter={axisLabel} />
              <Legend wrapperStyle={LEGEND_STYLE} />
              <ReferenceLine
                x={chart.splitAt}
                stroke="var(--color-chart-3)"
                strokeDasharray="8 8"
                strokeWidth={2}
                label={{ ...TODAY_LABEL, value: t('forecasting.chart.today') }}
              />
              <Area
                dataKey="band"
                name={t('forecasting.chart.ci', { value: confidenceInterval })}
                fill="var(--color-chart-1)"
                fillOpacity={0.1}
                stroke="none"
                connectNulls={false}
                isAnimationActive={false}
              />
              <Line
                type="linear"
                dataKey="historical"
                name={t('forecasting.chart.historical')}
                stroke="var(--color-chart-3)"
                strokeWidth={4}
                strokeLinecap="round"
                dot={{ r: 5, strokeWidth: 0, fill: 'var(--color-chart-3)' }}
                connectNulls={false}
                isAnimationActive={false}
              />
              <Line
                type="linear"
                dataKey="forecast"
                name={t('forecasting.chart.forecast')}
                stroke="var(--color-chart-1)"
                strokeWidth={4}
                strokeDasharray="8 8"
                strokeLinecap="round"
                dot={{ r: 6, strokeWidth: 0, fill: 'var(--color-chart-1)' }}
                connectNulls={false}
                isAnimationActive={false}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}

export default ProjectionChart;
