import { ChartLine } from 'lucide-react';
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
import { Button, Card, EmptyState } from '../../../../shared/ui';
import { buildChartData, useDashboardFormat } from './dashboardData';

// Colours and sizes from the tokens of src/index.css, as the machinery chart.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' };
const TODAY_LABEL = { position: 'top', fill: 'var(--color-chart-2)', fontSize: TEXT_XS };

/** Historical and forecast volumes with the confidence band and a Today marker. */
function ForecastChart({ forecasts, rowCount, onCreate, className }) {
  const { t } = useTranslation('salesmarketing');
  const format = useDashboardFormat();
  const chart = buildChartData(forecasts, format.month);
  const ticks = chart ? [0, 0.25, 0.5, 0.75, 1].map((share) => chart.maxLabel * share) : [];

  return (
    <Card
      className={className}
      title={t('dashboard.chart.title')}
      subtitle={rowCount > 0 ? t('dashboard.chart.rows', { number: rowCount }) : t('dashboard.chart.projection')}
    >
      {!chart ? (
        <EmptyState
          icon={ChartLine}
          message={t('dashboard.chart.empty')}
          action={
            <Button variant="ghost" onClick={onCreate}>
              {t('dashboard.chart.create')}
            </Button>
          }
        />
      ) : (
        <div className="h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chart.data} margin={{ top: 16, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
              <XAxis dataKey="key" tickFormatter={(index) => chart.data[index]?.label ?? ''} tickLine={false} axisLine={false} tick={AXIS_TICK} />
              <YAxis
                domain={[0, chart.maxLabel]}
                ticks={ticks}
                tickFormatter={format.tick}
                tickLine={false}
                axisLine={false}
                width={40}
                tick={AXIS_TICK}
              />
              <Tooltip
                contentStyle={TOOLTIP_STYLE}
                labelFormatter={(index) => chart.data[index]?.label ?? ''}
                formatter={(value, name) => [
                  Array.isArray(value) ? value.map((v) => format.tons(v)).join(' - ') : format.tons(value),
                  name,
                ]}
              />
              <Legend wrapperStyle={LEGEND_STYLE} />
              <Area
                dataKey="band"
                name={t('dashboard.chart.band')}
                stroke="none"
                fill="var(--color-chart-1)"
                fillOpacity={0.15}
                isAnimationActive={false}
              />
              <Line
                dataKey="historical"
                name={t('dashboard.chart.historical')}
                stroke="var(--color-chart-3)"
                strokeWidth={3}
                dot={false}
                isAnimationActive={false}
              />
              <Line
                dataKey="forecast"
                name={t('dashboard.chart.forecast')}
                stroke="var(--color-chart-1)"
                strokeWidth={3}
                strokeDasharray="8 8"
                dot={false}
                isAnimationActive={false}
              />
              <ReferenceLine
                x={chart.todayIndex}
                stroke="var(--color-chart-2)"
                strokeDasharray="4 4"
                label={{ ...TODAY_LABEL, value: t('dashboard.chart.today') }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}

export default ForecastChart;
