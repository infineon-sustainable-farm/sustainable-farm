import {
  Bar,
  BarChart,
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
  { key: 'period', label: 'Period' },
  { key: 'output', label: 'Production output' },
];

export default function ProductionOutputChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Production Output"
      subtitle="Output per week"
      demo={demo}
      empty={!hasRows(data)}
      exportName="production-output"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="80ms"
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_STROKE} />
              <XAxis dataKey="period" tickLine={false} axisLine={false} tick={CHART_AXIS_TICK} />
              <YAxis tickLine={false} axisLine={false} width={46} tick={CHART_AXIS_TICK} />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Bar
                dataKey="output"
                name="Production output"
                fill="#2e9e6b"
                radius={[6, 6, 0, 0]}
                maxBarSize={38}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No production output data for this period.
        </div>
      )}
    </PTChartShell>
  );
}
