import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PTChartShell from '../PTChartShell';
import { hasRows } from '../../utils/dashboardMetrics';
import {
  CHART_AXIS_TICK,
  CHART_GRID_STROKE,
  CHART_HEIGHT,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'date', label: 'Date' },
  { key: 'quantity', label: 'Harvest quantity (kg)' },
];

export default function HarvestTrendChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Harvest Quantity Trend"
      subtitle="From Plants harvest events (kg)"
      unit="kg"
      demo={demo}
      empty={!hasRows(data)}
      exportName="harvest-trend"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="0ms"
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_STROKE} />
              <XAxis dataKey="date" tickLine={false} axisLine={false} tick={CHART_AXIS_TICK} />
              <YAxis tickLine={false} axisLine={false} width={46} tick={CHART_AXIS_TICK} />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Area
                type="monotone"
                dataKey="quantity"
                name="Harvest quantity (kg)"
                stroke="#0a8276"
                fill="#0a8276"
                fillOpacity={0.2}
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No harvest trend data for this period.
        </div>
      )}
    </PTChartShell>
  );
}
