import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../shared/ui';
import { formatNumber } from '../utils/format';

// Colours and sizes from the tokens of src/index.css, as the machinery charts. The axis ticks are
// SVG attributes: their font size is the text-xs value as a number.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' };
const GRID = { strokeDasharray: '3 3', stroke: 'var(--color-chart-grid)' };
const MARGIN = { top: 8, right: 28, bottom: 0, left: 0 };
const PIE_COLORS = [1, 2, 3, 4, 5, 6].map((index) => `var(--color-chart-${index})`);

const hasRows = (rows) => Array.isArray(rows) && rows.length > 0;

function ChartCard({ title, subtitle, rows, emptyMessage, children }) {
  return (
    <Card title={title} titleAs="h3" subtitle={subtitle}>
      {hasRows(rows) ? (
        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            {children}
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="flex h-64 items-center justify-center text-sm text-text-muted">{emptyMessage}</div>
      )}
    </Card>
  );
}

/** The five charts of the Product Transformation dashboard, each with its empty state. */
export default function DashboardCharts({ charts }) {
  const { t } = useTranslation('producttransformation');

  const pieLabel = ({ name, percent }) => t('dashboard.pieLabel', { name, value: formatNumber(percent * 100, 0) });

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <ChartCard
        title={t('dashboard.charts.harvestTrend')}
        subtitle={t('dashboard.charts.harvestTrendSubtitle')}
        rows={charts.harvestTrend}
        emptyMessage={t('dashboard.charts.harvestTrendEmpty')}
      >
        <AreaChart data={charts.harvestTrend} margin={MARGIN}>
          <CartesianGrid {...GRID} />
          <XAxis dataKey="date" tick={AXIS_TICK} />
          <YAxis tick={AXIS_TICK} />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
          <Area
            type="monotone"
            dataKey="quantity"
            name={t('dashboard.charts.harvestSeries')}
            stroke="var(--color-chart-1)"
            fill="var(--color-chart-1)"
            fillOpacity={0.2}
            isAnimationActive={false}
          />
        </AreaChart>
      </ChartCard>

      <ChartCard
        title={t('dashboard.charts.productionOutput')}
        subtitle={t('dashboard.charts.productionOutputSubtitle')}
        rows={charts.productionOutput}
        emptyMessage={t('dashboard.charts.productionOutputEmpty')}
      >
        <BarChart data={charts.productionOutput} margin={MARGIN}>
          <CartesianGrid {...GRID} />
          <XAxis dataKey="period" tick={AXIS_TICK} />
          <YAxis tick={AXIS_TICK} />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
          <Bar
            dataKey="output"
            name={t('dashboard.charts.productionSeries')}
            fill="var(--color-chart-4)"
            isAnimationActive={false}
          />
        </BarChart>
      </ChartCard>

      <ChartCard
        title={t('dashboard.charts.qualityTrend')}
        subtitle={t('dashboard.charts.qualityTrendSubtitle')}
        rows={charts.qualityTrend}
        emptyMessage={t('dashboard.charts.qualityTrendEmpty')}
      >
        <LineChart data={charts.qualityTrend} margin={MARGIN}>
          <CartesianGrid {...GRID} />
          <XAxis dataKey="date" tick={AXIS_TICK} />
          <YAxis domain={[0, 100]} tick={AXIS_TICK} />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
          <Line
            type="monotone"
            dataKey="passRate"
            name={t('dashboard.charts.qualitySeries')}
            stroke="var(--color-chart-2)"
            strokeWidth={2}
            dot={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ChartCard>

      <ChartCard
        title={t('dashboard.charts.equipmentUtilization')}
        subtitle={t('dashboard.charts.equipmentUtilizationSubtitle')}
        rows={charts.equipmentUtilization}
        emptyMessage={t('dashboard.charts.equipmentUtilizationEmpty')}
      >
        <BarChart data={charts.equipmentUtilization} layout="vertical">
          <CartesianGrid {...GRID} />
          <XAxis type="number" domain={[0, 100]} tick={AXIS_TICK} />
          <YAxis dataKey="equipment" type="category" width={120} tick={AXIS_TICK} />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
          <Bar
            dataKey="utilization"
            name={t('dashboard.charts.utilizationSeries')}
            fill="var(--color-chart-6)"
            isAnimationActive={false}
          />
        </BarChart>
      </ChartCard>

      <ChartCard
        title={t('dashboard.charts.energyBreakdown')}
        subtitle={t('dashboard.charts.energyBreakdownSubtitle')}
        rows={charts.energyBreakdown}
        emptyMessage={t('dashboard.charts.energyBreakdownEmpty')}
      >
        <PieChart>
          <Pie
            data={charts.energyBreakdown}
            cx="50%"
            cy="50%"
            outerRadius={90}
            dataKey="value"
            nameKey="name"
            label={pieLabel}
            isAnimationActive={false}
          >
            {(charts.energyBreakdown ?? []).map((entry, index) => (
              <Cell key={`cell-${entry.name}-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
            ))}
          </Pie>
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
        </PieChart>
      </ChartCard>
    </div>
  );
}
