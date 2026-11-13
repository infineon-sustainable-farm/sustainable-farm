import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
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
  CHART_LEGEND_STYLE,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'batchId', label: 'Batch' },
  { key: 'yieldPct', label: 'Wash yield (%)' },
  { key: 'durationHours', label: 'Drying duration (h)' },
  { key: 'throughputKgPerHour', label: 'Throughput (kg/h)' },
];

export default function ProcessingPerformanceChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Processing Performance"
      subtitle="Wash yield and drying throughput per batch"
      unit="% · kg/h"
      demo={demo}
      empty={!hasRows(data)}
      exportName="processing-performance"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="240ms"
      note="Yield comes from the wash & sort records; throughput is wash output per drying hour."
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={data} margin={{ top: 8, right: 8, bottom: 8, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_STROKE} />
              <XAxis
                dataKey="batchId"
                tickLine={false}
                axisLine={false}
                interval={0}
                height={54}
                tick={{ ...CHART_AXIS_TICK, fontSize: 11 }}
                angle={-20}
                textAnchor="end"
              />
              <YAxis
                yAxisId="yield"
                domain={[0, 100]}
                tickLine={false}
                axisLine={false}
                width={40}
                tick={CHART_AXIS_TICK}
                tickFormatter={(value) => `${value}%`}
              />
              <YAxis
                yAxisId="throughput"
                orientation="right"
                tickLine={false}
                axisLine={false}
                width={48}
                tick={CHART_AXIS_TICK}
                tickFormatter={(value) => `${value}`}
              />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Legend wrapperStyle={CHART_LEGEND_STYLE} />
              <Bar
                yAxisId="yield"
                dataKey="yieldPct"
                name="Wash yield (%)"
                fill="#0a8276"
                radius={[6, 6, 0, 0]}
                maxBarSize={32}
              />
              <Line
                yAxisId="throughput"
                type="monotone"
                dataKey="throughputKgPerHour"
                name="Throughput (kg/h)"
                stroke="#d9a441"
                strokeWidth={2.5}
                dot={{ r: 3, strokeWidth: 0, fill: '#d9a441' }}
                activeDot={{ r: 5 }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No processing performance data for this period.
        </div>
      )}
    </PTChartShell>
  );
}
